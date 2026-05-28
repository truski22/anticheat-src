"""
Tests for the ML service.
Tests feature extraction and aggregation logic — the core of the fraud detection pipeline.
Does NOT require Stockfish (tests statistical analysis, not engine evaluation).
"""
import pytest
import numpy as np

import sys
import os
sys.path.insert(0, os.path.dirname(__file__))

from features.aggregation import aggregate_color_features, FEATURE_NAMES, _max_consecutive_true


class TestAggregateColorFeatures:
    """Test the new per-color feature aggregation."""

    def _make_move_data(self, cpls, top1s=None, top3s=None, top5s=None, evals=None):
        """Helper to build move_data dicts."""
        n = len(cpls)
        if top1s is None:
            top1s = [cpl == 0 for cpl in cpls]
        if top3s is None:
            top3s = [cpl <= 20 for cpl in cpls]
        if top5s is None:
            top5s = [cpl <= 50 for cpl in cpls]
        if evals is None:
            evals = [100 - cpl for cpl in cpls]

        return [
            {
                "cpl": cpls[i],
                "top1_match": top1s[i],
                "top3_match": top3s[i],
                "top5_match": top5s[i],
                "eval_best": 100,
                "eval_played": evals[i],
                "played_move": f"move_{i}",
            }
            for i in range(n)
        ]

    def test_all_feature_keys_present(self):
        """Verify all expected features are produced."""
        data = self._make_move_data([10, 20, 30, 5, 15, 25, 0, 40, 10, 20])
        result = aggregate_color_features(data)
        for key in FEATURE_NAMES:
            assert key in result, f"Missing key: {key}"

    def test_output_types_are_python_native(self):
        """Ensure all values are native Python types."""
        data = self._make_move_data([10, 0, 20, 5, 30])
        result = aggregate_color_features(data)
        for key, value in result.items():
            assert isinstance(value, float), \
                f"Key '{key}' has type {type(value)}, expected float"

    def test_cpl_mean(self):
        """Verify CPL mean is correct."""
        data = self._make_move_data([10, 20, 30, 40, 50])
        result = aggregate_color_features(data)
        assert result["cpl_mean"] == pytest.approx(30.0)

    def test_cpl_max(self):
        """Verify CPL max captures worst move."""
        data = self._make_move_data([5, 10, 200, 3, 8])
        result = aggregate_color_features(data)
        assert result["cpl_max"] == pytest.approx(200.0)

    def test_top1_rate(self):
        """Verify top-1 match rate."""
        top1s = [True, False, True, True, False]
        data = self._make_move_data([0, 50, 0, 0, 30], top1s=top1s)
        result = aggregate_color_features(data)
        assert result["top1_rate"] == pytest.approx(0.6)

    def test_zero_cpl_rate(self):
        """Verify rate of perfect moves (CPL = 0)."""
        data = self._make_move_data([0, 0, 10, 0, 20])
        result = aggregate_color_features(data)
        assert result["zero_cpl_rate"] == pytest.approx(0.6)

    def test_cheater_pattern_low_cpl(self):
        """Cheater-like play has very low CPL."""
        data = self._make_move_data([0, 2, 0, 1, 0, 3, 0, 0, 1, 0])
        result = aggregate_color_features(data)
        assert result["cpl_mean"] < 5
        assert result["top1_rate"] > 0.5

    def test_normal_play_pattern(self):
        """Normal play has higher CPL and lower top-1 rate."""
        data = self._make_move_data([30, 80, 5, 120, 45, 15, 200, 60, 10, 90])
        result = aggregate_color_features(data)
        assert result["cpl_mean"] > 30
        assert result["top1_rate"] < 0.5

    def test_empty_data(self):
        """Empty move list should return zero features without crashing."""
        result = aggregate_color_features([])
        assert result["cpl_mean"] == 0.0
        assert result["top1_rate"] == 0.0

    def test_single_move(self):
        """Single move should not crash."""
        data = self._make_move_data([15])
        result = aggregate_color_features(data)
        assert result["cpl_mean"] == pytest.approx(15.0)

    def test_phase_means_with_enough_moves(self):
        """Verify CPL by phase (opening/middle/endgame)."""
        cpls = [10] * 5 + [30] * 5 + [50] * 5  # 15 moves
        data = self._make_move_data(cpls)
        result = aggregate_color_features(data)
        assert result["cpl_opening"] == pytest.approx(10.0)
        assert result["cpl_middlegame"] == pytest.approx(30.0)
        assert result["cpl_endgame"] == pytest.approx(50.0)


class TestMaxConsecutiveTrue:
    """Test the consecutive top-1 streak helper."""

    def test_basic_streak(self):
        assert _max_consecutive_true(np.array([True, True, True, False, True])) == 3

    def test_all_true(self):
        assert _max_consecutive_true(np.array([True, True, True, True])) == 4

    def test_all_false(self):
        assert _max_consecutive_true(np.array([False, False, False])) == 0

    def test_empty(self):
        assert _max_consecutive_true(np.array([])) == 0

    def test_single_true(self):
        assert _max_consecutive_true(np.array([True])) == 1
