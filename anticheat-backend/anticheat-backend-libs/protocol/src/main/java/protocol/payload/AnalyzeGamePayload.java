package protocol.payload;

public record AnalyzeGamePayload(String moves) {
    public AnalyzeGamePayload {
        if (moves == null || moves.isBlank()) throw new IllegalArgumentException("Moves are required");
    }
}
