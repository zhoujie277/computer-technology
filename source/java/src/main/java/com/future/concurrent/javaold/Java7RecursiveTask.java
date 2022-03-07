package com.future.concurrent.javaold;

/**
 * Java 7 RecursiveTask
 */
public abstract class Java7RecursiveTask<V> extends Java7ForkJoinTask<V> {
    V result;

    /**
     * The main computation performed by this task.
     */
    protected abstract V compute();

    public final V getRawResult() {
        return result;
    }

    protected final void setRawResult(V value) {
        result = value;
    }

    /**
     * Implements execution conventions for RecursiveTask.
     */
    protected final boolean exec() {
        result = compute();
        return true;
    }
}
