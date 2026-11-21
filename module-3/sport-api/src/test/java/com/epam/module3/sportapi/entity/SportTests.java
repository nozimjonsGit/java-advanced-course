package com.epam.module3.sportapi.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.epam.module3.sportapi.exception.InvalidSportNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class SportTests {

    @Test
    void stripsWhitespaceAroundName() {
        assertThat(Sport.named("  Golf \t").name()).isEqualTo("Golf");
    }

    @Test
    void hasNoIdUntilSaved() {
        assertThat(Sport.named("Golf").id()).isNull();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void rejectsBlankName(String name) {
        assertThatThrownBy(() -> Sport.named(name))
                .isInstanceOf(InvalidSportNameException.class)
                .hasMessage("Sport name must not be blank");
    }

    @Test
    void acceptsNameOfMaximumLength() {
        String name = "a".repeat(Sport.MAX_NAME_LENGTH);

        assertThat(Sport.named(name).name()).isEqualTo(name);
    }

    @Test
    void rejectsNameLongerThanColumn() {
        assertThatThrownBy(() -> Sport.named("a".repeat(Sport.MAX_NAME_LENGTH + 1)))
                .isInstanceOf(InvalidSportNameException.class)
                .hasMessage("Sport name must be at most 100 characters long, but has 101");
    }
}
