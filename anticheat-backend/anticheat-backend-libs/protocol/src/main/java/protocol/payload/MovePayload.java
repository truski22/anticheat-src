package protocol.payload;

public record MovePayload(String from, String to, String player, long timestamp) {}
