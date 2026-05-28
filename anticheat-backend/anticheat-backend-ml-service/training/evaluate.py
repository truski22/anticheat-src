"""
Standalone evaluation: load a trained model and evaluate on the test split.

Usage:
    python evaluate.py [--model-dir <models/latest>] [--data <features.csv>]
"""

import argparse
import json
import os
import sys

import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import (
    accuracy_score, f1_score, precision_score, recall_score,
    roc_auc_score, classification_report, confusion_matrix,
)

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from features.aggregation import FEATURE_NAMES
from training.config import SEED, TEST_SIZE, FEATURES_CSV, LATEST_MODEL_DIR


def main(model_dir: str, csv_path: str):
    # Load model
    model_path = os.path.join(model_dir, "model.joblib")
    meta_path = os.path.join(model_dir, "metadata.json")

    print(f"Loading model: {model_path}")
    model = joblib.load(model_path)

    if os.path.exists(meta_path):
        with open(meta_path, "r", encoding="utf-8") as f:
            meta = json.load(f)
        print(f"  Version: {meta.get('version', '?')}")
        print(f"  Algorithm: {meta.get('algorithm', '?')}")
        print(f"  Trained at: {meta.get('trained_at', '?')}")

    # Load data
    print(f"\nLoading dataset: {csv_path}")
    df = pd.read_csv(csv_path)
    X = df[FEATURE_NAMES].values
    y = df["label"].values

    # Use same split as training
    _, X_test, _, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=SEED, stratify=y,
    )

    # Predict
    y_pred = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:, 1]

    # Metrics
    print(f"\n── Results on test set ({len(y_test)} samples) ──")
    print(f"  Accuracy:  {accuracy_score(y_test, y_pred):.4f}")
    print(f"  F1:        {f1_score(y_test, y_pred):.4f}")
    print(f"  Precision: {precision_score(y_test, y_pred):.4f}")
    print(f"  Recall:    {recall_score(y_test, y_pred):.4f}")
    print(f"  AUC-ROC:   {roc_auc_score(y_test, y_proba):.4f}")

    print(f"\nConfusion Matrix:\n{confusion_matrix(y_test, y_pred)}")
    print(f"\n{classification_report(y_test, y_pred, target_names=['legit', 'cheat'])}")

    # Feature importance
    if hasattr(model, "feature_importances_"):
        importances = model.feature_importances_
        sorted_idx = np.argsort(importances)[::-1]
        print("── Feature Importance ──")
        for rank, idx in enumerate(sorted_idx, 1):
            bar = "█" * int(importances[idx] * 50)
            print(f"  {rank:2d}. {FEATURE_NAMES[idx]:25s} {importances[idx]:.4f} {bar}")

    # Per-source breakdown
    _, df_test = train_test_split(
        df, test_size=TEST_SIZE, random_state=SEED, stratify=y,
    )
    df_test = df_test.copy()
    df_test["predicted"] = y_pred
    df_test["correct"] = df_test["label"] == df_test["predicted"]

    print("\n── Accuracy by source ──")
    for source in df_test["source"].unique():
        subset = df_test[df_test["source"] == source]
        acc = subset["correct"].mean()
        print(f"  {source}: {acc:.4f} ({len(subset)} samples)")

    print("\n── Accuracy by color ──")
    for color in ("white", "black"):
        subset = df_test[df_test["color"] == color]
        acc = subset["correct"].mean()
        print(f"  {color}: {acc:.4f} ({len(subset)} samples)")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Evaluate a trained model")
    parser.add_argument("--model-dir", default=LATEST_MODEL_DIR, help="Model directory")
    parser.add_argument("--data", default=FEATURES_CSV, help="Features CSV path")
    args = parser.parse_args()

    main(args.model_dir, args.data)
