package com.epam.module3.sportapi.setup;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.module3.sportapi.TestcontainersConfiguration;
import com.epam.module3.sportapi.client.RakutenFixtures;
import com.epam.module3.sportapi.client.RakutenItemSearchClient;
import com.epam.module3.sportapi.client.StubExchangeFunction;
import com.epam.module3.sportapi.repository.SportRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.convention.TestBean;
import reactor.core.publisher.Flux;

/**
 * Starts the application with a Rakuten stub that returns 45 genres, and checks in the log that
 * the import requests them 20 at a time.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ExtendWith(OutputCaptureExtension.class)
class SportSetupTests {

    private static final int GENRE_COUNT = 45;

    private static final Pattern SIGNAL = Pattern.compile(
            Pattern.quote(SportSetup.SIGNAL_LOG_CATEGORY) + "\\s*:\\s*(\\w+)\\(([^)]*)");

    @TestBean
    private RakutenItemSearchClient client;

    @Autowired
    private SportRepository repository;

    static RakutenItemSearchClient client() {
        List<String> names = IntStream.rangeClosed(1, GENRE_COUNT).mapToObj(i -> "Sport " + i).toList();
        String response = RakutenFixtures.responseWithChildGenres(names);
        return RakutenFixtures.client(StubExchangeFunction.json(HttpStatus.OK, response));
    }

    @Test
    void importsAllGenresTwentyAtATime(CapturedOutput output) {
        awaitLog(output, "Imported 45 new sports from Rakuten");

        assertThat(repository.count().block()).isEqualTo(GENRE_COUNT);
        List<String> expected = new ArrayList<>();
        expected.add("onSubscribe");
        expected.add("request(20)");
        expected.addAll(Collections.nCopies(20, "onNext"));
        expected.add("request(20)");
        expected.addAll(Collections.nCopies(20, "onNext"));
        expected.add("request(20)");
        expected.addAll(Collections.nCopies(5, "onNext"));
        expected.add("onComplete");
        assertThat(signals(output)).containsExactlyElementsOf(expected);
    }

    private static void awaitLog(CapturedOutput output, String text) {
        Flux.interval(Duration.ofMillis(50))
                .filter(tick -> output.getOut().contains(text))
                .blockFirst(Duration.ofSeconds(20));
    }

    /** The import's signals, in the order they were logged. Requests keep their amount. */
    private static List<String> signals(CapturedOutput output) {
        List<String> signals = new ArrayList<>();
        Matcher matcher = SIGNAL.matcher(output.getOut());
        while (matcher.find()) {
            String signal = matcher.group(1);
            signals.add(signal.equals("request") ? "request(" + matcher.group(2) + ")" : signal);
        }
        return signals;
    }
}
