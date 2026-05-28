"""
Training pipeline: load features CSV → train models → evaluate → save best.

Usage:
    python train.py [--data <csv>] [--output-dir <models/>]
"""

import argparse
import hashlib
import json
import os
import sys
import time
from datetime import datetime, timezone

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, GridSearchCV
from sklearn.metrics import (
    accuracy_score, f1_score, precision_score, recall_score,
    roc_auc_score, classification_report, confusion_matrix,
)

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from features.aggregation import FEATURE_NAMES
from training.config import (
    SEED, TEST_SIZE, FEATURES_CSV, MODELS_DIR, LATEST_MODEL_DIR,
    RANDOM_FOREST_GRID, XGBOOST_GRID, STOCKFISH_DEPTH, MULTIPV_TRAIN,
)


def load_dataset(csv_path: str):
    df = pd.read_csv(csv_path)
    X = df[FEATURE_NAMES].values
    y = df["label"].values
    return X, y, df


def compute_file_hash(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def next_version(models_dir: str) -> str:
    existing = [d for d in os.listdir(models_dir)
                if os.path.isdir(os.path.join(models_dir, d)) and d.startswith("v")]
    if not existing:
        return "v2"
    nums = []
    for d in existing:
        try:
            nums.append(int(d[1:]))
        except ValueError:
            pass
    return f"v{max(nums) + 1}" if nums else "v2"


def train_random_forest(X_train, y_train):
    print("\n── Training RandomForest with GridSearchCV ──")
    rf = RandomForestClassifier(random_state=SEED)
    grid = GridSearchCV(
        rf, RANDOM_FOREST_GRID,
        cv=5, scoring="f1", n_jobs=-1, verbose=1,
    )
    grid.fit(X_train, y_train)
    print(f"  Best params: {grid.best_params_}")
    print(f"  Best CV F1:  {grid.best_score_:.4f}")
    return grid.best_estimator_, grid.best_params_, grid.best_score_


def train_xgboost(X_train, y_train):
    try:
        from xgboost import XGBClassifier
    except ImportError:
        print("\n── XGBoost not installed, skipping ──")
        return None, None, 0.0

    print("\n── Training XGBoost with GridSearchCV ──")
    xgb = XGBClassifier(random_state=SEED, eval_metric="logloss", use_label_encoder=False)
    grid = GridSearchCV(
        xgb, XGBOOST_GRID,
        cv=5, scoring="f1", n_jobs=-1, verbose=1,
    )
    grid.fit(X_train, y_train)
    print(f"  Best params: {grid.best_params_}")
    print(f"  Best CV F1:  {grid.best_score_:.4f}")
    return grid.best_estimator_, grid.best_params_, grid.best_score_


def evaluate_model(model, X_test, y_test, label: str = ""):
    y_pred = model.predict(X_test)
    y_proba = model.predict_proba(X_test)[:, 1]

    metrics = {
        "accuracy": float(accuracy_score(y_test, y_pred)),
        "f1": float(f1_score(y_test, y_pred)),
        "precision": float(precision_score(y_test, y_pred)),
        "recall": float(recall_score(y_test, y_pred)),
        "auc_roc": float(roc_auc_score(y_test, y_proba)),
    }

    print(f"\n── Evaluation{' (' + label + ')' if label else ''} ──")
    print(f"  Accuracy:  {metrics['accuracy']:.4f}")
    print(f"  F1:        {metrics['f1']:.4f}")
    print(f"  Precision: {metrics['precision']:.4f}")
    print(f"  Recall:    {metrics['recall']:.4f}")
    print(f"  AUC-ROC:   {metrics['auc_roc']:.4f}")
    print(f"\n  Confusion Matrix:\n{confusion_matrix(y_test, y_pred)}")
    print(f"\n{classification_report(y_test, y_pred, target_names=['legit', 'cheat'])}")

    return metrics


def save_model(model, version: str, algorithm: str, metrics: dict,
               hyperparams: dict, dataset_hash: str, dataset_size: int,
               models_dir: str):
    version_dir = os.path.join(models_dir, version)
    os.makedirs(version_dir, exist_ok=True)

    model_path = os.path.join(version_dir, "model.joblib")
    joblib.dump(model, model_path)

    metadata = {
        "version": version,
        "algorithm": algorithm,
        "trained_at": datetime.now(timezone.utc).isoformat(),
        "dataset_hash": dataset_hash,
        "dataset_size": dataset_size,
        "test_size": TEST_SIZE,
        "seed": SEED,
        "metrics": metrics,
        "hyperparams": hyperparams,
        "features": FEATURE_NAMES,
        "stockfish_depth": STOCKFISH_DEPTH,
        "multipv": MULTIPV_TRAIN,
    }

    meta_path = os.path.join(version_dir, "metadata.json")
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2, ensure_ascii=False)

    print(f"\nModel saved: {model_path}")
    print(f"Metadata saved: {meta_path}")
    return version_dir


