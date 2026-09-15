package com.orte.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ValidationErrorResponse(
        int status,
        String code,
        String message,
        String path,
        LocalDateTime timestamp,
        Map<String, String> errors
) {
}
