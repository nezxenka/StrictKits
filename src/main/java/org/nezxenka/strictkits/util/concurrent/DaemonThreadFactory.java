package org.nezxenka.strictkits.util.concurrent;

import lombok.RequiredArgsConstructor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public final class DaemonThreadFactory implements ThreadFactory {

    private final String name;
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, name + "-" + counter.incrementAndGet());
        thread.setDaemon(true);
        return thread;
    }
}
