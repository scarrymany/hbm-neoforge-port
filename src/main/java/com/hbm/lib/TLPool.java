package com.hbm.lib;

import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * CE backs the shared overflow pool with {@code org.jctools.queues.MpmcArrayQueue}. jctools-core is
 * not on this port project's classpath yet (see the lib_util Phase 0 report), so the shared pool here
 * uses {@link ArrayBlockingQueue} instead: its non-blocking {@code poll()}/{@code offer()} give the
 * same "try, don't block" semantics as jctools' {@code relaxedPoll()}/{@code relaxedOffer()}, just
 * without the lock-free performance characteristics. Swap back to {@code MpmcArrayQueue} once
 * jctools-core is added to {@code build.gradle}.
 */
public final class TLPool<T> {
    private final ThreadLocal<ArrayDeque<T>> local;
    private final ArrayBlockingQueue<T> shared;

    private final Supplier<T> factory;
    private final Consumer<T> reset;
    private final int localCap;

    public TLPool(Supplier<T> factory, Consumer<T> reset, int localCap, int sharedCap) {
        this.factory = Objects.requireNonNull(factory);
        this.reset = Objects.requireNonNull(reset);
        this.localCap = Math.max(1, localCap);
        if (sharedCap <= 0) throw new IllegalArgumentException("sharedCap must be > 0");
        this.shared = new ArrayBlockingQueue<>(sharedCap);
        this.local = ThreadLocal.withInitial(() -> new ArrayDeque<>(this.localCap));
    }

    public T borrow() {
        ArrayDeque<T> q = local.get();
        T t = q.pollLast();
        if (t != null) return t;

        int moved = 0;
        while (moved < localCap) {
            T s = shared.poll();
            if (s == null) break;
            q.addLast(s);
            moved++;
        }

        t = q.pollLast();
        if (t != null) return t;

        t = factory.get();
        if (t == null) throw new NullPointerException();
        return t;
    }

    public void recycle(T t) {
        if (t == null) throw new NullPointerException();
        try {
            reset.accept(t);
        } catch (RuntimeException ignored) {
            return;
        }
        ArrayDeque<T> q = local.get();
        if (q.size() < localCap) {
            q.addLast(t);
            return;
        }
        shared.offer(t);
    }

    public void clearLocal() {
        local.remove();
    }

    public int trimSharedTo(int max) {
        if (max < 0) throw new IllegalArgumentException("max < 0");
        int toRemove = Math.max(0, shared.size() - max);
        int removed = 0;
        while (removed < toRemove) {
            if (shared.poll() == null) break;
            removed++;
        }
        return removed;
    }
}
