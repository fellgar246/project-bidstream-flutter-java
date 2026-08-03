package com.bidstream.api.error;

import com.bidstream.domain.auth.AlreadySellerException;
import com.bidstream.domain.auth.DuplicateEmailException;
import com.bidstream.domain.auth.InvalidCredentialsException;
import com.bidstream.domain.auth.TokenReuseException;
import com.bidstream.domain.bid.BidConflictException;
import com.bidstream.domain.bid.BidNotLiveException;
import com.bidstream.domain.bid.BidSelfException;
import com.bidstream.domain.bid.BidTooLowException;
import com.bidstream.domain.bid.LockTimeoutException;
import com.bidstream.domain.lot.FileTooLargeException;
import com.bidstream.domain.lot.ForbiddenLotAccessException;
import com.bidstream.domain.lot.ImageLimitReachedException;
import com.bidstream.domain.lot.ImageRequiredException;
import com.bidstream.domain.lot.InvalidImageOrderException;
import com.bidstream.domain.lot.InvalidTransitionException;
import com.bidstream.domain.lot.LotValidationException;
import com.bidstream.domain.lot.UnsupportedMediaTypeException;
import com.bidstream.domain.lot.UploadMismatchException;
import com.bidstream.domain.lot.UploadNotFoundException;
import com.bidstream.domain.ratelimit.RateLimitExceededException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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

  @ExceptionHandler(DuplicateEmailException.class)
  public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "conflict", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(AlreadySellerException.class)
  public ResponseEntity<ErrorResponse> handleAlreadySeller(AlreadySellerException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "already_seller", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.UNAUTHORIZED, "unauthorized", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(TokenReuseException.class)
  public ResponseEntity<ErrorResponse> handleTokenReuse(TokenReuseException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.UNAUTHORIZED, "token_reuse_detected", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(ForbiddenLotAccessException.class)
  public ResponseEntity<ErrorResponse> handleForbiddenLot(ForbiddenLotAccessException ex) {
    String traceId = traceId();
    return response(HttpStatus.FORBIDDEN, "forbidden", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(InvalidTransitionException.class)
  public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidTransitionException ex) {
    String traceId = traceId();
    Map<String, String> details = new LinkedHashMap<>();
    details.put("from", ex.from().name());
    details.put("event", ex.event().name());
    return response(
        HttpStatus.CONFLICT, "invalid_transition", ex.getMessage(), details, traceId, ex);
  }

  @ExceptionHandler(LotValidationException.class)
  public ResponseEntity<ErrorResponse> handleLotValidation(LotValidationException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.BAD_REQUEST, "validation_error", ex.getMessage(), ex.details(), traceId, ex);
  }

  @ExceptionHandler(UnsupportedMediaTypeException.class)
  public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
      UnsupportedMediaTypeException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.BAD_REQUEST, "unsupported_media_type", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(BidTooLowException.class)
  public ResponseEntity<ErrorResponse> handleBidTooLow(BidTooLowException ex) {
    String traceId = traceId();
    Map<String, String> details = new LinkedHashMap<>(ex.details());
    return response(HttpStatus.CONFLICT, "bid_too_low", ex.getMessage(), details, traceId, ex);
  }

  @ExceptionHandler(BidNotLiveException.class)
  public ResponseEntity<ErrorResponse> handleBidNotLive(BidNotLiveException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "bid_not_live", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(BidSelfException.class)
  public ResponseEntity<ErrorResponse> handleBidSelf(BidSelfException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "bid_self", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(BidConflictException.class)
  public ResponseEntity<ErrorResponse> handleBidConflict(BidConflictException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "bid_conflict", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(LockTimeoutException.class)
  public ResponseEntity<ErrorResponse> handleLockTimeout(LockTimeoutException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.SERVICE_UNAVAILABLE, "lock_timeout", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(FileTooLargeException.class)
  public ResponseEntity<ErrorResponse> handleFileTooLarge(FileTooLargeException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.BAD_REQUEST, "file_too_large", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(ImageLimitReachedException.class)
  public ResponseEntity<ErrorResponse> handleImageLimit(ImageLimitReachedException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.CONFLICT, "image_limit_reached", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(RateLimitExceededException.class)
  public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex) {
    String traceId = traceId();
    long retrySeconds = Math.max(1, ex.retryAfter().getSeconds());
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-After", String.valueOf(retrySeconds))
        .body(new ErrorResponse(new ErrorBody("rate_limited", ex.getMessage(), Map.of()), traceId));
  }

  @ExceptionHandler(UploadNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleUploadNotFound(UploadNotFoundException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.CONFLICT, "upload_not_found", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(UploadMismatchException.class)
  public ResponseEntity<ErrorResponse> handleUploadMismatch(UploadMismatchException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.BAD_REQUEST, "upload_mismatch", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(InvalidImageOrderException.class)
  public ResponseEntity<ErrorResponse> handleInvalidImageOrder(InvalidImageOrderException ex) {
    String traceId = traceId();
    return response(
        HttpStatus.BAD_REQUEST, "validation_error", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(ImageRequiredException.class)
  public ResponseEntity<ErrorResponse> handleImageRequired(ImageRequiredException ex) {
    String traceId = traceId();
    return response(HttpStatus.CONFLICT, "image_required", ex.getMessage(), Map.of(), traceId, ex);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    String traceId = traceId();
    return response(HttpStatus.FORBIDDEN, "forbidden", "Access denied", Map.of(), traceId, ex);
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
