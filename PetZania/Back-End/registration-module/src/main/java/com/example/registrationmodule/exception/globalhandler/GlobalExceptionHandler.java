package com.example.registrationmodule.exception.globalhandler;


import com.example.registrationmodule.exception.admin.AdminNotFound;
import com.example.registrationmodule.exception.authenticationAndVerificattion.*;
import com.example.registrationmodule.exception.media.InvalidMediaFile;
import com.example.registrationmodule.exception.media.MediaNotFound;
import com.example.registrationmodule.exception.pet.PetNotFound;
import com.example.registrationmodule.exception.rateLimiting.*;
import com.example.registrationmodule.exception.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String STATUS_STRING = "status";
    private static final String ERROR_STRING = "error";
    private static final String MESSAGE_STRING = "message";

    private ResponseEntity<Map<String, Object>> buildErrorResponse(Exception ex, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(STATUS_STRING, status.value());
        errorResponse.put(ERROR_STRING, status.getReasonPhrase());
        errorResponse.put(MESSAGE_STRING, ex.getMessage());
        return ResponseEntity.status(status).body(errorResponse);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put(STATUS_STRING, HttpStatus.NOT_ACCEPTABLE.value());
        errorResponse.put(ERROR_STRING, HttpStatus.NOT_ACCEPTABLE.getReasonPhrase());

        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> {
                    Map<String, String> errorDetails = new HashMap<>();
                    errorDetails.put("field", fieldError.getField());
                    errorDetails.put(MESSAGE_STRING, fieldError.getDefaultMessage());
                    return errorDetails;
                })
                .toList();
        errorResponse.put(MESSAGE_STRING, errors);

        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RefreshTokenNotValid.class)
    public ResponseEntity<Map<String, Object>> handleRefreshTokenNotValid(RefreshTokenNotValid ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(EmailAlreadyExists.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(EmailAlreadyExists ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(EmailNotSent.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotSend(EmailNotSent ex) {
        return buildErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ExpiredOTP.class)
    public ResponseEntity<Map<String, Object>> handleExpiredOTP(ExpiredOTP ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidOTPCode.class)
    public ResponseEntity<Map<String, Object>> handleInvalidOTPCode(InvalidOTPCode ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidToken.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(InvalidToken ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(InvalidUserCredentials.class)
    public ResponseEntity<Map<String, Object>> handleInvalidUserCredentials(InvalidUserCredentials ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserAlreadyBlocked.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyBlocked(UserAlreadyBlocked ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyLoggedOut.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyLoggedOut(UserAlreadyLoggedOut ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyUnblocked.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyUnblocked(UserAlreadyUnblocked ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserAlreadyVerified.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyVerified(UserAlreadyVerified ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(UserIsBlocked.class)
    public ResponseEntity<Map<String, Object>> handleUserIsBlocked(UserIsBlocked ex) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(UsernameAlreadyExists.class)
    public ResponseEntity<Map<String, Object>> handleUsernameAlreadyExists(UsernameAlreadyExists ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UserNotVerified.class)
    public ResponseEntity<Map<String, Object>> handleUserNotVerified(UserNotVerified ex) {
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UserIdNull.class)
    public ResponseEntity<Map<String, Object>> handleUserIdNull(UserIdNull ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UserAccessDenied.class)
    public ResponseEntity<Map<String, Object>> handleUserAccessDenied(UserAccessDenied ex) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticatedUserNotFound.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticatedUserNotFound(AuthenticatedUserNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AdminNotFound.class)
    public ResponseEntity<Map<String, Object>> handleAdminNotFound(AdminNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PetNotFound.class)
    public ResponseEntity<Map<String, Object>> handlePetNotFound(PetNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(InvalidMediaFile.class)
    public ResponseEntity<Map<String, Object>> handleInvalidMediaFile(InvalidMediaFile ex) {
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MediaNotFound.class)
    public ResponseEntity<Map<String, Object>> handleMediaNotFound(MediaNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RateLimitExceeded.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceed(RateLimitExceeded ex) {
        return buildErrorResponse(ex, HttpStatus.TOO_MANY_REQUESTS);
    }
}
