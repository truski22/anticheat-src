package protocol.payload;

import java.util.List;

public record AnalyzeResultPayload(boolean legal, List<Integer> white, List<Integer> black) {}
