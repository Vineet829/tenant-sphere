package com.tenantsphere.auth;

import com.tenantsphere.auth.dto.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class GuestController {
    private final GuestService guests;
    private final CookieService cookies;
    private final JwtService jwt;

    public GuestController(GuestService guests, CookieService cookies, JwtService jwt) {
        this.guests = guests;
        this.cookies = cookies;
        this.jwt = jwt;
    }

    @PostMapping("/guest/")
    public MessageResponse login(@AuthenticationPrincipal AppUserDetails principal,
            HttpServletRequest request, HttpServletResponse response) {
        // An already authenticated visitor continues their own account.
        var user = principal != null && principal.getUser().isGuest()
                ? principal.getUser() : guests.create(request.getRemoteAddr());
        if (request.getSession(false) != null) request.changeSessionId();
        UUID session = UUID.randomUUID();
        cookies.writeAuthCookies(
                response, jwt.issueAccessToken(user.getId(), session), jwt.issueRefreshToken(user.getId(), session));
        return new MessageResponse("Your temporary demo account is ready. Activity expires after 24 hours.");
    }
}
