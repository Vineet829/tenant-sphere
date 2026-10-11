package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.ActivationRequest;
import com.tenantsphere.auth.dto.CurrentUserResponse;
import com.tenantsphere.auth.dto.LoginRequest;
import com.tenantsphere.auth.dto.MessageResponse;
import com.tenantsphere.auth.dto.PasswordResetConfirmRequest;
import com.tenantsphere.auth.dto.PasswordResetRequest;
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
    private final PasswordResetService passwordResetService;
    private final RequestLimiter limiter;
    private final TokenRevocations revocations;
    private final String refreshCookieName;
    private final String accessCookieName;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            CookieService cookieService,
            RegistrationService registrationService,
            PasswordResetService passwordResetService,
            RequestLimiter limiter,
            TokenRevocations revocations,
            AppProperties properties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
        this.limiter = limiter;
        this.revocations = revocations;
        this.refreshCookieName = properties.cookie().refreshName();
        this.accessCookieName = properties.cookie().accessName();
    }

    @GetMapping("/csrf/")
    public ResponseEntity<Void> csrf(org.springframework.security.web.csrf.CsrfToken token) {
        token.getToken(); // Materialize the deferred token and write its cookie.
        return ResponseEntity.noContent().header("Cache-Control", "no-store").build();
    }

    @PostMapping("/login/")
    public MessageResponse login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest,
            HttpServletResponse response) {
        limiter.consume(RequestLimit.SIGN_IN_ADDRESS, servletRequest.getRemoteAddr());
        limiter.consume(RequestLimit.SIGN_IN_EMAIL, request.email());
        User user = authService.authenticate(request.email(), request.password());
        if (servletRequest.getSession(false) != null) servletRequest.changeSessionId();
        issueCookies(user.getId(), UUID.randomUUID(), response);
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
        if (!jwtService.isCurrent(token, user) || revocations.isRevoked(token)) {
            throw new InvalidCredentialsException();
        }
        // The new tokens stay in the same sign-in, so signing out refuses the old ones with them.
        issueCookies(user.getId(), jwtService.sessionOf(token).orElseGet(UUID::randomUUID), response);
        return new MessageResponse("Access tokens refreshed successfully");
    }

    @PostMapping("/users/")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        limiter.consume(RequestLimit.SIGN_UP_ADDRESS, servletRequest.getRemoteAddr());
        User user = registrationService.register(request, servletRequest.getRemoteAddr());
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
            @Valid @RequestBody ResendActivationRequest request, HttpServletRequest servletRequest) {
        limitAccountEmail(servletRequest, request.email());
        registrationService.resendActivation(request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/users/reset_password/")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request, HttpServletRequest servletRequest) {
        limitAccountEmail(servletRequest, request.email());
        passwordResetService.requestReset(request.email());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/users/reset_password_confirm/")
    public ResponseEntity<Void> resetPasswordConfirm(
            @Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(
                request.uid(), request.token(), request.newPassword(), request.reNewPassword());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // Counted whether or not an account has the email, so a refusal says nothing about it.
    private void limitAccountEmail(HttpServletRequest request, String email) {
        limiter.consumeAccountEmail(request.getRemoteAddr(), email);
    }

    // The tokens are refused from now on, not only taken from this browser.
    @PostMapping("/logout/")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readCookie(request, accessCookieName).ifPresent(revocations::revoke);
        readCookie(request, refreshCookieName).ifPresent(revocations::revoke);
        cookieService.clearAuthCookies(response);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/users/me/")
    public CurrentUserResponse currentUser(@AuthenticationPrincipal AppUserDetails principal) {
        User user = principal.getUser();
        Profile profile = authService.requireProfile(user);
        return CurrentUserResponse.from(user, profile);
    }

    private void issueCookies(UUID userId, UUID session, HttpServletResponse response) {
        cookieService.writeAuthCookies(
                response, jwtService.issueAccessToken(userId, session), jwtService.issueRefreshToken(userId, session));
    }

    private Optional<String> readRefreshCookie(HttpServletRequest request) {
        return readCookie(request, refreshCookieName);
    }

    private static Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }
}
