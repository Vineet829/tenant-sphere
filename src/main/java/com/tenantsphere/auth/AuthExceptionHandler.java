package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.DetailResponse;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<DetailResponse> handleInvalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new DetailResponse(e.getMessage()));
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<Map<String, List<String>>> handleRegistration(RegistrationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(e.getField(), List.of(e.getMessage())));
    }

    @ExceptionHandler(InvalidActivationTokenException.class)
    public ResponseEntity<Map<String, List<String>>> handleActivation(
            InvalidActivationTokenException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("token", List.of(e.getMessage())));
    }
}
