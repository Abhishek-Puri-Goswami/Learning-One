package com.retailco.bankrag.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deliverable: "JWT validation workflow." L2 HLD UseCase3 functional scope:
 * "JWT-based authentication."
 *
 * A real, from-scratch HS256 JWT implementation using only
 * `javax.crypto.Mac` (pure JDK -- no `io.jsonwebtoken`/`jjwt`, no Spring
 * Security dependency needed for the signing/verification primitive
 * itself). This is possible without any third-party library because HS256
 * is simply Base64URL(header) + "." + Base64URL(payload), HMAC-SHA256
 * signed with a shared secret, Base64URL-encoded -- every piece of that is
 * already in the JDK. This is what let this module be ACTUALLY compiled
 * and run in this sandbox (Maven Central blocked, same limitation as every
 * other module in this submission) rather than only written and reviewed.
 *
 * Deliberately minimal/manual JSON construction for the header and claims
 * (same reasoning as L2/UC1 and UC2's hand-rolled JSON: no Jackson/Gson
 * reachable here) -- NOT a general-purpose JWT library, only what this use
 * case's token shape needs (sub, roles, iat, exp).
 */
public class JwtService {

    private static final String ALG_HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    private final byte[] secretKeyBytes;
    private final long expirySeconds;

    public JwtService(String secret, long expirySeconds) {
        if (secret == null || secret.length() < 32) {
            // HS256 signing keys should be at least 256 bits (32 bytes) per
            // RFC 7518 -- enforced here rather than silently accepting a weak
            // key, per L2 HLD UseCase3's "Prevent unauthorized access."
            throw new IllegalArgumentException("JWT secret must be at least 32 characters (256 bits) for HS256");
        }
        this.secretKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.expirySeconds = expirySeconds;
    }

    public record Claims(String subject, List<String> roles, long issuedAt, long expiresAt) {
        boolean isExpired(Instant now) {
            return now.getEpochSecond() > expiresAt;
        }
    }

    public String issueToken(String subject, List<String> roles) {
        Instant now = Instant.now();
        long iat = now.getEpochSecond();
        long exp = iat + expirySeconds;

        String payloadJson = buildPayloadJson(subject, roles, iat, exp);
        String headerB64 = B64.encodeToString(ALG_HEADER_JSON.getBytes(StandardCharsets.UTF_8));
        String payloadB64 = B64.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signingInput = headerB64 + "." + payloadB64;
        String signatureB64 = B64.encodeToString(hmacSha256(signingInput));

        return signingInput + "." + signatureB64;
    }

    public sealed interface VerificationResult permits Valid, Invalid {
    }

    public record Valid(Claims claims) implements VerificationResult {
    }

    public record Invalid(String reason) implements VerificationResult {
    }

    public VerificationResult verify(String token) {
        if (token == null || token.isBlank()) {
            return new Invalid("Token is missing");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return new Invalid("Malformed token: expected 3 dot-separated segments, found " + parts.length);
        }

        String signingInput = parts[0] + "." + parts[1];
        byte[] expectedSignature = hmacSha256(signingInput);
        byte[] providedSignature;
        try {
            providedSignature = B64D.decode(parts[2]);
        } catch (IllegalArgumentException e) {
            return new Invalid("Malformed signature encoding");
        }

        if (!constantTimeEquals(expectedSignature, providedSignature)) {
            // Tampering / wrong secret -- exercised for real in Main.java's
            // demo by presenting a token signed with a different secret.
            return new Invalid("Signature verification failed -- token was tampered with or signed by a different key");
        }

        Claims claims;
        try {
            claims = parsePayload(parts[1]);
        } catch (RuntimeException e) {
            return new Invalid("Malformed claims payload: " + e.getMessage());
        }

        if (claims.isExpired(Instant.now())) {
            return new Invalid("Token expired at " + Instant.ofEpochSecond(claims.expiresAt()));
        }

        return new Valid(claims);
    }

    /**
     * Constant-time comparison to avoid a timing side-channel on signature
     * verification -- a standard JWT-implementation hardening step, cheap
     * to include correctly and easy to get wrong with a naive `Arrays.equals`
     * short-circuit.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private byte[] hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKeyBytes, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // HmacSHA256 is a mandatory JDK algorithm (JCA standard names) --
            // this should be unreachable, but fail loudly rather than
            // silently if it ever isn't.
            throw new IllegalStateException("HmacSHA256 unavailable in this JVM", e);
        }
    }

    private String buildPayloadJson(String subject, List<String> roles, long iat, long exp) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"sub\":\"").append(escape(subject)).append("\",");
        sb.append("\"roles\":[");
        for (int i = 0; i < roles.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escape(roles.get(i))).append("\"");
        }
        sb.append("],");
        sb.append("\"iat\":").append(iat).append(",");
        sb.append("\"exp\":").append(exp);
        sb.append("}");
        return sb.toString();
    }

    /**
     * Minimal hand-rolled JSON object parser for exactly this payload shape
     * (flat object: string, string-array, and integer fields only) -- not a
     * general JSON parser, since no JSON library is reachable in this
     * sandbox and a full parser is out of scope for what this token payload
     * actually needs.
     */
    private Claims parsePayload(String payloadB64) {
        String json = new String(B64D.decode(payloadB64), StandardCharsets.UTF_8);
        Map<String, String> scalarFields = new LinkedHashMap<>();
        String subject = extractStringField(json, "sub");
        List<String> roles = extractStringArrayField(json, "roles");
        long iat = Long.parseLong(extractRawNumberField(json, "iat"));
        long exp = Long.parseLong(extractRawNumberField(json, "exp"));
        return new Claims(subject, roles, iat, exp);
    }

    private String extractStringField(String json, String key) {
        String marker = "\"" + key + "\":\"";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Missing field: " + key);
        start += marker.length();
        int end = json.indexOf('"', start);
        return json.substring(start, end);
    }

    private String extractRawNumberField(String json, String key) {
        String marker = "\"" + key + "\":";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Missing field: " + key);
        start += marker.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        return json.substring(start, end);
    }

    private List<String> extractStringArrayField(String json, String key) {
        String marker = "\"" + key + "\":[";
        int start = json.indexOf(marker);
        if (start < 0) throw new IllegalArgumentException("Missing field: " + key);
        start += marker.length();
        int end = json.indexOf(']', start);
        String inner = json.substring(start, end).trim();
        if (inner.isEmpty()) return List.of();
        String[] items = inner.split(",");
        return List.of(items).stream().map(s -> s.trim().replaceAll("^\"|\"$", "")).toList();
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
