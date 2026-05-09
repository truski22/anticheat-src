package protocol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import protocol.payload.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PayloadRegistryTest {

    // ── Deserialization Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Deserialize valid LOGIN message")
    void deserializeLogin() throws InvalidMessageException {
        String json = """
            {"type":"LOGIN","messageId":"abc-123","payload":{"password":"secret"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        
        assertEquals(MessageType.LOGIN, msg.type());
        assertEquals("abc-123", msg.messageId());
        assertInstanceOf(LoginPayload.class, msg.payload());
        assertEquals("secret", ((LoginPayload) msg.payload()).password());
    }

    @Test
    @DisplayName("Deserialize valid REGISTER message")
    void deserializeRegister() throws InvalidMessageException {
        String json = """
            {"type":"REGISTER","payload":{"email":"test@example.com","password":"pass123"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        
        assertEquals(MessageType.REGISTER, msg.type());
        assertInstanceOf(RegisterPayload.class, msg.payload());
        RegisterPayload p = (RegisterPayload) msg.payload();
        assertEquals("test@example.com", p.email());
        assertEquals("pass123", p.password());
    }

    @Test
    @DisplayName("Deserialize USER_INFO_REQUEST with no payload")
    void deserializeUserInfoRequest() throws InvalidMessageException {
        String json = """
            {"type":"USER_INFO_REQUEST","messageId":"xyz"}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        
        assertEquals(MessageType.USER_INFO_REQUEST, msg.type());
        assertNull(msg.payload());
    }

    @Test
    @DisplayName("Deserialize GAMES_REQUEST with no payload")
    void deserializeGamesRequest() throws InvalidMessageException {
        String json = """
            {"type":"GAMES_REQUEST"}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        assertEquals(MessageType.GAMES_REQUEST, msg.type());
        assertNull(msg.payload());
    }

    @Test
    @DisplayName("Deserialize ANALYZE_GAME message")
    void deserializeAnalyzeGame() throws InvalidMessageException {
        String json = """
            {"type":"ANALYZE_GAME","payload":{"moves":"e2e4 e7e5 g1f3"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        
        assertInstanceOf(AnalyzeGamePayload.class, msg.payload());
        assertEquals("e2e4 e7e5 g1f3", ((AnalyzeGamePayload) msg.payload()).moves());
    }

    @Test
    @DisplayName("Deserialize SAVE_GAME message")
    void deserializeSaveGame() throws InvalidMessageException {
        String json = """
            {"type":"SAVE_GAME","payload":{"moves":"d2d4 d7d5","legal":true}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        
        assertInstanceOf(SaveGamePayload.class, msg.payload());
        SaveGamePayload p = (SaveGamePayload) msg.payload();
        assertEquals("d2d4 d7d5", p.moves());
        assertTrue(p.legal());
    }

    @Test
    @DisplayName("Deserialize CHANGE_PASSWORD message")
    void deserializeChangePassword() throws InvalidMessageException {
        String json = """
            {"type":"CHANGE_PASSWORD","payload":{"password":"newpass"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        assertInstanceOf(ChangePasswordPayload.class, msg.payload());
    }

    @Test
    @DisplayName("Deserialize CHANGE_PASSWORD_EMAIL message")
    void deserializeChangePasswordEmail() throws InvalidMessageException {
        String json = """
            {"type":"CHANGE_PASSWORD_EMAIL","payload":{"email":"a@b.com","password":"p"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        assertInstanceOf(ChangePasswordEmailPayload.class, msg.payload());
    }

    @Test
    @DisplayName("Deserialize SEND_EMAIL_CHANGE_PASSWORD message")
    void deserializeSendEmail() throws InvalidMessageException {
        String json = """
            {"type":"SEND_EMAIL_CHANGE_PASSWORD","payload":{"email":"x@y.com"}}
            """;
        ChessMessage msg = PayloadRegistry.deserialize(json);
        assertInstanceOf(SendEmailChangePasswordPayload.class, msg.payload());
    }

    // ── Error Handling Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("Reject message with missing type")
    void rejectMissingType() {
        String json = """
            {"messageId":"abc","payload":{"password":"secret"}}
            """;
        InvalidMessageException ex = assertThrows(InvalidMessageException.class,
            () -> PayloadRegistry.deserialize(json));
        assertEquals("MISSING_TYPE", ex.getCode());
    }

    @Test
    @DisplayName("Reject message with unknown type")
    void rejectUnknownType() {
        String json = """
            {"type":"UNKNOWN_TYPE","payload":{}}
            """;
        InvalidMessageException ex = assertThrows(InvalidMessageException.class,
            () -> PayloadRegistry.deserialize(json));
        assertEquals("UNKNOWN_TYPE", ex.getCode());
    }

    @Test
    @DisplayName("Reject LOGIN with missing payload")
    void rejectMissingPayload() {
        String json = """
            {"type":"LOGIN"}
            """;
        InvalidMessageException ex = assertThrows(InvalidMessageException.class,
            () -> PayloadRegistry.deserialize(json));
        assertEquals("MISSING_PAYLOAD", ex.getCode());
    }

    @Test
    @DisplayName("Reject LOGIN with invalid payload (missing required password)")
    void rejectInvalidPayload() {
        String json = """
            {"type":"LOGIN","payload":{"wrongField":"value"}}
            """;
        InvalidMessageException ex = assertThrows(InvalidMessageException.class,
            () -> PayloadRegistry.deserialize(json));
        assertEquals("INVALID_PAYLOAD", ex.getCode());
    }

    @Test
    @DisplayName("Reject malformed JSON")
    void rejectMalformedJson() {
        String json = "this is not json";
        InvalidMessageException ex = assertThrows(InvalidMessageException.class,
            () -> PayloadRegistry.deserialize(json));
        assertEquals("MALFORMED_JSON", ex.getCode());
    }

    // ── Serialization Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("Serialize and deserialize round-trip for LOGIN_RESPONSE")
    void serializeRoundTrip() throws InvalidMessageException {
        ChessMessage original = new ChessMessage(
            MessageType.LOGIN_RESPONSE, "msg-1",
            new ResponsePayload(true, "Authenticated"));
        
        String json = PayloadRegistry.serialize(original);
        
        assertNotNull(json);
        assertTrue(json.contains("LOGIN_RESPONSE"));
        assertTrue(json.contains("Authenticated"));
        assertTrue(json.contains("msg-1"));
    }

    @Test
    @DisplayName("Serialize ERROR message via factory method")
    void serializeError() {
        ChessMessage error = ChessMessage.error("TEST_CODE", "Test message", "corr-id");
        String json = PayloadRegistry.serialize(error);
        
        assertTrue(json.contains("ERROR"));
        assertTrue(json.contains("TEST_CODE"));
        assertTrue(json.contains("Test message"));
        assertTrue(json.contains("corr-id"));
    }

    @Test
    @DisplayName("Serialize GAMES response with game list")
    void serializeGames() {
        var games = List.of(
            new GamesPayload.GameEntry("e2e4 e7e5", true),
            new GamesPayload.GameEntry("d2d4 d7d5", false)
        );
        ChessMessage msg = new ChessMessage(MessageType.GAMES,
            new GamesPayload(games));
        
        String json = PayloadRegistry.serialize(msg);
        assertTrue(json.contains("GAMES"));
        assertTrue(json.contains("e2e4 e7e5"));
    }

    @Test
    @DisplayName("Serialize ANALYZE_RESULT with evaluation arrays")
    void serializeAnalyzeResult() {
        ChessMessage msg = new ChessMessage(MessageType.ANALYZE_RESULT,
            new AnalyzeResultPayload(true, List.of(20, 15, -5), List.of(-10, 5, -20)));
        
        String json = PayloadRegistry.serialize(msg);
        assertTrue(json.contains("ANALYZE_RESULT"));
        assertTrue(json.contains("true"));
    }

    @Test
    @DisplayName("Serialize USER_INFO with stats")
    void serializeUserInfo() {
        ChessMessage msg = new ChessMessage(MessageType.USER_INFO,
            new UserInfoPayload("test@x.com", 100, 5, 95));
        
        String json = PayloadRegistry.serialize(msg);
        assertTrue(json.contains("USER_INFO"));
        assertTrue(json.contains("test@x.com"));
        assertTrue(json.contains("100"));
    }

    // ── Payload Validation Tests ──────────────────────────────────────────

    @Test
    @DisplayName("LoginPayload rejects blank password")
    void loginPayloadValidation() {
        assertThrows(IllegalArgumentException.class, () -> new LoginPayload(""));
        assertThrows(IllegalArgumentException.class, () -> new LoginPayload(null));
    }

    @Test
    @DisplayName("RegisterPayload rejects blank email or password")
    void registerPayloadValidation() {
        assertThrows(IllegalArgumentException.class, () -> new RegisterPayload("", "pass"));
        assertThrows(IllegalArgumentException.class, () -> new RegisterPayload("email", ""));
    }

    @Test
    @DisplayName("AnalyzeGamePayload rejects blank moves")
    void analyzeGamePayloadValidation() {
        assertThrows(IllegalArgumentException.class, () -> new AnalyzeGamePayload(""));
    }

    @Test
    @DisplayName("SaveGamePayload rejects blank moves")
    void saveGamePayloadValidation() {
        assertThrows(IllegalArgumentException.class, () -> new SaveGamePayload("", true));
    }
}
