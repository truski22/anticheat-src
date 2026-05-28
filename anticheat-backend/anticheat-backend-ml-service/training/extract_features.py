"""
Extract features from PGN files and produce a CSV dataset.

Usage:
    python extract_features.py --legales <path> --trampas <path> [--output <csv>]

This is SLOW (~1-3 hours for 250 games at depth 12, multipv 5, 10 workers).
Run once, then use the cached CSV for training.
"""

import argparse
import csv
import os
import sys
import time
import chess.pgn
import io

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from features.engine_analysis import EngineAnalysisPool
from features.aggregation import aggregate_color_features, FEATURE_NAMES
from training.config import (
    STOCKFISH_PATH, STOCKFISH_DEPTH, MULTIPV_TRAIN,
    STOCKFISH_WORKERS, FEATURES_CSV,
)


def read_pgn_file(path: str) -> str:
    with open(path, "r", encoding="utf-8", errors="replace") as f:
        return f.read()


def get_cheat_color(pgn_string: str):
    """
    Parse [Cheat "..."] header from PGN.
    Returns "white", "black", or None.
    """
    game = chess.pgn.read_game(io.StringIO(pgn_string))
    if game is None:
        return None
    cheat_header = game.headers.get("Cheat", "").strip().lower()
    if cheat_header in ("blancas", "white"):
        return "white"
    elif cheat_header in ("negras", "black"):
        return "black"
    return None


def extract_all(legales_dir: str, trampas_dir: str, output_csv: str):
    pool = EngineAnalysisPool(
        engine_path=STOCKFISH_PATH,
        max_workers=STOCKFISH_WORKERS,
        depth=STOCKFISH_DEPTH,
        multipv=MULTIPV_TRAIN,
    )

    rows = []
    csv_columns = ["source", "file", "color", "label"] + FEATURE_NAMES

    # -- Legal games --
    legal_files = sorted([f for f in os.listdir(legales_dir) if f.endswith(".pgn")])
    total = len(legal_files)
    print(f"Processing {total} legal games...")

    for i, fname in enumerate(legal_files, 1):
        pgn = read_pgn_file(os.path.join(legales_dir, fname))
        t0 = time.time()
        analysis = pool.analyze_game(pgn)
        elapsed = time.time() - t0

        for color in ("white", "black"):
            features = aggregate_color_features(analysis[color])
            row = {
                "source": "legal",
                "file": fname,
                "color": color,
                "label": 0,
            }
            row.update(features)
            rows.append(row)

        print(f"  [{i}/{total}] {fname} ({elapsed:.1f}s)")

    # -- Cheat games --
    cheat_files = sorted([f for f in os.listdir(trampas_dir) if f.endswith(".pgn")])
    total = len(cheat_files)
    print(f"\nProcessing {total} cheat games...")

    for i, fname in enumerate(cheat_files, 1):
        pgn = read_pgn_file(os.path.join(trampas_dir, fname))
        cheat_color = get_cheat_color(pgn)
        t0 = time.time()
        analysis = pool.analyze_game(pgn)
        elapsed = time.time() - t0

        for color in ("white", "black"):
            features = aggregate_color_features(analysis[color])
            label = 1 if color == cheat_color else 0
            row = {
                "source": "cheat",
                "file": fname,
                "color": color,
                "label": label,
            }
            row.update(features)
            rows.append(row)

        print(f"  [{i}/{total}] {fname} (cheat={cheat_color}) ({elapsed:.1f}s)")

    pool.shutdown()

    # -- Write CSV --
    os.makedirs(os.path.dirname(output_csv), exist_ok=True)
    with open(output_csv, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=csv_columns)
        writer.writeheader()
        writer.writerows(rows)

    # -- Summary --
    label_1 = sum(1 for r in rows if r["label"] == 1)
    label_0 = sum(1 for r in rows if r["label"] == 0)
    print(f"\nDone! Saved {len(rows)} rows to {output_csv}")
    print(f"  Label 0 (legit): {label_0}")
    print(f"  Label 1 (cheat): {label_1}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract features from PGN dataset")
    parser.add_argument("--legales", required=True, help="Path to legal games directory")
    parser.add_argument("--trampas", required=True, help="Path to cheat games directory")
    parser.add_argument("--output", default=FEATURES_CSV, help="Output CSV path")
    args = parser.parse_args()

    extract_all(args.legales, args.trampas, args.output)
