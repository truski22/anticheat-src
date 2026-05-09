"""
Strategic tests for the ML service.
Tests feature extraction logic — the core of the fraud detection pipeline.
Does NOT require Stockfish (tests the statistical analysis, not engine evaluation).
"""
import pytest
import numpy as np

# Import the feature extraction function directly
import sys
import os
sys.path.insert(0, os.path.dirname(__file__))
from main import calculate_metric


class TestCalculateMetric:
    """Test the statistical feature extraction that feeds the ML model."""

    def test_basic_output_keys(self):
        """Verify all expected features are produced."""
        scores = [20, -15, 30, -10, 25, -5, 40, -20, 15, -8]
        result = calculate_metric(scores)
        
        expected_keys = [
            "media_total", "mediana_total", "varianza_total",
            "curtosis_total", "asimetria_total",
            "min_valor", "max_valor", "rango", "iqr", "cv",
            "percentil_25", "percentil_50", "percentil_75", "percentil_90",
            "deltas_promedio", "variabilidad_entre_turnos",
            "entropia_evaluaciones", "inversiones_ventaja",
            "media_inicio", "media_medio", "media_final",
            "freq_dominante", "entropia_espectral",
        ]
        for key in expected_keys:
            assert key in result, f"Missing key: {key}"

    def test_mean_calculation(self):
        """Verify mean is correct for known input."""
        scores = [10, 20, 30, 40, 50]
        result = calculate_metric(scores)
        assert result["media_total"] == pytest.approx(30.0)

    def test_variance_calculation(self):
        """Verify variance is correct for known input."""
        scores = [10, 20, 30, 40, 50]
        result = calculate_metric(scores)
        assert result["varianza_total"] == pytest.approx(200.0)

    def test_range_calculation(self):
        """Verify range = max - min."""
        scores = [-100, 0, 50, 200]
        result = calculate_metric(scores)
        assert result["min_valor"] == -100
        assert result["max_valor"] == 200
        assert result["rango"] == 300

    def test_sign_inversions(self):
        """Verify advantage inversion count."""
        # +, +, -, -, +, - → inversions: +→-, -→+, +→- = 3
        scores = [10, 5, -3, -8, 12, -1]
        result = calculate_metric(scores)
        assert result["inversiones_ventaja"] == 3

    def test_constant_scores(self):
        """Constant scores should have zero variance and no inversions."""
        scores = [50, 50, 50, 50, 50, 50]
        result = calculate_metric(scores)
        assert result["varianza_total"] == pytest.approx(0.0)
        assert result["inversiones_ventaja"] == 0

    def test_cheater_like_pattern(self):
        """Consistently low centipawn loss (cheater pattern) should have low variance."""
        # Simulated "perfect" play — all evaluations close to 0
        scores = [5, 3, 7, 2, 4, 6, 3, 5, 4, 3, 5, 2, 6, 4, 3]
        result = calculate_metric(scores)
        # Low variance is suspicious
        assert result["varianza_total"] < 10

    def test_normal_play_pattern(self):
        """Normal play has wider variance in evaluations."""
        scores = [20, -50, 100, -30, 200, -150, 80, -20, 50, -100]
        result = calculate_metric(scores)
        # Higher variance expected
        assert result["varianza_total"] > 1000

    def test_phase_means_with_enough_moves(self):
        """Verify opening/middle/endgame phase means are computed."""
        scores = list(range(1, 31))  # 30 moves
        result = calculate_metric(scores)
        # First third: 1-10, mean = 5.5
        assert result["media_inicio"] == pytest.approx(5.5)
        # Second third: 11-20, mean = 15.5
        assert result["media_medio"] == pytest.approx(15.5)

    def test_short_game(self):
        """Very short game (< 3 moves) should not crash."""
        scores = [10, 20]
        result = calculate_metric(scores)
        assert "media_total" in result
        assert result["media_total"] == pytest.approx(15.0)

    def test_output_types_are_python_native(self):
        """Ensure all values are native Python types, not numpy scalars."""
        scores = [10, -5, 20, -10, 15]
        result = calculate_metric(scores)
        for key, value in result.items():
            assert not isinstance(value, np.generic), \
                f"Key '{key}' has numpy type {type(value)}, expected native Python type"
