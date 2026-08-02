package com.bidstream.api.error;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    String traceId = traceId();
    Map<String, String> details = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
    return response(
        HttpStatus.BAD_REQUEST, "validation_error", "Validation failed", details, traceId, ex);
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
    String traceId = traceId();
    return response(HttpStatus.NOT_FOUND, "not_found", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public ResponseEntity<ErrorResponse> handleNoResource(Exception ex) {
    String traceId = traceId();
    return response(HttpStatus.NOT_FOUND, "not_found", "Resource not found", Map.of(), traceId, ex);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    String traceId = traceId();
    return response(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal_error",
        "An unexpected error occurred",
        Map.of(),
        traceId,
        ex);
  }

  private ResponseEntity<ErrorResponse> response(
      HttpStatus status,
      String code,
      String message,
      Map<String, String> details,
      String traceId,
      Exception ex) {
    if (status.is5xxServerError()) {
      log.error("traceId={} code={} message={}", traceId, code, message, ex);
    } else {
      log.warn("traceId={} code={} message={}", traceId, code, message);
    }
    return ResponseEntity.status(status)
        .body(new ErrorResponse(new ErrorBody(code, message, details), traceId));
  }

  private String traceId() {
    String traceId = MDC.get("traceId");
    if (traceId == null || traceId.isBlank()) {
      traceId = UUID.randomUUID().toString();
      MDC.put("traceId", traceId);
    }
    return traceId;
  }
}
