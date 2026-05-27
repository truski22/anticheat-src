package auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class JwtService {
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public JwtService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "[AUTH] FATAL: JWT_SECRET environment variable is not set. " +
                "Set it to a strong random secret (e.g. openssl rand -hex 32)."
            );
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.verifier = JWT.require(algorithm).build();
    }

    public String generateToken(String username) {
        return JWT.create()
            .withSubject(username)
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
            .sign(algorithm);
    }

    public String validateToken(String token) throws JWTVerificationException {
        DecodedJWT decoded = verifier.verify(token);
        return decoded.getSubject();
    }
}
