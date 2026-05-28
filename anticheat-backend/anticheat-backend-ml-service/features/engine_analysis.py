"""
Stockfish analysis with MultiPV support.
Computes centipawn loss and top-N match for each move in a game.
"""

import chess
import chess.engine
import chess.pgn
import io
import threading
from queue import Queue, Empty
from typing import List, Dict, Optional


class MultiPVWorker(threading.Thread):
    """Stockfish worker that returns top-N lines per position."""

    def __init__(self, engine_path: str, depth: int, multipv: int,
                 task_queue: Queue, result_queue: Queue):
        super().__init__(daemon=True)
        self.engine_path = engine_path
        self.depth = depth
        self.multipv = multipv
        self.task_queue = task_queue
        self.result_queue = result_queue
        self.engine = chess.engine.SimpleEngine.popen_uci(self.engine_path)

    def run(self):
        while True:
            try:
                task_id, fen, played_move_uci = self.task_queue.get(timeout=3)
                board = chess.Board(fen)
                color = board.turn

                infos = self.engine.analyse(
                    board,
                    chess.engine.Limit(depth=self.depth),
                    multipv=self.multipv,
                )

                top_moves = []
                for info in infos:
                    pv = info.get("pv")
                    score = info.get("score")
                    if pv and score:
                        move_uci = pv[0].uci()
                        cp = score.pov(color)
                        value = cp.score(mate_score=10000)
                        top_moves.append({"move_uci": move_uci, "score_cp": value})

                best_score = top_moves[0]["score_cp"] if top_moves else 0

                # Evaluate the move that was actually played
                played_score = None
                for tm in top_moves:
                    if tm["move_uci"] == played_move_uci:
                        played_score = tm["score_cp"]
                        break

                if played_score is None:
                    # Played move is not in the top-N: evaluate it separately
                    board.push(chess.Move.from_uci(played_move_uci))
                    after_info = self.engine.analyse(
                        board,
                        chess.engine.Limit(depth=self.depth),
                    )
                    after_score = after_info["score"].pov(color)
                    played_score = after_score.score(mate_score=10000)
                    board.pop()

                cpl = max(0, best_score - played_score)

                top_move_ucis = [tm["move_uci"] for tm in top_moves]
                top1_match = played_move_uci == top_move_ucis[0] if top_move_ucis else False
                top3_match = played_move_uci in top_move_ucis[:3]
                top5_match = played_move_uci in top_move_ucis[:5]

                self.result_queue.put((task_id, {
                    "cpl": cpl,
                    "top1_match": top1_match,
                    "top3_match": top3_match,
                    "top5_match": top5_match,
                    "eval_best": best_score,
                    "eval_played": played_score,
                    "played_move": played_move_uci,
                }))

            except Empty:
                continue
            except Exception as e:
                self.result_queue.put((task_id, {
                    "cpl": 0, "top1_match": False, "top3_match": False,
                    "top5_match": False, "eval_best": 0, "eval_played": 0,
                    "played_move": played_move_uci, "error": str(e),
                }))

    def close(self):
        self.engine.quit()


class EngineAnalysisPool:
    """Pool of Stockfish workers for MultiPV analysis."""

    def __init__(self, engine_path: str, max_workers: int = 8,
                 depth: int = 12, multipv: int = 5):
        self.task_queue = Queue()
        self.result_queue = Queue()
        self.workers = [
            MultiPVWorker(engine_path, depth, multipv, self.task_queue, self.result_queue)
            for _ in range(max_workers)
        ]
        for w in self.workers:
            w.start()

    def analyze_game(self, pgn_string: str) -> Dict[str, List[Dict]]:
        """
        Analyze all moves in a game, returning per-move data split by color.

        Returns:
            {"white": [move_data, ...], "black": [move_data, ...]}
        """
        game = chess.pgn.read_game(io.StringIO(pgn_string))
        if game is None:
            return {"white": [], "black": []}

        board = game.board()
        tasks = []

        for move_idx, move in enumerate(game.mainline_moves()):
            fen_before = board.fen()
            uci = move.uci()
            self.task_queue.put((move_idx, fen_before, uci))
            tasks.append((move_idx, chess.WHITE if board.turn == chess.WHITE else chess.BLACK))
            board.push(move)

        results = {}
        while len(results) < len(tasks):
            task_id, data = self.result_queue.get()
            results[task_id] = data

        white_moves = []
        black_moves = []
        for move_idx, color in tasks:
            data = results[move_idx]
            if color == chess.WHITE:
                white_moves.append(data)
            else:
                black_moves.append(data)

        return {"white": white_moves, "black": black_moves}

    def shutdown(self):
        for w in self.workers:
            w.close()
