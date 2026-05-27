package grpc;

import com.chessfraud.grpc.analysis.AnalyzeGameRequest;
import com.chessfraud.grpc.analysis.AnalyzeGameResponse;
import com.chessfraud.grpc.analysis.AnalysisServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.AnalysisResult;
import service.GameAnalysisService;

public class AnalysisServiceGrpcImpl extends AnalysisServiceGrpc.AnalysisServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(AnalysisServiceGrpcImpl.class);
    private final GameAnalysisService analysisService;

    public AnalysisServiceGrpcImpl(GameAnalysisService analysisService) {
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
                    .setLegal(result.isLegal())
                    .addAllWhite(result.getDataWhite())
                    .addAllBlack(result.getDataBlack())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("[GRPC] Error analyzing game for user '{}': {}", user, e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Analysis failed: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
