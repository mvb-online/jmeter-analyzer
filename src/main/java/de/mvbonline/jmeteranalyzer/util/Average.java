package de.mvbonline.jmeteranalyzer.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mholz on 22.12.2015.
 */
public class Average {
    private AtomicLong sum = new AtomicLong();

    private AtomicLong count = new AtomicLong();

    public long count() {
        return count.get();
    }

    public void add(long value) {
        count.incrementAndGet();
        sum.addAndGet(value);
    }

    public synchronized void reset() {
        count.set(0);
        sum.set(0);
    }

    public synchronized double average() {
        return (double) sum.get() / (double) count.get();
    }
}
