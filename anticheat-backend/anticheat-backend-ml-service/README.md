# ML Service

Python FastAPI service that performs **per-color** chess fraud detection using a RandomForest classifier trained with CPL (centipawn loss) and top-N engine match features.

## Overview

Analyzes chess games by evaluating each position with Stockfish MultiPV, computing centipawn loss and engine agreement metrics per color, and classifying **each side independently** using a trained model. This tells you *who* cheated, not just *if* someone cheated.

Interactive API documentation is auto-generated at `/docs` (Swagger UI) and `/redoc`.

## API

### `POST /predict`

Analyze a chess game for fraud with per-color classification.

**Request:**
```json
{
  "moves": "1. e4 e5 2. Nf3 Nc6 ..."
}
```

**Response:**
```json
{
  "data_white": [30, 45, ...],
  "data_black": [-20, -35, ...],
  "white": { "prediction": 0, "confidence": 0.92 },
  "black": { "prediction": 1, "confidence": 0.87 }
}
```

| Prediction | Meaning |
|------------|---------|
| `0` | Legal (fair play) |
| `1` | Cheated |

### `POST /feedback`

Submit correction for a previous prediction (for future retraining).

### `GET /model/info`

Returns current model metadata (version, metrics, training date).

### `GET /model/reload`

Hot-reload the model from disk without restarting the service.

### `GET /health`

Health check endpoint (no auth required).

All endpoints except `/health` require a `Bearer` token via the `Authorization` header.

## Project Structure

```
ml-service/
├── main.py                  # FastAPI app (inference endpoints)
├── features/
│   ├── engine_analysis.py   # MultiPV Stockfish worker pool
│   └── aggregation.py       # Per-color feature aggregation (18 features)
├── training/
│   ├── config.py            # Centralized configuration
│   ├── extract_features.py  # PGN → CSV feature extraction
│   ├── train.py             # GridSearchCV training pipeline
│   └── evaluate.py          # Detailed model evaluation
├── models/
│   ├── latest/              # Promoted best model (used at runtime)
│   ├── v1/                  # Original legacy model
│   └── v2/                  # Current model (95% acc, 89.8% F1)
└── data/                    # Extracted features CSV
```

## Model

- **Algorithm:** RandomForest (scikit-learn), selected via GridSearchCV over XGBoost
- **Features:** 18 per-color features based on CPL statistics and top-N engine match rates
- **Metrics (v2):** 95% accuracy · 89.8% F1 · 91.7% precision · 88% recall · 98.9% AUC-ROC
- **Top features:** cpl_mean, cpl_p90, cpl_variance, top3_rate, zero_cpl_rate

## Training

```bash
# Extract features from PGN files
python -m training.extract_features --legales <path> --trampas <path>

# Train and evaluate
python -m training.train

# Detailed evaluation
python -m training.evaluate
```

## Environment Variables

| Variable | Description |
|----------|-------------|
| `STOCKFISH_PATH` | Path to the Stockfish binary |
| `AUTH_TOKEN` | Authentication token for API access |
| `MODEL_DIR` | Model directory (default: `models/latest`) |

## Requirements

- Python 3.11+
- Stockfish chess engine installed and accessible
- Dependencies listed in `requirements.txt`

## How to Run

```bash
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 5002
```

The service listens on **port 5002**.
