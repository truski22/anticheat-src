package com.chessfraud.analysisservice.dto.analysis;

import java.util.List;

/**
 * Carries the result of a game-analysis call to the ML inference service.
 * Contains per-color cheat detection results.
 */
public class AnalysisResult {

    private final boolean whiteLegal;
    private final boolean blackLegal;
    private final List<Integer> dataWhite;
    private final List<Integer> dataBlack;

    public AnalysisResult(boolean whiteLegal, boolean blackLegal, List<Integer> dataWhite, List<Integer> dataBlack) {
        this.whiteLegal = whiteLegal;
        this.blackLegal = blackLegal;
        this.dataWhite = dataWhite;
        this.dataBlack = dataBlack;
    }

    /** @return {@code true} if the ML model classified white's play as fair */
    public boolean isWhiteLegal() {
        return whiteLegal;
    }

    /** @return {@code true} if the ML model classified black's play as fair */
    public boolean isBlackLegal() {
        return blackLegal;
    }

    /**
     * @return per-move evaluation scores for the white player as returned by the ML service
     */
    public List<Integer> getDataWhite() {
        return dataWhite;
    }

    /**
     * @return per-move evaluation scores for the black player as returned by the ML service
     */
    public List<Integer> getDataBlack() {
        return dataBlack;
    }
}
