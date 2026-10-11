package com.tenantsphere.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tenantsphere.common.Slugs;

import jakarta.mail.internet.InternetAddress;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Console;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

/**
 * Operator-only CLI: requires direct database credentials and exposes no HTTP bootstrap endpoint.
 */
public final class CreateAdminCommand {
    private CreateAdminCommand() {}

    public static void main(String[] args) {
        try {
            if (args.length != 2 && args.length != 4) {
                throw new IllegalArgumentException(
                        "Usage: CreateAdminCommand <email> <username> [<first name> <last name>]");
            }
            var dataSource = new DriverManagerDataSource();
            dataSource.setUrl(
                    "jdbc:postgresql://"
                            + env("POSTGRES_HOST", "localhost")
                            + ":"
                            + env("POSTGRES_PORT", "5432")
                            + "/"
                            + env("POSTGRES_DB", "tenantsphere"));
            dataSource.setUsername(requiredEnv("POSTGRES_USER"));
            dataSource.setPassword(requiredEnv("POSTGRES_PASSWORD"));
            String password = readPassword();
            create(
                    dataSource,
                    args[0],
                    args[1],
                    args.length == 4 ? args[2] : "TenantSphere",
                    args.length == 4 ? args[3] : "Administrator",
                    password);
            System.out.println(
                    "Administrator created. Sign in at /admin/login using the supplied email.");
        } catch (DuplicateKeyException e) {
            System.err.println(
                    "An account with that email or username already exists. No account was"
                        + " changed.");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (DataAccessException e) {
            System.err.println(
                    "Could not create administrator. Check database access and apply the"
                        + " application migrations first.");
            System.exit(1);
        }
    }

    static long create(
            DataSource dataSource,
            String email,
            String username,
            String firstName,
            String lastName,
            String password) {
        email = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        username = username == null ? "" : username.trim();
        try {
            new InternetAddress(email, true).validate();
        } catch (Exception e) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
        if (email.length() > 254 || !email.contains("@"))
            throw new IllegalArgumentException("Enter a valid email address.");
        if (!username.matches("[a-zA-Z0-9_]{1,60}"))
            throw new IllegalArgumentException(
                    "Username must be 1–60 letters, digits or underscores.");
        if (firstName == null
                || firstName.isBlank()
                || firstName.length() > 60
                || lastName == null
                || lastName.isBlank()
                || lastName.length() > 60)
            throw new IllegalArgumentException("First and last names must be 1–60 characters.");
        if (password == null || !password.matches("(?s)^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,128}$"))
            throw new IllegalArgumentException(
                    "Admin password must be 12–128 characters with an uppercase letter, a lowercase"
                        + " letter and a digit.");

        String hash = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(password);
        String normalizedEmail = email, normalizedUsername = username;
        var db = new JdbcTemplate(dataSource);
        return new TransactionTemplate(new DataSourceTransactionManager(dataSource))
                .execute(
                        status -> {
                            db.execute("SELECT pg_advisory_xact_lock(170628,4)");
                            UUID userId = UUID.randomUUID(), profileId = UUID.randomUUID();
                            long user =
                                    db.queryForObject(
                                            "INSERT INTO"
                                                + " users(id,email,username,first_name,last_name,password,is_active,is_staff,is_superuser)"
                                                + " VALUES(?,?,?,?,?,?,TRUE,TRUE,TRUE) RETURNING"
                                                + " pkid",
                                            Long.class,
                                            userId,
                                            normalizedEmail,
                                            normalizedUsername,
                                            firstName.trim(),
                                            lastName.trim(),
                                            hash);
                            String slug =
                                    Slugs.of(normalizedUsername, "profile")
                                            + "-"
                                            + profileId.toString().substring(0, 8);
                            long profile =
                                    db.queryForObject(
                                            "INSERT INTO profiles(id,user_pkid,slug) VALUES(?,?,?)"
                                                + " RETURNING pkid",
                                            Long.class,
                                            profileId,
                                            user,
                                            slug);
                            record(
                                    db,
                                    userId,
                                    normalizedEmail,
                                    "users",
                                    user,
                                    normalizedUsername,
                                    Map.of(
                                            "email",
                                            normalizedEmail,
                                            "username",
                                            normalizedUsername,
                                            "first_name",
                                            firstName.trim(),
                                            "last_name",
                                            lastName.trim(),
                                            "is_active",
                                            true,
                                            "is_staff",
                                            true,
                                            "is_superuser",
                                            true,
                                            "password_changed",
                                            true,
                                            "setup_method",
                                            "local command"));
                            record(
                                    db,
                                    userId,
                                    normalizedEmail,
                                    "profiles",
                                    profile,
                                    slug,
                                    Map.of(
                                            "user_pkid",
                                            user,
                                            "slug",
                                            slug,
                                            "setup_method",
                                            "local command"));
                            return user;
                        });
    }

    private static void record(
            JdbcTemplate db,
            UUID actor,
            String email,
            String resource,
            long id,
            String label,
            Map<String, Object> values) {
        try {
            String json = new ObjectMapper().writeValueAsString(values);
            db.update(
                    "INSERT INTO"
                        + " admin_audit_log(actor_id,actor_label,resource,object_pkid,object_label,action,after_data)"
                        + " VALUES(?,?,?,?,?,'create',?::jsonb)",
                    actor,
                    email,
                    resource,
                    id,
                    label,
                    json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not record administrator setup.", e);
        }
    }

    private static String readPassword() {
        String configured = System.getenv("TENANTSPHERE_ADMIN_PASSWORD");
        if (configured != null && !configured.isBlank()) return configured;
        Console console = System.console();
        if (console == null)
            throw new IllegalArgumentException(
                    "Run in a terminal to enter a password, or provide TENANTSPHERE_ADMIN_PASSWORD"
                        + " through a secret manager.");
        char[] password = console.readPassword("Admin password: ");
        char[] confirmation = console.readPassword("Confirm password: ");
        try {
            if (password == null || confirmation == null || !Arrays.equals(password, confirmation))
                throw new IllegalArgumentException("Passwords do not match.");
            return new String(password);
        } finally {
            if (password != null) Arrays.fill(password, '\0');
            if (confirmation != null) Arrays.fill(confirmation, '\0');
        }
    }

    private static String env(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("Set " + name + " before running this command.");
        return value;
    }
}
