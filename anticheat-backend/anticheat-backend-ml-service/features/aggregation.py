"""
Aggregates per-move engine analysis into per-color features for the classifier.
"""

import numpy as np
from scipy.stats import entropy, kurtosis, skew
from typing import List, Dict


def aggregate_color_features(move_data: List[Dict]) -> Dict[str, float]:
    """
    Take a list of per-move dicts (from EngineAnalysisPool) for ONE color
    and produce aggregated features.

    Args:
        move_data: List of dicts with keys: cpl, top1_match, top3_match,
                   top5_match, eval_best, eval_played.

    Returns:
        Dict of ~18 features for this color.
    """
    if not move_data:
        return _empty_features()

    cpls = np.array([m["cpl"] for m in move_data], dtype=np.float64)
    evals = np.array([m["eval_played"] for m in move_data], dtype=np.float64)
    top1s = np.array([m["top1_match"] for m in move_data], dtype=bool)
    top3s = np.array([m["top3_match"] for m in move_data], dtype=bool)
    top5s = np.array([m["top5_match"] for m in move_data], dtype=bool)

    n = len(cpls)
    features = {}

    # -- CPL features --
    features["cpl_mean"] = float(np.mean(cpls))
    features["cpl_median"] = float(np.median(cpls))
    features["cpl_variance"] = float(np.var(cpls))
    features["cpl_max"] = float(np.max(cpls))
    features["cpl_p90"] = float(np.percentile(cpls, 90))
    features["zero_cpl_rate"] = float(np.sum(cpls == 0) / n)

    # -- Top-N match rates --
    features["top1_rate"] = float(np.mean(top1s))
    features["top3_rate"] = float(np.mean(top3s))
    features["top5_rate"] = float(np.mean(top5s))

    # -- Consecutive top-1 streak --
    features["max_consecutive_top1"] = float(_max_consecutive_true(top1s))

    # -- CPL by game phase --
    third = n // 3
    if third > 0:
        features["cpl_opening"] = float(np.mean(cpls[:third]))
        features["cpl_middlegame"] = float(np.mean(cpls[third:2 * third]))
        features["cpl_endgame"] = float(np.mean(cpls[2 * third:]))
    else:
        features["cpl_opening"] = features["cpl_mean"]
        features["cpl_middlegame"] = features["cpl_mean"]
        features["cpl_endgame"] = features["cpl_mean"]

    # -- Legacy features (computed on eval_played for this color) --
    features["varianza_eval"] = float(np.var(evals)) if n > 1 else 0.0
    features["curtosis_eval"] = float(kurtosis(evals)) if n > 3 else 0.0
    features["asimetria_eval"] = float(skew(evals)) if n > 2 else 0.0

    signs = np.sign(evals)
    features["inversiones_ventaja"] = float(np.sum(np.diff(signs) != 0)) if n > 1 else 0.0

    if n > 1:
        hist, _ = np.histogram(evals, bins=min(30, n), density=True)
        hist = hist[hist > 0]
        features["entropia_eval"] = float(entropy(hist))
    else:
        features["entropia_eval"] = 0.0

    return features


def _max_consecutive_true(arr: np.ndarray) -> int:
    """Find the longest consecutive run of True values."""
    if len(arr) == 0:
        return 0
    max_run = 0
    current = 0
    for val in arr:
        if val:
            current += 1
            max_run = max(max_run, current)
        else:
            current = 0
    return max_run


def _empty_features() -> Dict[str, float]:
    """Return zero-valued features when no move data is available."""
    keys = [
        "cpl_mean", "cpl_median", "cpl_variance", "cpl_max", "cpl_p90",
        "zero_cpl_rate", "top1_rate", "top3_rate", "top5_rate",
        "max_consecutive_top1", "cpl_opening", "cpl_middlegame", "cpl_endgame",
        "varianza_eval", "curtosis_eval", "asimetria_eval",
        "inversiones_ventaja", "entropia_eval",
    ]
    return {k: 0.0 for k in keys}


FEATURE_NAMES = list(_empty_features().keys())
