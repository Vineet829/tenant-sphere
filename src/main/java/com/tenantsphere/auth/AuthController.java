package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.ActivationRequest;
import com.tenantsphere.auth.dto.CurrentUserResponse;
import com.tenantsphere.auth.dto.LoginRequest;
import com.tenantsphere.auth.dto.MessageResponse;
import com.tenantsphere.auth.dto.RegisterRequest;
import com.tenantsphere.auth.dto.RegisterResponse;
import com.tenantsphere.auth.dto.ResendActivationRequest;
import com.tenantsphere.config.AppProperties;
import com.tenantsphere.profile.Profile;
import com.tenantsphere.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;
    private final RegistrationService registrationService;
    private final String refreshCookieName;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            CookieService cookieService,
            RegistrationService registrationService,
            AppProperties properties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.registrationService = registrationService;
        this.refreshCookieName = properties.cookie().refreshName();
    }

    @PostMapping("/login/")
    public MessageResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {

        User user = authService.authenticate(request.email(), request.password());
        issueCookies(user.getId(), response);
        return new MessageResponse("Login Successful.");
    }

    @PostMapping("/refresh/")
    public MessageResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = readRefreshCookie(request).orElseThrow(InvalidCredentialsException::new);
        UUID userId;
        try {
            userId = jwtService.extractUserId(token, TokenType.REFRESH);
        } catch (Exception e) {
            throw new InvalidCredentialsException();
        }
        User user = authService.requireActiveUser(userId);
        issueCookies(user.getId(), response);
        return new MessageResponse("Access tokens refreshed successfully");
    }

    @PostMapping("/users/")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = registrationService.register(request);
        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail());
    }

    @PostMapping("/users/activation/")
    public ResponseEntity<Void> activate(@Valid @RequestBody ActivationRequest request) {
        if (!registrationService.activate(request.uid(), request.token())) {
            throw new InvalidActivationTokenException();
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/users/resend_activation/")
    public ResponseEntity<Void> resendActivation(
            @Valid @RequestBody ResendActivationRequest request) {
        registrationService.resendActivation(request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/logout/")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        cookieService.clearAuthCookies(response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/users/me/")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AppUserDetails principal) {
        User user = principal.getUser();
        Profile profile = authService.requireProfile(user);
        return CurrentUserResponse.from(user, profile);
    }

    private void issueCookies(UUID userId, HttpServletResponse response) {
        cookieService.writeAuthCookies(
                response, jwtService.issueAccessToken(userId), jwtService.issueRefreshToken(userId));
    }

    private Optional<String> readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> refreshCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
