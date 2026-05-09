package protocol.payload;

public record SaveGamePayload(String moves, boolean legal) {
    public SaveGamePayload {
        if (moves == null || moves.isBlank()) throw new IllegalArgumentException("Moves are required");
    }
}
