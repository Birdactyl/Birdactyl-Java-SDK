package io.birdactyl.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Futures {
    private Futures() {}

    @SafeVarargs
    public static <T> CompletableFuture<List<T>> all(CompletableFuture<T>... futures) {
        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    List<T> results = new ArrayList<>(futures.length);
                    for (CompletableFuture<T> f : futures) {
                        results.add(f.join());
                    }
                    return results;
                });
    }

    public static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
        @SuppressWarnings("unchecked")
        CompletableFuture<T>[] arr = futures.toArray(new CompletableFuture[0]);
        return all(arr);
    }

    @SafeVarargs
    public static <T> CompletableFuture<T> any(CompletableFuture<T>... futures) {
        return CompletableFuture.anyOf(futures)
                .thenApply(obj -> {
                    @SuppressWarnings("unchecked")
                    T result = (T) obj;
                    return result;
                });
    }

    public static <T, R> CompletableFuture<R> map(CompletableFuture<T> future, Function<T, R> fn) {
        return future.thenApply(fn);
    }

    public static <T, R> CompletableFuture<R> flatMap(CompletableFuture<T> future, Function<T, CompletableFuture<R>> fn) {
        return future.thenCompose(fn);
    }

    public static <T> CompletableFuture<T> completed(T value) {
        return CompletableFuture.completedFuture(value);
    }

    public static <T> CompletableFuture<T> failed(Throwable ex) {
        CompletableFuture<T> f = new CompletableFuture<>();
        f.completeExceptionally(ex);
        return f;
    }

    public static CompletableFuture<Void> parallel(Runnable... actions) {
        CompletableFuture<?>[] futures = new CompletableFuture[actions.length];
        for (int i = 0; i < actions.length; i++) {
            final int idx = i;
            futures[i] = CompletableFuture.runAsync(actions[idx]);
        }
        return CompletableFuture.allOf(futures);
    }

    public static <T, R> CompletableFuture<List<R>> sequence(List<T> items, Function<T, CompletableFuture<R>> fn) {
        List<CompletableFuture<R>> futures = items.stream()
                .map(fn)
                .collect(Collectors.toList());
        return all(futures);
    }
}
