package com.azadi.common;

import java.time.Instant;

public record ErrorResponse(int status, String message, Instant timestamp) {
}
