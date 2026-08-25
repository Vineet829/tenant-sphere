package com.tenantsphere.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final HandlerLabelResolver labelResolver;

    public ApiExceptionHandler(HandlerLabelResolver labelResolver) {
        this.labelResolver = labelResolver;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidation(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        Map<String, List<String>> fields = new LinkedHashMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.computeIfAbsent(error.getField(), field -> new ArrayList<>())
                    .add(error.getDefaultMessage());
        }
        return respond(request, HttpStatus.BAD_REQUEST, fields);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Object> handleResponseStatus(
            ResponseStatusException e, HttpServletRequest request) {

        HttpStatusCode status = e.getStatusCode();
        String detail = e.getReason() == null ? HttpStatus.valueOf(status.value()).getReasonPhrase() : e.getReason();
        return respond(request, status, Map.of("detail", detail));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {

        return respond(
                withSupportedMethod(request, e),
                HttpStatus.METHOD_NOT_ALLOWED,
                Map.of("detail", "Method \"" + e.getMethod() + "\" not allowed."));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResource(
            NoResourceFoundException e, HttpServletRequest request) {

        return respond(request, HttpStatus.NOT_FOUND, Map.of("detail", "Not found."));
    }

    private HttpServletRequest withSupportedMethod(
            HttpServletRequest request, HttpRequestMethodNotSupportedException e) {

        Set<HttpMethod> supported = e.getSupportedHttpMethods();
        if (supported == null || supported.isEmpty()) {
            return request;
        }
        String method = supported.iterator().next().name();
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getMethod() {
                return method;
            }
        };
    }

    private ResponseEntity<Object> respond(
            HttpServletRequest request, HttpStatusCode status, Object body) {

        String label = labelResolver.resolve(request);
        if (label == null) {
            return ResponseEntity.status(status).body(body);
        }
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status_code", status.value());
        envelope.put(label, body);
        return ResponseEntity.status(status).body(envelope);
    }
}
