package service;

import java.util.List;

/**
 * Carries the result of a game-analysis call to the ML inference service.
 */
public class AnalysisResult {

    private final boolean legal;
    private final List<Integer> dataWhite;
    private final List<Integer> dataBlack;

    public AnalysisResult(boolean legal, List<Integer> dataWhite, List<Integer> dataBlack) {
        this.legal     = legal;
        this.dataWhite = dataWhite;
        this.dataBlack = dataBlack;
    }

    /** @return {@code true} if the ML model classified the game as fair play */
    public boolean isLegal() { return legal; }

    /**
     * @return per-move evaluation scores for the white player as returned by the ML service
     */
    public List<Integer> getDataWhite() { return dataWhite; }

    /**
     * @return per-move evaluation scores for the black player as returned by the ML service
     */
    public List<Integer> getDataBlack() { return dataBlack; }
}
