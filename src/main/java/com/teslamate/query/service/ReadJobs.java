package com.teslamate.query.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Run independent read-only work on virtual threads.
 * Hikari still caps real connections; extras just wait.
 */
public final class ReadJobs {

    private static final ExecutorService VIRTUAL = Executors.newVirtualThreadPerTaskExecutor();

    private ReadJobs() {}

    public static <A, B> Pair<A, B> both(Supplier<A> first, Supplier<B> second) {
        Future<A> fa = VIRTUAL.submit(first::get);
        Future<B> fb = VIRTUAL.submit(second::get);
        return new Pair<>(join(fa), join(fb));
    }

    @SafeVarargs
    public static <T> List<T> all(Supplier<T>... jobs) {
        return all(List.of(jobs));
    }

    public static <T> List<T> all(List<Supplier<T>> jobs) {
        if (jobs == null || jobs.isEmpty()) {
            return List.of();
        }
        if (jobs.size() == 1) {
            return List.of(jobs.getFirst().get());
        }
        List<Future<T>> futures = new ArrayList<>(jobs.size());
        for (Supplier<T> job : jobs) {
            futures.add(VIRTUAL.submit(job::get));
        }
        List<T> out = new ArrayList<>(futures.size());
        for (Future<T> future : futures) {
            out.add(join(future));
        }
        return out;
    }

    public static <T, R> List<R> map(List<T> items, int maxConcurrent, Function<T, R> fn) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        int limit = Math.max(1, maxConcurrent);
        Semaphore permits = new Semaphore(limit);
        List<Supplier<R>> jobs = new ArrayList<>(items.size());
        for (T item : items) {
            jobs.add(() -> {
                permits.acquireUninterruptibly();
                try {
                    return fn.apply(item);
                } finally {
                    permits.release();
                }
            });
        }
        return all(jobs);
    }

    static <T> T join(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException(cause);
        }
    }

    public record Pair<A, B>(A first, B second) {}
}
