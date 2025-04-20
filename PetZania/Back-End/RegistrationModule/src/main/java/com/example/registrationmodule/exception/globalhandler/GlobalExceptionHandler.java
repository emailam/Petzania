package com.example.registrationmodule.exception.globalhandler;

import com.example.registrationmodule.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return errors;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ex.getMessage());
    }


    @ExceptionHandler(RefreshTokenNotValid.class)
    public ResponseEntity<String> handleRefreshTokenNotValid(RefreshTokenNotValid ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExists.class)
    public ResponseEntity<String> handleEmailAlreadyExists(EmailAlreadyExists ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(EmailNotSent.class)
    public ResponseEntity<String> handleEmailNotSend(EmailNotSent ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(ExpiredOTP.class)
    public ResponseEntity<String> handleExpiredOTP(ExpiredOTP ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidOTPCode.class)
    public ResponseEntity<String> handleInvalidOTPCode(InvalidOTPCode ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidToken.class)
    public ResponseEntity<String> handleInvalidToken(InvalidToken ex){
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }
    @ExceptionHandler(InvalidUserCredentials.class)
    public ResponseEntity<String> handleInvalidUserCredentials(InvalidUserCredentials ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyBlocked.class)
    public ResponseEntity<String> handleUserAlreadyBlocked(UserAlreadyBlocked ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyLoggedOut.class)
    public ResponseEntity<String> handleUserAlreadyLoggedOut(UserAlreadyLoggedOut ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyUnblocked.class)
    public ResponseEntity<String> handleUserAlreadyUnblocked(UserAlreadyUnblocked ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UserAlreadyVerified.class)
    public ResponseEntity<String> handleUserAlreadyVerified(UserAlreadyVerified ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UserDoesNotExist.class)
    public ResponseEntity<String> handleUserDoesNotExist(UserDoesNotExist ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(UserIsBlocked.class)
    public ResponseEntity<String> handleUserIsBlocked(UserIsBlocked ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExists.class)
    public ResponseEntity<String> handleUsernameAlreadyExists(UsernameAlreadyExists ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(UserNotVerified.class)
    public ResponseEntity<String> handleUserNotVerified(UserNotVerified ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }


}
