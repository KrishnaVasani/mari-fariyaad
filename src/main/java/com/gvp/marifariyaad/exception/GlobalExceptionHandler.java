package com.gvp.marifariyaad.exception;

import com.gvp.marifariyaad.dto.ApiMessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiMessageResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(ApiMessageResponse.of(false, ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiMessageResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiMessageResponse.of(false, ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiMessageResponse> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiMessageResponse.of(false, ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiMessageResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiMessageResponse.of(false, "You are not authorized to access this resource."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(" "));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", message.isBlank() ? "Validation failed." : message);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiMessageResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiMessageResponse.of(false, "File too large. Photos must be under 10 MB and videos under 50 MB."));
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiMessageResponse> handleMultipart(MultipartException ex) {
        return ResponseEntity.badRequest().body(ApiMessageResponse.of(false, "Invalid file upload request."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiMessageResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiMessageResponse.of(false, "An unexpected error occurred. Please try again."));
    }
}
