package com.tenantsphere.auth;

public class InvalidActivationTokenException extends RuntimeException {

    public InvalidActivationTokenException() {
        super("Invalid or expired activation token");
    }
}
