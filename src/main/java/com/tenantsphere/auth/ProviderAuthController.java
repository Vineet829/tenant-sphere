package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.AuthorizationUrlResponse;
import com.tenantsphere.auth.dto.MessageResponse;
import com.tenantsphere.user.User;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/o")
public class ProviderAuthController {

    private final GoogleOAuthService googleOAuthService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    public ProviderAuthController(
            GoogleOAuthService googleOAuthService,
            JwtService jwtService,
            CookieService cookieService) {
        this.googleOAuthService = googleOAuthService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
    }

    @GetMapping("/{provider}/")
    public AuthorizationUrlResponse authorizationUrl(
            @PathVariable String provider,
            @RequestParam("redirect_uri") String redirectUri, HttpServletRequest request) {
        return new AuthorizationUrlResponse(
                googleOAuthService.authorizationUrl(provider, redirectUri, request));
    }

    @PostMapping("/{provider}/")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse login(
            @PathVariable String provider,
            @RequestParam("state") String state,
            @RequestParam("code") String code,
            HttpServletRequest request, HttpServletResponse response) {
        User user = googleOAuthService.login(provider, state, code, request);
        if (request.getSession(false) != null) request.changeSessionId();
        UUID userId = user.getId();
        cookieService.writeAuthCookies(
                response, jwtService.issueAccessToken(userId), jwtService.issueRefreshToken(userId));
        return new MessageResponse("You are logged in Successful.");
    }
}
