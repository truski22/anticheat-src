"""
Central configuration for training and inference.
"""

import os

# -- Reproducibility --
SEED = 42

# -- Stockfish --
STOCKFISH_PATH = os.getenv("STOCKFISH_PATH", "stockfish")
STOCKFISH_DEPTH = 12
MULTIPV_TRAIN = 5
MULTIPV_PROD = 3
STOCKFISH_WORKERS = int(os.getenv("STOCKFISH_WORKERS", "10"))

# -- Paths --
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
MODELS_DIR = os.path.join(BASE_DIR, "models")
LATEST_MODEL_DIR = os.path.join(MODELS_DIR, "latest")
FEATURES_CSV = os.path.join(DATA_DIR, "features.csv")

# -- Training --
TEST_SIZE = 0.2

RANDOM_FOREST_GRID = {
    "n_estimators": [100, 200, 500],
    "max_depth": [None, 10, 20, 30],
    "min_samples_split": [2, 5, 10],
    "min_samples_leaf": [1, 2, 4],
}

XGBOOST_GRID = {
    "n_estimators": [100, 200, 500],
    "max_depth": [3, 6, 10],
    "learning_rate": [0.01, 0.1, 0.2],
    "subsample": [0.8, 1.0],
}
