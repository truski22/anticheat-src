package com.chessfraud.analysisservice.grpc;

import com.chessfraud.analysisservice.dto.analysis.AnalysisResult;
import com.chessfraud.analysisservice.service.analysis.GameAnalysisService;
import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import com.chessfraud.grpc.analysis.AnalyzeGameRequest;
import com.chessfraud.grpc.analysis.AnalyzeGameResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the contract from anticheat-backend-libs/grpc-api/analysis_service.proto
 * (same default host/port "analysis-service:9092" expected by
 * anticheat-backend-gateway/grpc/ServiceClients.java).
 */
@GrpcService
public class AnalysisGrpcService extends AnalysisServiceGrpc.AnalysisServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(AnalysisGrpcService.class);

    private final GameAnalysisService analysisService;

    public AnalysisGrpcService(GameAnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Override
    public void analyzeGame(AnalyzeGameRequest request, StreamObserver<AnalyzeGameResponse> responseObserver) {
        String user = request.getUser();
        String moves = request.getMoves();

        if (user.isEmpty() || moves.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("user and moves are required")
                    .asRuntimeException());
            return;
        }

        try {
            AnalysisResult result = analysisService.analyze(moves);

            AnalyzeGameResponse response = AnalyzeGameResponse.newBuilder()
                    .setWhiteLegal(result.isWhiteLegal())
                    .setBlackLegal(result.isBlackLegal())
                    .addAllWhite(result.getDataWhite())
                    .addAllBlack(result.getDataBlack())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[ANALYSIS] Error analyzing game for user '{}': {}", user, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Analysis failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
