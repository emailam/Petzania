package com.example.friends.and.chats.module.exception.globalhandler;

import com.example.friends.and.chats.module.exception.admin.AdminNotFound;
import com.example.friends.and.chats.module.exception.user.*;
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

    @ExceptionHandler(UserNotFound.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AdminNotFound.class)
    public ResponseEntity<Map<String, Object>> handleAdminNotFoundException(AdminNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthenticatedUserNotFound.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticatedUserNotFoundException(AuthenticatedUserNotFound ex) {
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidFriendRequest.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFriendRequestException(InvalidFriendRequest ex) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BlockingExist.class)
    public ResponseEntity<Map<String, Object>> handleBlockingExistException(BlockingExist ex) {
        return buildErrorResponse(ex, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(FriendRequestAlreadyExists.class)
    public ResponseEntity<Map<String, Object>> handleFriendRequestAlreadyExistsException(FriendRequestAlreadyExists ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(FollowingAlreadyExists.class)
    public ResponseEntity<Map<String, Object>> handleFollowingAlreadyExistsException(FollowingAlreadyExists ex) {
        return buildErrorResponse(ex, HttpStatus.CONFLICT);
    }
}