def promote_to_latest(version_dir: str, latest_dir: str):
    """Copy model and metadata to latest/."""
    os.makedirs(latest_dir, exist_ok=True)
    for fname in ("model.joblib", "metadata.json"):
        src = os.path.join(version_dir, fname)
        dst = os.path.join(latest_dir, fname)
        if os.path.exists(src):
            import shutil
            shutil.copy2(src, dst)
    print(f"Promoted to latest: {latest_dir}")


def compare_with_previous(new_metrics: dict, latest_dir: str) -> bool:
    meta_path = os.path.join(latest_dir, "metadata.json")
    if not os.path.exists(meta_path):
        print("\nNo previous model found. New model is the first.")
        return True

    with open(meta_path, "r", encoding="utf-8") as f:
        prev_meta = json.load(f)

    prev_metrics = prev_meta.get("metrics", {})
    prev_f1 = prev_metrics.get("f1", 0)
    new_f1 = new_metrics.get("f1", 0)

    print(f"\n── Comparison with previous model ({prev_meta.get('version', '?')}) ──")
    print(f"  Previous F1: {prev_f1:.4f}")
    print(f"  New F1:      {new_f1:.4f}")

    if new_f1 > prev_f1:
        print("  ✅ New model is BETTER. Promoting.")
        return True
    elif new_f1 == prev_f1:
        print("  ⚠️  Same F1. Promoting (newer data).")
        return True
    else:
        print("  ❌ New model is WORSE. NOT promoting.")
        return False


def main(csv_path: str, models_dir: str):
    print(f"Loading dataset: {csv_path}")
    X, y, df = load_dataset(csv_path)
    print(f"  Total samples: {len(y)}")
    print(f"  Label 0 (legit): {sum(y == 0)}")
    print(f"  Label 1 (cheat): {sum(y == 1)}")

    dataset_hash = compute_file_hash(csv_path)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=TEST_SIZE, random_state=SEED, stratify=y,
    )
    print(f"  Train: {len(y_train)}, Test: {len(y_test)}")

    # Train both models
    rf_model, rf_params, rf_cv_f1 = train_random_forest(X_train, y_train)
    xgb_model, xgb_params, xgb_cv_f1 = train_xgboost(X_train, y_train)

    # Evaluate on test set
    rf_metrics = evaluate_model(rf_model, X_test, y_test, "RandomForest")

    best_model = rf_model
    best_metrics = rf_metrics
    best_params = rf_params
    best_algo = "RandomForest"

    if xgb_model is not None:
        xgb_metrics = evaluate_model(xgb_model, X_test, y_test, "XGBoost")
        if xgb_metrics["f1"] > rf_metrics["f1"]:
            print("\n🏆 XGBoost wins!")
            best_model = xgb_model
            best_metrics = xgb_metrics
            best_params = xgb_params
            best_algo = "XGBoost"
        else:
            print("\n🏆 RandomForest wins!")

    # Feature importance
    if hasattr(best_model, "feature_importances_"):
        importances = best_model.feature_importances_
        sorted_idx = np.argsort(importances)[::-1]
        print("\n── Feature Importance (top 10) ──")
        for rank, idx in enumerate(sorted_idx[:10], 1):
            print(f"  {rank}. {FEATURE_NAMES[idx]}: {importances[idx]:.4f}")

    # Save
    version = next_version(models_dir)
    latest_dir = os.path.join(models_dir, "latest")

    version_dir = save_model(
        best_model, version, best_algo, best_metrics,
        best_params, dataset_hash, len(y), models_dir,
    )

    # Compare and promote
    should_promote = compare_with_previous(best_metrics, latest_dir)
    if should_promote:
        promote_to_latest(version_dir, latest_dir)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Train cheat detection model")
    parser.add_argument("--data", default=FEATURES_CSV, help="Path to features CSV")
    parser.add_argument("--models-dir", default=MODELS_DIR, help="Models output directory")
    args = parser.parse_args()

    main(args.data, args.models_dir)
