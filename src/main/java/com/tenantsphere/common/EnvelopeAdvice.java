package com.tenantsphere.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class EnvelopeAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return resolveLabel(returnType) != null;
    }

    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> converterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        String label = resolveLabel(returnType);
        if (label == null || carriesErrorsKey(body)) {
            return body;
        }

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("status_code", statusOf(response));
        envelope.put(label, body);
        return envelope;
    }

    private boolean carriesErrorsKey(Object body) {
        return body instanceof Map<?, ?> map && map.containsKey("errors");
    }

    private String resolveLabel(MethodParameter returnType) {
        ObjectLabel onMethod = returnType.getMethodAnnotation(ObjectLabel.class);
        if (onMethod != null) {
            return onMethod.value();
        }
        Class<?> declaring = returnType.getContainingClass();
        ObjectLabel onClass = declaring.getAnnotation(ObjectLabel.class);
        return onClass != null ? onClass.value() : null;
    }

    private int statusOf(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            return servletResponse.getServletResponse().getStatus();
        }
        return 200;
    }
}
