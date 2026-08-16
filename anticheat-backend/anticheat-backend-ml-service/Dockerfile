FROM python:3.11-slim

RUN apt-get update && \
    apt-get install -y stockfish && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .
COPY features/ ./features/
COPY models/ ./models/

ENV STOCKFISH_PATH=/usr/games/stockfish
# AUTH_TOKEN must be provided at runtime (docker run -e / k8s Secret) — the app refuses to
# start without it, so no default is baked into the image.

EXPOSE 5002
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5002"]
