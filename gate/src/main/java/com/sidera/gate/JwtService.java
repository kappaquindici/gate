package com.sidera.gate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private static final String SECRET = "lorenzo-gate-app-jwt-secret-lorenzo-20202-secure";
	private static final Duration EXPIRATION = Duration.ofHours(12);
	private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

	public String generateToken(String username) {
		Instant now = Instant.now();
		long issuedAt = now.getEpochSecond();
		long expiresAt = now.plus(EXPIRATION).getEpochSecond();

		try {
			String header = encodeJson("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
			String payload = encodeJson(
				"{\"sub\":\"" + escapeJson(username) + "\",\"iat\":" + issuedAt + ",\"exp\":" + expiresAt + "}");

			String content = header + "." + payload;
			String signature = sign(content);
			return content + "." + signature;
		} catch (Exception exception) {
			throw new IllegalStateException("Impossibile generare il token JWT.", exception);
		}
	}

	public boolean isValid(String token) {
		try {
			String payload = extractPayloadJson(token);
			Long expiration = extractLongClaim(payload, "exp");
			if (expiration == null) {
				return false;
			}

			return expiration > Instant.now().getEpochSecond();
		} catch (Exception exception) {
			return false;
		}
	}

	public String extractUsername(String token) {
		try {
			String payload = extractPayloadJson(token);
			return extractStringClaim(payload, "sub");
		} catch (Exception exception) {
			return null;
		}
	}

	public long getExpirationInSeconds() {
		return EXPIRATION.toSeconds();
	}

	private String extractPayloadJson(String token) throws Exception {
		String[] parts = token.split("\\.");
		if (parts.length != 3) {
			throw new IllegalArgumentException("Token JWT non valido.");
		}

		String content = parts[0] + "." + parts[1];
		String expectedSignature = sign(content);
		if (!expectedSignature.equals(parts[2])) {
			throw new IllegalArgumentException("Firma JWT non valida.");
		}

		byte[] payloadBytes = URL_DECODER.decode(parts[1]);
		return new String(payloadBytes, StandardCharsets.UTF_8);
	}

	private String encodeJson(String json) {
		return URL_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private String sign(String content) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		mac.init(secretKeySpec);
		byte[] signatureBytes = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
		return URL_ENCODER.encodeToString(signatureBytes);
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private String extractStringClaim(String payload, String claimName) {
		String search = "\"" + claimName + "\":\"";
		int start = payload.indexOf(search);
		if (start < 0) {
			return null;
		}

		int valueStart = start + search.length();
		int valueEnd = payload.indexOf('"', valueStart);
		if (valueEnd < 0) {
			return null;
		}

		return payload.substring(valueStart, valueEnd)
			.replace("\\\"", "\"")
			.replace("\\\\", "\\");
	}

	private Long extractLongClaim(String payload, String claimName) {
		String search = "\"" + claimName + "\":";
		int start = payload.indexOf(search);
		if (start < 0) {
			return null;
		}

		int valueStart = start + search.length();
		int valueEnd = valueStart;
		while (valueEnd < payload.length() && Character.isDigit(payload.charAt(valueEnd))) {
			valueEnd++;
		}

		if (valueStart == valueEnd) {
			return null;
		}

		return Long.parseLong(payload.substring(valueStart, valueEnd));
	}
}
