package com.tenantsphere.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseWriter {

    private final HandlerLabelResolver labelResolver;
    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(HandlerLabelResolver labelResolver, ObjectMapper objectMapper) {
        this.labelResolver = labelResolver;
        this.objectMapper = objectMapper;
    }

    public void write(HttpServletRequest request, HttpServletResponse response, int status, String detail)
            throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> detailBody = Map.of("detail", detail);
        String label = labelResolver.resolve(request);

        Object payload;
        if (label == null) {
            payload = detailBody;
        } else {
            Map<String, Object> envelope = new LinkedHashMap<>();
            envelope.put("status_code", status);
            envelope.put(label, detailBody);
            payload = envelope;
        }

        objectMapper.writeValue(response.getOutputStream(), payload);
    }
}
