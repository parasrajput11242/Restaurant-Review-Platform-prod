package com.paras.restaurant.controllers;

import com.paras.restaurant.domain.dtos.ErrorDto;
import com.paras.restaurant.exceptions.AddressNotFoundException;
import com.paras.restaurant.exceptions.BaseException;
import com.paras.restaurant.exceptions.ConflictException;
import com.paras.restaurant.exceptions.ForbiddenOperationException;
import com.paras.restaurant.exceptions.InvalidPhotoException;
import com.paras.restaurant.exceptions.NotFoundException;
import com.paras.restaurant.exceptions.ReviewNotAllowedException;
import com.paras.restaurant.exceptions.StorageException;
import com.paras.restaurant.exceptions.UpstreamServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/**
 * One place that turns every failure into the same {status, message, timestamp} JSON.
 * Extending ResponseEntityExceptionHandler also covers the framework's own errors
 * (unknown URL 404, wrong method 405, unreadable JSON 400, upload too large 413, ...)
 * with their correct status codes instead of a blanket 500.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ---- our own exceptions ----

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorDto> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReviewNotAllowedException.class)
    public ResponseEntity<ErrorDto> handleReviewNotAllowed(ReviewNotAllowedException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidPhotoException.class)
    public ResponseEntity<ErrorDto> handleInvalidPhoto(InvalidPhotoException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(AddressNotFoundException.class)
    public ResponseEntity<ErrorDto> handleAddressNotFound(AddressNotFoundException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorDto> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorDto> handleForbidden(Exception ex) {
        String message = ex instanceof ForbiddenOperationException
                ? ex.getMessage()
                : "You do not have permission to do this";
        return build(HttpStatus.FORBIDDEN, message);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorDto> handleOptimisticLock(OptimisticLockingFailureException ex) {
        log.warn("Concurrent modification detected", ex);
        return build(HttpStatus.CONFLICT, "Someone else changed this at the same moment. Please try again.");
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorDto> handleUpstream(UpstreamServiceException ex) {
        log.error("Upstream service failure", ex);
        return build(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorDto> handleStorage(StorageException ex) {
        log.error("Storage failure", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save or retrieve files at this time");
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorDto> handleBase(BaseException ex) {
        log.error("Unhandled application exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    /** Last resort. Details go to the log, never to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleUnexpected(Exception ex) {
        log.error("Unexpected exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // ---- framework exceptions ----

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(status).body(ErrorDto.of(status.value(), message));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Framework exception", ex);
        }
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        String message = statusCode.is5xxServerError()
                ? "An unexpected error occurred"
                : (resolved != null ? resolved.getReasonPhrase() : "Request failed");
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorDto.of(statusCode.value(), message));
    }

    private static ResponseEntity<ErrorDto> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorDto.of(status.value(), message));
    }
}
