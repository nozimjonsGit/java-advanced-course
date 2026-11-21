package com.epam.module3.sportapi.exception;

/**
 * Thrown when a sport name is blank or longer than the database column allows.
 */
public class InvalidSportNameException extends IllegalArgumentException {

    public InvalidSportNameException(String message) {
        super(message);
    }
}
