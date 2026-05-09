import chess
import chess.engine
import chess.pgn
import threading
from flask import Flask, request, jsonify
from queue import Queue, Empty
import os
import io
import joblib
import numpy as np
from scipy.stats import entropy, kurtosis, skew
from scipy.signal import periodogram

app = Flask(__name__)

model = joblib.load("modelo_RandomForest_Top5_RF_Importances.joblib")
# Configuración
STOCKFISH_PATH = os.getenv("STOCKFISH_PATH", "stockfish")
AUTH_TOKEN = os.getenv("AUTH_TOKEN", "secreto123")  # Token de autenticación

# Verificación de token de autenticación
@app.before_request
def verificar_token():
    if request.method == "POST":
        auth_header = request.headers.get("Authorization", "")
        if not auth_header.startswith("Bearer ") or auth_header.split(" ", 1)[1] != AUTH_TOKEN:
            return jsonify({"error": "Unauthorized"}), 401

# Trabajador persistente de Stockfish
class PersistentStockfishWorker(threading.Thread):
    def __init__(self, engine_path, depth, task_queue, result_queue):
        super().__init__()
        self.engine_path = engine_path
        self.depth = depth
        self.task_queue = task_queue
        self.result_queue = result_queue
        self.daemon = True
        self.engine = chess.engine.SimpleEngine.popen_uci(self.engine_path)

    def run(self):
        while True:
            try:
                task_id, fen, color = self.task_queue.get(timeout=3)
                board = chess.Board(fen)
                info = self.engine.analyse(board, chess.engine.Limit(depth=self.depth))
                score = info["score"].pov(color)
                value = score.score(mate_score=10000) if score.is_mate() else score.score()
                self.result_queue.put((task_id, value))
            except Empty:
                continue
            except Exception:
                self.result_queue.put((task_id, 0))

    def close(self):
        self.engine.quit()

# Pool persistente de Stockfish
class StockfishPool:
    def __init__(self, max_workers=8, depth=12):
        self.engine_path = STOCKFISH_PATH
        self.depth = depth
        self.task_queue = Queue()
        self.result_queue = Queue()
        self.workers = [
            PersistentStockfishWorker(self.engine_path, self.depth, self.task_queue, self.result_queue)
            for _ in range(max_workers)
        ]
        for w in self.workers:
            w.start()

    def evaluate_all(self, boards):
        task_ids = []
        for i, (board, color) in enumerate(boards):
            fen = board.fen()
            self.task_queue.put((i, fen, color))
            task_ids.append(i)

        results = {}
        while len(results) < len(task_ids):
            task_id, score = self.result_queue.get()
            results[task_id] = score

        return [results[i] for i in task_ids]

    def shutdown(self):
        for w in self.workers:
            w.close()

sf_pool = StockfishPool(max_workers=10, depth=12)

# Generar tableros desde movimientos SAN
def build_boards(moves):
    boards = []
    board = chess.Board()
    for i, move in enumerate(moves):
        try:
            board.push_san(move)
            color = chess.WHITE if i % 2 == 0 else chess.BLACK
            boards.append((board.copy(), color))
        except ValueError:
            break
    return boards

def all_moves(pgn_string):
    game = chess.pgn.read_game(io.StringIO(pgn_string))
    moves = []
    board = game.board()
    for move in game.mainline_moves():
        san = board.san(move)
        moves.append(san)
        board.push(move)
    return moves

def calculate_metric(movs):
    movs = np.array(movs, dtype=np.float64)

    result = {}
    result["media_total"] = np.mean(movs)
    result["mediana_total"] = np.median(movs)
    result["varianza_total"] = np.var(movs)
    result["curtosis_total"] = kurtosis(movs)
    result["asimetria_total"] = skew(movs)
    result["min_valor"] = np.min(movs)
    result["max_valor"] = np.max(movs)
    result["rango"] = result["max_valor"] - result["min_valor"]
    result["iqr"] = np.percentile(movs, 75) - np.percentile(movs, 25)
    result["cv"] = np.std(movs) / np.mean(movs) if np.mean(movs) != 0 else 0

    for p in [25, 50, 75, 90]:
        result[f"percentil_{p}"] = np.percentile(movs, p)

    deltas = np.diff(movs)
    result["deltas_promedio"] = np.mean(deltas) if len(deltas) else 0
    result["variabilidad_entre_turnos"] = np.std(deltas) if len(deltas) else 0

    hist, _ = np.histogram(movs, bins=30, density=True)
    hist = hist[hist > 0]
    result["entropia_evaluaciones"] = entropy(hist)

    signos = np.sign(movs)
    cambios_signo = np.sum(np.diff(signos) != 0)
    result["inversiones_ventaja"] = cambios_signo

    tercio = len(movs) // 3
    if tercio > 0:
        fases = [movs[:tercio], movs[tercio:2*tercio], movs[2*tercio:]]
        for nombre, valores in zip(["inicio", "medio", "final"], fases):
            result[f"media_{nombre}"] = np.mean(valores)
    else:
        result.update({"media_inicio": 0, "media_medio": 0, "media_final": 0})

    if len(movs) > 1:
        freqs, psd = periodogram(movs)
        result["freq_dominante"] = freqs[np.argmax(psd)] if len(freqs) else 0
        psd_sum = psd.sum()
        result["entropia_espectral"] = entropy(psd / psd_sum) if psd_sum != 0 else 0
    else:
        result["freq_dominante"] = 0
        result["entropia_espectral"] = 0

    return {k: (v.item() if isinstance(v, np.generic) else v) for k, v in result.items()}

@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"}), 200

@app.route("/eval", methods=["POST"])
def evaluate():
    data = request.get_json()
    moves = all_moves(data.get("moves", ""))
    boards = build_boards(moves)
    if not boards:
        return jsonify({"error": "No valid moves provided."}), 400

    results = sf_pool.evaluate_all(boards)
    white = [r for (b, c), r in zip(boards, results) if c == chess.WHITE]
    black = [r for (b, c), r in zip(boards, results) if c == chess.BLACK]

    features = calculate_metric(results)

    feature_order = model.feature_names_in_
    input_vector = np.array([features.get(f, 0) for f in feature_order]).reshape(1, -1)

    # Hacer predicción
    prediction = model.predict(input_vector)[0]
    proba = model.predict_proba(input_vector)[0][prediction]

    return jsonify({
        "data_white": white,
        "data_black": black,
        "prediction": int(prediction),
        "confidence": proba
    }),200

if __name__ == "__main__":
    from waitress import serve
    serve(app, host="0.0.0.0", port=5002)