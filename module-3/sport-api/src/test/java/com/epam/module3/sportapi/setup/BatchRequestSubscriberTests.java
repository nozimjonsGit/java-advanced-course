package com.epam.module3.sportapi.setup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.publisher.TestPublisher;

class BatchRequestSubscriberTests {

    private final List<Long> requests = new ArrayList<>();
    private final List<Integer> received = new ArrayList<>();
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final AtomicBoolean completed = new AtomicBoolean();

    @Test
    void requestsOneBatchAtATimeUntilUpstreamCompletes() {
        Flux.range(1, 45)
                .doOnRequest(requests::add)
                .subscribe(subscriber(20));

        assertThat(requests).containsExactly(20L, 20L, 20L);
        assertThat(received).hasSize(45).startsWith(1, 2, 3).endsWith(44, 45);
        assertThat(completed).isTrue();
    }

    @Test
    void requestsNextBatchOnlyAfterReceivingWholeBatch() {
        TestPublisher<Integer> publisher = TestPublisher.create();
        publisher.flux().doOnRequest(requests::add).subscribe(subscriber(20));
        assertThat(requests).containsExactly(20L);

        for (int i = 1; i <= 19; i++) {
            publisher.next(i);
        }
        assertThat(requests).containsExactly(20L);

        publisher.next(20);
        assertThat(requests).containsExactly(20L, 20L);
    }

    @Test
    void passesUpstreamErrorToHandler() {
        IllegalStateException failure = new IllegalStateException("upstream failed");

        Flux.just(1, 2).concatWith(Flux.error(failure)).subscribe(subscriber(20));

        assertThat(received).containsExactly(1, 2);
        assertThat(error).hasValue(failure);
        assertThat(completed).isFalse();
    }

    @Test
    void rejectsBatchSizeBelowOne() {
        assertThatThrownBy(() -> subscriber(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Batch size must be at least 1, but is 0");
    }

    private BatchRequestSubscriber<Integer> subscriber(int batchSize) {
        return new BatchRequestSubscriber<>(batchSize, received::add, error::set, () -> completed.set(true));
    }
}
