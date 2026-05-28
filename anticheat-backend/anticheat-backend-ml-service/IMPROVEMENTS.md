# ML Service — Mejoras Propuestas

## Estado actual

- Modelo RandomForest pre-entrenado cargado desde `.joblib` al arrancar.
- 25+ features estadísticas (media, varianza, curtosis, entropía…) extraídas de las **evaluaciones absolutas** de Stockfish por posición.
- Dataset original: 125 partidas legales + 125 con trampas (250 total).
- No hay reentrenamiento, versionado, feedback, ni monitoreo.

## Problema principal: las features no miden lo correcto

El sistema actual evalúa **quién va ganando** en cada posición (evaluación absoluta: "+150cp para blancas"). Eso no mide si una jugada fue buena o mala.

Lo que los sistemas anticheat reales miden es **la calidad de cada jugada individual**:

```
Posición antes de la jugada:
  → Stockfish dice: mejor jugada = Nf3, evaluación = +150cp
  → Jugador jugó Nf3 → centipawn loss = 0 (jugó la mejor)
  → Jugador jugó Be2 → Stockfish evalúa Be2 = +80cp → centipawn loss = 70cp
```

Un humano ELO 1500 tiene centipawn loss promedio de ~80-120cp. Un cheater con engine: ~5-15cp.
**Esta feature sola es más potente que las 25 features estadísticas actuales combinadas.**

---

## Mejoras por prioridad

### 🥇 1. Feature engineering — centipawn loss y top-N match (MÁXIMO IMPACTO)

El mayor salto de precisión no viene de cambiar el algoritmo, sino de **mejorar las features**.

**Nuevas features por jugada:**
- **Centipawn loss (CPL)**: diferencia entre la evaluación de la jugada del jugador y la mejor jugada de Stockfish.
- **Top-1 match**: ¿el jugador jugó la mejor jugada del engine? (booleano)
- **Top-3 match**: ¿jugó una de las 3 mejores? (booleano)
- **Top-5 match**: ¿jugó una de las 5 mejores? (booleano)

**Features agregadas por partida (a partir de las anteriores):**
- CPL promedio, mediana, varianza, percentiles.
- % de jugadas que coinciden con top-1, top-3, top-5 de Stockfish.
- CPL por fase de juego (apertura, medio juego, final).
- CPL separado por color (blancas vs. negras).
- Racha máxima de jugadas perfectas consecutivas (top-1).

**Cambio en Stockfish:** actualmente se analiza cada posición con `analyse()`. Para CPL se necesita analizar la posición **antes y después** de la jugada del jugador, o usar `multipv` para obtener las N mejores jugadas.

**Esfuerzo:** Medio. Requiere modificar `StockfishPool` para pedir multipv y calcular CPL.

### 🥇 2. Ampliar dataset — generación sintética + Lichess

**Dataset actual:** 250 partidas. Es muy pequeño para un modelo robusto.

**Estrategia de ampliación combinada:**

#### a) Generación sintética controlada (labels 100% fiables)
- Engine débil (profundidad 4-6, simula humano ~1200-1500) juega contra sí mismo.
- En un % configurable de jugadas, se sustituye por Stockfish depth 20 (simula cheating).
- Perfiles variados:
  - Cheater completo: 100% jugadas engine.
  - Cheater intermitente: 20-50% jugadas engine, el resto humano.
  - Cheater selectivo: solo usa engine en posiciones críticas (evaluación cercana a 0).
- **Ventaja:** sabes exactamente cuáles son trampa. Sin ruido.

#### b) Lichess open database (volumen, con ruido)
- Descargar partidas de jugadores baneados por `TOS violation` (API: `GET /api/user/{username}` → `tosViolation: true`).
- ⚠️ **Problema:** no sabes en cuáles partidas específicas hizo trampa. Un jugador baneado puede haber hecho trampa en 5 de 20 partidas.
- **Mitigación:** filtrar con heurísticas (CPL bajo + alto % top-1 match) para quedarte solo con las más sospechosas.
- **Realidad del campo:** la mayoría de papers académicos usan este enfoque con noisy labels. RandomForest/XGBoost toleran ~20-30% de ruido en labels.

#### c) Partidas legales (fácil de conseguir)
- APIs de Lichess/Chess.com con jugadores activos no baneados.
- Volumen ilimitado.

**Objetivo:** pasar de 250 a 2000+ partidas.

### 🥇 3. Pipeline de reentrenamiento (`train.py`)

Script que automatiza: cargar datos → extraer features → entrenar → evaluar → guardar modelo versionado.

```
training/
├── train.py              # Pipeline principal
├── evaluate.py           # Métricas y reportes
├── config.py             # Hiperparámetros, paths
├── generate_synthetic.py # Generación de partidas sintéticas
└── data/                 # Datasets
    ├── confirmed/        # Tus 125 legales + 125 trampas
    ├── synthetic/        # Generadas
    └── lichess/          # Descargadas
models/
├── v1/
│   ├── model.joblib
│   └── metadata.json     # fecha, métricas, hash dataset, features
└── latest -> v1/
```

**Esfuerzo:** Medio.

### 🥈 4. Feedback loop (`POST /feedback`)

- Endpoint para que un moderador confirme/corrija predicciones: `{game_id, true_label}`.
- Alimenta el dataset de reentrenamiento con labels reales de producción.
- A largo plazo es la fuente más valiosa de datos etiquetados.

**Esfuerzo:** Bajo.

### 🥈 5. Versionado de modelos con metadata

- Cada modelo guardado con: fecha, métricas (accuracy, F1, AUC, precision, recall), hash del dataset, features usadas, hiperparámetros.
- Opción simple: `models/v1/metadata.json`.
- Opción pro: MLflow o DVC.

**Esfuerzo:** Bajo.

### 🥉 6. Monitoreo y observabilidad

- **Data drift:** comparar distribución de features en producción vs. entrenamiento (`evidently` o `alibi-detect`).
- **Confidence monitoring:** alertar si la confianza media baja de un umbral.
- Endpoint `GET /model/metrics`.

**Esfuerzo:** Medio.

### 🥉 7. Ensembles y modelos alternativos

- Probar XGBoost/LightGBM además del RandomForest.
- Ensemble de varios modelos.
- Cross-validation con métricas robustas.
- **Nota:** esto importa MENOS que mejorar features y datos. Un mal feature set con XGBoost no supera un buen feature set con RandomForest.

**Esfuerzo:** Medio.

### 🏅 8. A/B testing de modelos

- Cargar 2 versiones simultáneamente, comparar en producción.

**Esfuerzo:** Alto.

### 🏅 9. Calibración de probabilidades

- `CalibratedClassifierCV` de sklearn para que `confidence` refleje probabilidad real.

**Esfuerzo:** Bajo.

---

## Roadmap recomendado

| Fase | Qué hacer | Por qué |
|------|-----------|---------|
| **Fase 1** | Feature engineering (CPL + top-N match) | Mayor salto de precisión posible |
| **Fase 2** | Ampliar dataset (sintético + Lichess) | Más datos = modelo más robusto |
| **Fase 3** | Pipeline `train.py` + versionado | Poder iterar rápido sobre mejoras |
| **Fase 4** | Feedback loop + monitoreo | Mejora continua en producción |
| **Fase 5** | Ensemble / A/B testing | Optimización fina |
