package com.future.concurrent.javaold;

public abstract class Java7RecursiveAction extends Java7ForkJoinTask<Void> {

    protected abstract void compute();

    public final Void getRawResult() {
        return null;
    }

    protected final void setRawResult(Void mustBeNull) {
    }

    protected final boolean exec() {
        compute();
        return true;
    }
}
