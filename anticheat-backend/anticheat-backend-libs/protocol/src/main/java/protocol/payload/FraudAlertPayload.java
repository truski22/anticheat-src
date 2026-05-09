package protocol.payload;

public record FraudAlertPayload(String gameId, double fraudProbability, String reason) {}
