package com.tenantsphere.auth;

public class InvalidCredentialsException extends RuntimeException {

    public static final String MESSAGE = "No active account found with the given credentials";

    public InvalidCredentialsException() {
        super(MESSAGE);
    }
}
