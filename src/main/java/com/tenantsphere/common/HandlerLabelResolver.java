package com.tenantsphere.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Component
public class HandlerLabelResolver {

    private final ApplicationContext applicationContext;
    private volatile RequestMappingHandlerMapping handlerMapping;

    public HandlerLabelResolver(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public String resolve(HttpServletRequest request) {
        HandlerMethod handlerMethod = fromRequestAttribute(request);
        if (handlerMethod == null) {
            handlerMethod = fromHandlerMapping(request);
        }
        if (handlerMethod == null) {
            return null;
        }
        ObjectLabel onMethod = handlerMethod.getMethodAnnotation(ObjectLabel.class);
        if (onMethod != null) {
            return onMethod.value();
        }
        ObjectLabel onClass = handlerMethod.getBeanType().getAnnotation(ObjectLabel.class);
        return onClass != null ? onClass.value() : null;
    }

    private HandlerMethod fromRequestAttribute(HttpServletRequest request) {
        Object attribute = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        return attribute instanceof HandlerMethod method ? method : null;
    }

    private HandlerMethod fromHandlerMapping(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = mapping().getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod method) {
                return method;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private RequestMappingHandlerMapping mapping() {
        RequestMappingHandlerMapping local = handlerMapping;
        if (local == null) {
            synchronized (this) {
                local = handlerMapping;
                if (local == null) {
                    local = applicationContext.getBean(
                            "requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
                    handlerMapping = local;
                }
            }
        }
        return local;
    }
}
