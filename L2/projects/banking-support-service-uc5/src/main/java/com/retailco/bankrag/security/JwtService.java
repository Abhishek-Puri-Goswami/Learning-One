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
 * This class creates and checks JWTs (JSON Web Tokens), implemented from
 * scratch using the HS256 signing algorithm. A JWT proves a request comes
 * from a specific, authenticated customer with specific roles — without
 * needing a server-side session store, since the token itself securely
 * carries that information. {@code JwtAuthenticationFilter} calls
 * {@code verify()} on every incoming request to figure out who's calling.
 * <p>
 * How a JWT is structured (see {@code issueToken()} below): it's three
 * parts joined by dots — {@code header.payload.signature}.
 * <ol>
 *   <li>The header: fixed info about which algorithm is used.</li>
 *   <li>The payload: the actual claims — who this is, what roles they
 *       have, when it was issued, and when it expires.</li>
 *   <li>The signature: a cryptographic "seal" computed from the header
 *       and payload using a secret key only the server knows. Anyone can
 *       READ a token's payload (it's just encoded, not encrypted), but
 *       only the server can produce a VALID signature — which is exactly
 *       what stops someone from tampering with it.</li>
 * </ol>
 * <p>
 * How {@code verify()} works, step by step: split the token into its 3
 * parts, recompute what the signature SHOULD be using our own secret key,
 * and compare it to the signature the token actually has. If they match,
 * read the claims out and check the token hasn't expired. Any mismatch or
 * expired token returns an {@code Invalid} result with a clear reason.
 * <p>
 * One important detail: the comparison in {@code constantTimeEquals}
 * deliberately checks every single byte before deciding match/no-match,
 * instead of stopping early at the first difference. A naive comparison
 * that returns as soon as it finds a mismatch can leak timing
 * information — a clever attacker measuring how LONG the check takes
 * could learn how many leading bytes were correct. Taking the same amount
 * of time regardless of where a mismatch occurs is a standard security
 * hardening technique, not something specific to JWTs.
 * <p>
 * Why this is all hand-written instead of using an existing library: this
 * module is intentionally dependency-free, and everything HS256 needs is
 * already built into the JDK.
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

    // This is a Java "sealed interface" — it means verify() can ONLY
    // ever return one of exactly these two types (Valid or Invalid),
    // never anything else. That lets code checking the result (with a
    // switch statement) be verified complete by the compiler — there's
    // no way to forget to handle one of the cases.
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
