package com.epam.module3.sportapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.setup.SportSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "app.sport-import.enabled=false")
@Import(TestcontainersConfiguration.class)
class SportApiApplicationTests {

    @Autowired
    private ApplicationContext context;

    @Test
    void doesNotImportSportsWhenImportIsDisabled() {
        assertThat(context.getBeansOfType(SportSetup.class)).isEmpty();
    }
}
