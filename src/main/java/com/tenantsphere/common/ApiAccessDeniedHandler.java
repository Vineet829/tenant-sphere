package com.tenantsphere.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter writer;

    public ApiAccessDeniedHandler(ErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException e)
            throws IOException {
        writer.write(request, response, 403,
                e instanceof org.springframework.security.web.csrf.CsrfException
                        ? "Your security token is missing or expired. Please reload and try again."
                        : "You do not have permission to perform this action.");
    }
}
