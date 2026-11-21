package com.epam.module3.sportapi.exception;

/**
 * Thrown when a sport is created with the name of an existing sport. Names are compared ignoring case.
 */
public class SportAlreadyExistsException extends RuntimeException {

    public SportAlreadyExistsException(String name, Throwable cause) {
        super("Sport \"%s\" already exists".formatted(name), cause);
    }
}
