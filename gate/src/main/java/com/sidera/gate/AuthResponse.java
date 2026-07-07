package com.sidera.gate;

public record AuthResponse(String token, long expiresInSeconds) {
}
