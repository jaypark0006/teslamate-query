package com.teslamate.query.exception;

import com.teslamate.query.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException ex, ServerHttpRequest req) {
        log.warn("404 {} {}", path(req), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("NOT_FOUND", ex.getMessage(), Instant.now(), path(req)));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> responseStatus(ResponseStatusException ex, ServerHttpRequest req) {
        int code = ex.getStatusCode().value();
        String reason = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        if (code == 404) {
            log.warn("404 {} {}", path(req), reason);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", reason, Instant.now(), path(req)));
        }
        if (ex.getStatusCode().is4xxClientError()) {
            log.warn("{} {} {}", code, path(req), reason);
            return ResponseEntity.status(ex.getStatusCode())
                    .body(new ErrorResponse("BAD_REQUEST", reason, Instant.now(), path(req)));
        }
        log.error("{} {} {}", code, path(req), reason);
        return ResponseEntity.status(ex.getStatusCode())
                .body(new ErrorResponse("INTERNAL_ERROR", "Internal server error", Instant.now(), path(req)));
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException ex, ServerHttpRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage(), Instant.now(), path(req)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> argumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, ServerHttpRequest req) {
        return badType(ex.getName(), ex.getValue(), req);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<ErrorResponse> webInput(ServerWebInputException ex, ServerHttpRequest req) {
        if (ex.getCause() instanceof TypeMismatchException tme) {
            return typeMismatch(tme, req);
        }
        String message = ex.getReason() != null ? ex.getReason() : "Bad request";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", message, Instant.now(), path(req)));
    }

    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<ErrorResponse> typeMismatch(TypeMismatchException ex, ServerHttpRequest req) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        return badType(name, ex.getValue(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex, ServerHttpRequest req) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", msg, Instant.now(), path(req)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generic(Exception ex, ServerHttpRequest req) {
        log.error("Unhandled error on {}", path(req), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "Internal server error", Instant.now(), path(req)));
    }

    private static ResponseEntity<ErrorResponse> badType(String name, Object value, ServerHttpRequest req) {
        String raw = value == null ? "null" : value.toString();
        String message = name + " must be a number, got: " + raw;
        if (raw.contains("${") || raw.contains("$car")) {
            message += " (Grafana did not substitute this variable — add a dashboard variable with that name)";
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("BAD_REQUEST", message, Instant.now(), path(req)));
    }

    private static String path(ServerHttpRequest req) {
        return req.getPath().value();
    }
}
