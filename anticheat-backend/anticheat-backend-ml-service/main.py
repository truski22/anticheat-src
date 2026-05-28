import json
import os
import io
from typing import List, Optional
from contextlib import asynccontextmanager
from datetime import datetime, timezone

import joblib
import numpy as np
from fastapi import FastAPI, HTTPException, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from pydantic import BaseModel, Field

from features.engine_analysis import EngineAnalysisPool
from features.aggregation import aggregate_color_features, FEATURE_NAMES

# ── Configuration ──────────────────────────────────────────────────────

STOCKFISH_PATH = os.getenv("STOCKFISH_PATH", "stockfish")
AUTH_TOKEN = os.getenv("AUTH_TOKEN", "secreto123")
STOCKFISH_WORKERS = int(os.getenv("STOCKFISH_WORKERS", "10"))
STOCKFISH_DEPTH = 12
MULTIPV_PROD = 3
MODELS_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models")
FEEDBACK_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)), "data", "feedback.jsonl")

# ── Model Loading ──────────────────────────────────────────────────────

def load_model(model_dir: str = None):
    if model_dir is None:
        model_dir = os.path.join(MODELS_DIR, "latest")
    model_path = os.path.join(model_dir, "model.joblib")
    meta_path = os.path.join(model_dir, "metadata.json")

    loaded = joblib.load(model_path)

    metadata = None
    if os.path.exists(meta_path):
        with open(meta_path, "r", encoding="utf-8") as f:
            metadata = json.load(f)

    return loaded, metadata


model, model_metadata = load_model()

# ── Auth ───────────────────────────────────────────────────────────────

security = HTTPBearer()

def verify_token(credentials: HTTPAuthorizationCredentials = Security(security)):
    if credentials.credentials != AUTH_TOKEN:
        raise HTTPException(status_code=401, detail="Unauthorized")
    return credentials.credentials

# ── Engine Pool ────────────────────────────────────────────────────────

analysis_pool = EngineAnalysisPool(
    engine_path=STOCKFISH_PATH,
    max_workers=STOCKFISH_WORKERS,
    depth=STOCKFISH_DEPTH,
    multipv=MULTIPV_PROD,
)

# ── Pydantic Models ────────────────────────────────────────────────────

class PredictionRequest(BaseModel):
    moves: str = Field(..., description="PGN-format game moves", min_length=1)

class ColorPrediction(BaseModel):
    prediction: int = Field(..., description="0 = legit, 1 = cheat")
    confidence: float = Field(..., description="Model confidence score")

class PredictionResponse(BaseModel):
    data_white: List[int] = Field(..., description="White move evaluations (centipawns)")
    data_black: List[int] = Field(..., description="Black move evaluations (centipawns)")
    white: ColorPrediction = Field(..., description="Prediction for white player")
    black: ColorPrediction = Field(..., description="Prediction for black player")

class FeedbackRequest(BaseModel):
    game_id: str = Field(..., description="Unique game identifier")
    cheat_color: Optional[str] = Field(None, description="'white', 'black', or null if legit")
    notes: Optional[str] = Field(None, description="Optional notes")

class ModelInfoResponse(BaseModel):
    version: Optional[str] = None
    algorithm: Optional[str] = None
    trained_at: Optional[str] = None
    features: Optional[List[str]] = None
    metrics: Optional[dict] = None

# ── App ────────────────────────────────────────────────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    yield
    analysis_pool.shutdown()

app = FastAPI(
    title="Chess Fraud Detection — ML Service",
    description="Analyzes chess games for potential fraud using Stockfish evaluation and ML classification. "
                "Classifies each color independently.",
    version="3.0.0",
    lifespan=lifespan,
)


def _predict_color(move_data: list) -> dict:
    """Run feature extraction + model prediction for one color."""
    features = aggregate_color_features(move_data)
    input_vector = np.array([features.get(f, 0) for f in FEATURE_NAMES]).reshape(1, -1)

    prediction = model.predict(input_vector)[0]
    proba = model.predict_proba(input_vector)[0][prediction]

    return {"prediction": int(prediction), "confidence": float(proba)}


def _analyze(moves_pgn: str) -> dict:
    """Core analysis: engine analysis → features → per-color predictions."""
    analysis = analysis_pool.analyze_game(moves_pgn)

    if not analysis["white"] and not analysis["black"]:
        raise HTTPException(status_code=400, detail="No valid moves provided.")

    white_evals = [m["eval_played"] for m in analysis["white"]]
    black_evals = [m["eval_played"] for m in analysis["black"]]

    white_pred = _predict_color(analysis["white"])
    black_pred = _predict_color(analysis["black"])

    return {
        "data_white": white_evals,
        "data_black": black_evals,
        "white": white_pred,
        "black": black_pred,
    }


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict", response_model=PredictionResponse, dependencies=[Security(verify_token)])
def predict(request: PredictionRequest):
    """Analyze a chess game for fraud. Returns per-color predictions."""
    return _analyze(request.moves)


@app.get("/model/info", response_model=ModelInfoResponse, dependencies=[Security(verify_token)])
def model_info():
    """Return information about the currently loaded model."""
    if model_metadata:
        return ModelInfoResponse(
            version=model_metadata.get("version"),
            algorithm=model_metadata.get("algorithm"),
            trained_at=model_metadata.get("trained_at"),
            features=model_metadata.get("features"),
            metrics=model_metadata.get("metrics"),
        )
    return ModelInfoResponse()


@app.post("/model/reload", dependencies=[Security(verify_token)])
def reload_model():
    """Hot-reload the model from models/latest/ without restarting."""
    global model, model_metadata
    try:
        model, model_metadata = load_model()
        version = model_metadata.get("version", "unknown") if model_metadata else "unknown"
        return {"status": "reloaded", "version": version}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to reload model: {str(e)}")


@app.post("/feedback", dependencies=[Security(verify_token)])
def submit_feedback(request: FeedbackRequest):
    """Submit feedback on a prediction to improve future models."""
    entry = {
        "game_id": request.game_id,
        "cheat_color": request.cheat_color,
        "notes": request.notes,
        "submitted_at": datetime.now(timezone.utc).isoformat(),
    }

    os.makedirs(os.path.dirname(FEEDBACK_FILE), exist_ok=True)
    with open(FEEDBACK_FILE, "a", encoding="utf-8") as f:
        f.write(json.dumps(entry, ensure_ascii=False) + "\n")

    return {"status": "recorded", "game_id": request.game_id}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5002)
