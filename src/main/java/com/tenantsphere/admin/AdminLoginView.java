package com.tenantsphere.admin;

import com.tenantsphere.auth.AuthService;
import com.tenantsphere.auth.CookieService;
import com.tenantsphere.auth.JwtService;
import com.tenantsphere.config.AppProperties;
import com.tenantsphere.user.User;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.AbstractLogin;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.UUID;

@Route("login")
@PageTitle("Log in | TenantSphere Administration")
@AnonymousAllowed
public class AdminLoginView extends VerticalLayout {
    private final AuthService authService;
    private final JwtService jwtService;
    private final CookieService cookieService;

    public AdminLoginView(
            AuthService authService,
            JwtService jwtService,
            CookieService cookieService,
            AppProperties properties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        addClassName("admin-login");
        setPadding(false);
        setSpacing(false);

        Div intro = new Div(AdminPresentation.brand());
        intro.addClassName("admin-login-intro");
        Div introduction =
                new Div(
                        AdminPresentation.eyebrow("Community operations"),
                        new H1("Community administration"),
                        new Paragraph("Manage people, homes and your community’s everyday needs."));
        introduction.addClassName("admin-login-introduction");
        intro.add(
                introduction,
                feature(
                        VaadinIcon.USERS,
                        "Residents & homes",
                        "Keep records and assignments organized."),
                feature(
                        VaadinIcon.TOOLS,
                        "Maintenance & reports",
                        "Follow up on community issues."),
                feature(
                        VaadinIcon.CLOCK,
                        "Change history",
                        "See what changed and who made the update."));
        Anchor back = new Anchor(properties.site().domain(), "Back to the community");
        back.addComponentAsFirst(AdminPresentation.icon(VaadinIcon.ARROW_LEFT));
        back.addClassName("admin-text-link");
        intro.add(back);

        LoginI18n i18n = LoginI18n.createDefault();
        i18n.getForm().setTitle("");
        i18n.getForm().setUsername("Email address");
        i18n.getErrorMessage().setTitle("Incorrect email or password");
        i18n.getErrorMessage()
                .setMessage(
                        "Check that you have entered the correct email address and password, "
                                + "and that your account has staff access.");
        LoginForm loginForm = new LoginForm();
        loginForm.setI18n(i18n);
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.addLoginListener(event -> handleLogin(event, loginForm));

        Span accessLabel =
                new Span(AdminPresentation.icon(VaadinIcon.LOCK), new Span("Staff access"));
        accessLabel.addClassName("admin-login-access");
        Div form =
                new Div(
                        accessLabel,
                        new H2("Sign in"),
                        new Paragraph("Use your administrator or staff account."),
                        loginForm);
        form.addClassName("admin-login-form");
        Paragraph help = new Paragraph("Need access? Contact your community administrator.");
        help.addClassName("admin-login-help");
        form.add(help);
        Div card = new Div(intro, form);
        card.addClassName("admin-login-card");
        add(card);
    }

    private static Div feature(VaadinIcon icon, String title, String description) {
        Span symbol = new Span(AdminPresentation.icon(icon));
        symbol.addClassName("admin-login-feature-icon");
        Div copy = new Div(new H3(title), new Paragraph(description));
        Div feature = new Div(symbol, copy);
        feature.addClassName("admin-login-feature");
        return feature;
    }

    private void handleLogin(AbstractLogin.LoginEvent event, LoginForm loginForm) {
        try {
            User user = authService.authenticate(event.getUsername(), event.getPassword());
            if (!user.isStaff() && !user.isSuperuser()) {
                loginForm.setError(true);
                return;
            }
            UUID userId = user.getId();
            UUID session = UUID.randomUUID();
            var request = VaadinServletRequest.getCurrent().getHttpServletRequest();
            if (request.getSession(false) != null) request.changeSessionId();
            cookieService.writeAuthCookies(
                    VaadinServletResponse.getCurrent().getHttpServletResponse(),
                    jwtService.issueAccessToken(userId, session),
                    jwtService.issueRefreshToken(userId, session));
            getUI().ifPresent(ui -> ui.getPage().setLocation("/admin/"));
        } catch (RuntimeException exception) {
            loginForm.setError(true);
        }
    }
}
