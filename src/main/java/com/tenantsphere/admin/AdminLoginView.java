package com.tenantsphere.admin;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginI18n;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("admin/login")
@PageTitle("Log in | Tenant Sphere Admin")
@AnonymousAllowed
public class AdminLoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public AdminLoginView() {
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
        loginForm.setI18n(i18n);
        loginForm.setForgotPasswordButtonVisible(false);
        loginForm.setAction("admin/login");

        add(title, subtitle, loginForm);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean failed = event.getLocation().getQueryParameters()
                .getParameters().containsKey("error");
        loginForm.setError(failed);
    }
}
