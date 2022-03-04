package com.future.concurrent.api;


import com.future.concurrent.history.javaold.Java7ForkJoinPool;
import com.future.concurrent.history.javaold.Java7RecursiveTask;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

/**
 * ForkJoinPool API
 *
 * @author future
 */
@SuppressWarnings("all")
class ForkJoinAPI {

    private final ForkJoinPool pool = new ForkJoinPool();
    private final Java7ForkJoinPool java7Pool = new Java7ForkJoinPool();

    double sumOfSquares(ForkJoinPool pool, double[] array) {
        int n = array.length;
        Applyer a = new Applyer(array, 0, n, null);
        pool.invoke(a);
        return a.result;
    }

    /**
     * 下面的例子说明了一些可能导致更好的性能的细化和习惯做法。
     * 递归动作不需要完全递归，只要它们保持基本的分割和征服方法。
     * 这是一个对双数组中每个元素的平方进行求和的类，
     * 它只对重复除以 2 的右手边进行细分，并通过一连串的下一个引用对其进行跟踪。
     * 它使用一个基于 getSurplusQueuedTaskCount 方法的动态阈值，
     * 但通过直接对未窃取的任务进行叶子操作而不是进一步细分来抵消潜在的过度分区。
     */
    static class Applyer extends RecursiveAction {
        final double[] array;
        final int lo, hi;
        double result;
        Applyer next;

        Applyer(double[] array, int lo, int hi, Applyer next) {
            this.array = array;
            this.lo = lo;
            this.hi = hi;
            this.next = next;
        }

        double atLeaf(int l, int h) {
            double sum = 0;
            for (int i = l; i < h; i++) {
                sum += array[i] * array[i];
            }
            return sum;
        }

        @Override
        protected void compute() {
            int l = lo;
            int h = hi;
            Applyer right = null;
            while (h - 1 > 1 && getSurplusQueuedTaskCount() <= 3) {
                int mid = (l + h) >>> 1;
                right = new Applyer(array, mid, h, right);
                right.fork();
                h = mid;
            }
            double sum = atLeaf(l, h);
            while (right != null) {
                if (right.tryUnfork()) {
                    sum += right.atLeaf(right.lo, right.hi);
                } else {
                    right.join();
                    sum += right.result;
                }
                right = right.next;
            }
            result = sum;
        }
    }

    static class IncrementTask extends RecursiveAction {
        private static final int THRESHOLD = 10;
        final long[] array;
        final int lo;
        final int hi;

        IncrementTask(long[] array, int lo, int hi) {
            this.array = array;
            this.lo = lo;
            this.hi = hi;
        }

        @Override
        protected void compute() {
            if (hi - lo < THRESHOLD) {
                for (int i = lo; i < hi; i++) {
                    array[i]++;
                }
            } else {
                int mid = (lo + hi) >>> 1;
                invokeAll(new IncrementTask(array, lo, mid), new IncrementTask(array, mid, hi));
            }
        }
    }

    static class SortTask extends RecursiveAction {
        private static final int THRESHOLD = 5;
        final long[] array;
        final int lo, hi;

        SortTask(long[] array, int lo, int hi) {
            this.array = array;
            this.lo = lo;
            this.hi = hi;
        }

        SortTask(long[] array) {
            this(array, 0, array.length);
        }

        @Override
        protected void compute() {
            if (hi - lo < THRESHOLD)
                sortSequentially(lo, hi);
            else {
                int mid = (lo + hi) >>> 1;
                invokeAll(new SortTask(array, lo, mid),
                        new SortTask(array, mid, hi));
                merge(lo, mid, hi);
            }
        }

        void sortSequentially(int lo, int hi) {
            Arrays.sort(array, lo, hi);
        }

        void merge(int lo, int mid, int hi) {
            long[] buf = Arrays.copyOfRange(array, lo, mid);
            for (int i = 0, j = lo, k = mid; i < buf.length; j++) {
                array[j] = (k == hi || buf[i] < array[k]) ? buf[i++] : array[k++];
            }
        }
    }

    /**
     * 然而，除了是一种计算斐波那契函数的愚蠢方式
     * （有一个简单的快速线性算法，你在实践中会使用），
     * 这很可能表现不佳，因为最小的子任务太小，不值得拆分。
     * 相反，就像几乎所有的 fork/join 应用一样，你会选择一些最小的颗粒度大小
     * （例如这里的 10），对于这些颗粒度，你总是 顺序解决，而不是进行细分。
     */
    static class Fibonacci extends RecursiveTask<Integer> {
        final int n;

        Fibonacci(int n) {
            this.n = n;
        }

        @Override
        protected Integer compute() {
            if (n <= 1)
                return n;
            Fibonacci f1 = new Fibonacci(n - 1);
            f1.fork();
            Fibonacci f2 = new Fibonacci(n - 2);
            return f2.compute() + f1.join();
        }
    }

    private static class Task extends Java7RecursiveTask<Integer> {
        private final int[] numbers;
        private final int start;
        private final int end;

        public Task(int[] numbers, int from, int to) {
            this.numbers = numbers;
            this.start = from;
            this.end = to;
        }

        @Override
        protected Integer compute() {
            if (end - start < 5) {
                int total = 0;
                for (int i = start; i <= end; i++) {
                    total += numbers[i];
                }
                return total;
            } else {
                int middle = (start + end) >>> 1;
                Task left = new Task(numbers, start, middle);
                Task right = new Task(numbers, middle + 1, end);
                right.fork();
                return left.compute() + right.join();
            }
        }
    }

    public int sum(int[] numbers) {
        return java7Pool.invoke(new Task(numbers, 0, numbers.length - 1));
    }

    public static void main(String[] args) {
        int[] array = IntStream.rangeClosed(1, 100).toArray();
        int sum = new ForkJoinAPI().sum(array);
        System.out.println(sum);
    }
}
