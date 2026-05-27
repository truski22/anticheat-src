# ML Service

Python FastAPI service that performs chess fraud detection using a RandomForest classifier and the Stockfish engine.

## Overview

Analyzes chess games by evaluating each move with Stockfish, extracting 25+ statistical features (mean, median, variance, kurtosis, skewness, entropy, etc.), and classifying the game using a pre-trained RandomForest model.

Interactive API documentation is auto-generated at `/docs` (Swagger UI) and `/redoc`.

## API

### `POST /eval` (legacy)

Evaluate a chess game for potential cheating. Kept for backward compatibility.

### `POST /predict`

Analyze a chess game for fraud (preferred endpoint, same logic as `/eval`).

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
  "prediction": 0,
  "confidence": 0.95
}
```

| Prediction | Meaning |
|------------|---------|
| `0` | Legal (fair play) |
| `1` | Cheated |

Both endpoints require a `Bearer` token via the `Authorization` header.

Request and response payloads are validated with Pydantic models (`PredictionRequest` / `PredictionResponse`).

### `GET /health`

Health check endpoint (no auth required).

## Model

- **File:** `modelo_RandomForest_Top5_RF_Importances.joblib`
- **Algorithm:** RandomForest (scikit-learn)
- **Features:** 25+ statistical features derived from per-move Stockfish evaluations

## Environment Variables

| Variable | Description |
|----------|-------------|
| `STOCKFISH_PATH` | Path to the Stockfish binary |
| `AUTH_TOKEN` | Authentication token for API access |

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
