package com.epam.module3.sportapi.entity;

import com.epam.module3.sportapi.exception.InvalidSportNameException;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A sport. The database generates the ID, so a sport that is not saved yet has a {@code null} ID.
 *
 * @param id   ID of the sport
 * @param name name of the sport, without leading or trailing whitespace
 */
@Table("sports")
public record Sport(@Id Integer id, String name) {

    /** Length of the {@code name} column. */
    public static final int MAX_NAME_LENGTH = 100;

    /**
     * @throws InvalidSportNameException if the name is blank or longer than {@value #MAX_NAME_LENGTH} characters
     */
    public Sport {
        if (name == null || name.isBlank()) {
            throw new InvalidSportNameException("Sport name must not be blank");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidSportNameException(
                    "Sport name must be at most %d characters long, but has %d".formatted(MAX_NAME_LENGTH, name.length()));
        }
    }

    /** Creates a sport that is not saved yet. */
    public static Sport named(String name) {
        return new Sport(null, name);
    }
}
