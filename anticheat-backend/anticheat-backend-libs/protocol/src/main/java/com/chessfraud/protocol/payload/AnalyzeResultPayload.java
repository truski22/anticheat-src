package com.chessfraud.protocol.payload;

import java.util.List;

public record AnalyzeResultPayload(boolean whiteLegal, boolean blackLegal, List<Integer> white, List<Integer> black) {}
