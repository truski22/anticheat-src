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
ENV AUTH_TOKEN=secreto123

EXPOSE 5002
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "5002"]
