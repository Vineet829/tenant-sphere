package com.tenantsphere.admin;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/** Presentation helpers only; authorization and record operations remain in their services. */
final class AdminPresentation {
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd MMM yyyy · HH:mm 'UTC'", Locale.ENGLISH)
                    .withZone(ZoneOffset.UTC);

    private AdminPresentation() {}

    static Icon icon(VaadinIcon symbol) {
        Icon icon = symbol.create();
        icon.addClassName("admin-icon");
        icon.getElement().setAttribute("aria-hidden", "true");
        return icon;
    }

    static VaadinIcon symbol(AdminResource resource) {
        return switch (resource) {
            case USERS -> VaadinIcon.USERS;
            case PROFILES -> VaadinIcon.USER;
            case APARTMENTS -> VaadinIcon.BUILDING;
            case ISSUES -> VaadinIcon.TOOLS;
            case POSTS -> VaadinIcon.COMMENTS;
            case RATINGS -> VaadinIcon.STAR;
            case REPORTS -> VaadinIcon.FLAG;
            case CONTENT_VIEWS -> VaadinIcon.EYE;
            case TAGS -> VaadinIcon.TAGS;
            case GROUPS -> VaadinIcon.SHIELD;
        };
    }

    static String description(AdminResource resource) {
        return switch (resource) {
            case USERS -> "Accounts, contact details and access.";
            case PROFILES -> "Resident and technician information.";
            case APARTMENTS -> "Buildings, homes and tenant assignments.";
            case ISSUES -> "Maintenance requests and assignments.";
            case POSTS -> "Conversations across your community.";
            case RATINGS -> "Resident feedback on technicians.";
            case REPORTS -> "Resident concerns and account reports.";
            case CONTENT_VIEWS -> "Recorded visits to posts and issues.";
            case TAGS -> "Topics that organize community posts.";
            case GROUPS -> "Staff groups and model permissions.";
        };
    }

    static Div brand() {
        Span mark = new Span(icon(VaadinIcon.BUILDING));
        mark.addClassName("admin-brand-mark");
        Span name = new Span("TenantSphere");
        name.addClassName("admin-brand-name");
        Span caption = new Span("Administration");
        caption.addClassName("admin-brand-caption");
        Div wordmark = new Div(name, caption);
        wordmark.addClassName("admin-wordmark");
        Div brand = new Div(mark, wordmark);
        brand.addClassName("admin-brand");
        return brand;
    }

    static Span eyebrow(String text) {
        Span label = new Span(text);
        label.addClassName("admin-eyebrow");
        return label;
    }

    static String number(long number) {
        return NumberFormat.getIntegerInstance(Locale.ENGLISH).format(number);
    }

    static String timestamp(Object value) {
        if (value == null) return "—";
        Instant instant;
        if (value instanceof Date date) instant = date.toInstant();
        else if (value instanceof Instant time) instant = time;
        else if (value instanceof OffsetDateTime time) instant = time.toInstant();
        else if (value instanceof ZonedDateTime time) instant = time.toInstant();
        else return value.toString();
        return DATE.format(instant);
    }
}
