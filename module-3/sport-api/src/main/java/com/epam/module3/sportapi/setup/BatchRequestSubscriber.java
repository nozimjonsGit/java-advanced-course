package com.epam.module3.sportapi.setup;

import java.util.function.Consumer;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;

/**
 * Applies backpressure: requests {@code batchSize} elements when it subscribes, and requests the
 * next {@code batchSize} only after it has received all elements of the current batch. Upstream
 * therefore never has more than one batch in flight.
 *
 * @param <T> type of the elements
 */
public class BatchRequestSubscriber<T> extends BaseSubscriber<T> {

    private final int batchSize;
    private final Consumer<? super T> onNext;
    private final Consumer<? super Throwable> onError;
    private final Runnable onComplete;

    /** Elements received since the last request. Signals arrive one at a time, so no locking is needed. */
    private int receivedInBatch;

    /**
     * @param batchSize  how many elements to request at a time
     * @param onNext     called with each element
     * @param onError    called if upstream fails
     * @param onComplete called when upstream completes
     */
    public BatchRequestSubscriber(
            int batchSize, Consumer<? super T> onNext, Consumer<? super Throwable> onError, Runnable onComplete) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1, but is " + batchSize);
        }
        this.batchSize = batchSize;
        this.onNext = onNext;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(batchSize);
    }

    @Override
    protected void hookOnNext(T value) {
        onNext.accept(value);
        receivedInBatch++;
        if (receivedInBatch == batchSize) {
            receivedInBatch = 0;
            request(batchSize);
        }
    }

    @Override
    protected void hookOnError(Throwable throwable) {
        onError.accept(throwable);
    }

    @Override
    protected void hookOnComplete() {
        onComplete.run();
    }
}
