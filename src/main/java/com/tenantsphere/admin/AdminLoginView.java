package com.tenantsphere.admin;

import com.tenantsphere.auth.AuthService;
import com.tenantsphere.auth.CookieService;
import com.tenantsphere.auth.JwtService;
import com.tenantsphere.user.User;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import java.util.UUID;

@Route("admin/login")
@PageTitle("Log in | Tenant Sphere Admin")
@AnonymousAllowed
public class AdminLoginView extends VerticalLayout {

    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    public AdminLoginView(
            AuthService authService, JwtService jwtService, CookieService cookieService) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;

        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setAlignItems(Alignment.CENTER);
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        H1 title = new H1("Tenant Sphere administration");
        title.getStyle().set("font-size", "var(--lumo-font-size-xxl)");

        Paragraph subtitle = new Paragraph("Staff accounts only.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        subtitle.getStyle().set("margin-top", "0");

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("Log in");
        i18n.getForm().setUsername("Email address");
        i18n.getErrorMessage().setTitle("Incorrect email or password");
        i18n.getErrorMessage().setMessage(
                "Check that you have entered the correct email address and password, "
                        + "and that your account has staff access.");

        LoginForm loginForm = new LoginForm();
        loginForm.setI18n(i18n);
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addLoginListener(event -> handleLogin(event, loginForm));

        add(title, subtitle, loginForm);
    }

    private void handleLogin(AbstractLogin.LoginEvent event, LoginForm loginForm) {
        try {
            User user = authService.authenticate(event.getUsername(), event.getPassword());
            if (!user.isStaff() && !user.isSuperuser()) {
                loginForm.setError(true);
                return;
            }
            UUID userId = user.getId();
            cookieService.writeAuthCookies(
                    VaadinServletResponse.getCurrent().getHttpServletResponse(),
                    jwtService.issueAccessToken(userId),
                    jwtService.issueRefreshToken(userId));
            getUI().ifPresent(ui -> ui.getPage().setLocation("/admin"));
        } catch (RuntimeException exception) {
            loginForm.setError(true);
        }
    }
}
