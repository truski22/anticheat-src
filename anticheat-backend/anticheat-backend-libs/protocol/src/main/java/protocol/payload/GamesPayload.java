package protocol.payload;

import java.util.List;

public record GamesPayload(List<GameEntry> games) {
    public record GameEntry(String moves, boolean legal) {}
}
