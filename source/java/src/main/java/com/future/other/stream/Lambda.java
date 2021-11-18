package com.future.other.stream;

import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;

import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("unused")
@Slf4j
public class Lambda {

    private void simplify1() {
        // lambda 表达式功能之一，优化某些匿名内部类的写法
        new Thread(() -> {
            System.out.println("thread run...");
        }).start();
    }

    private static int calculateNum(IntBinaryOperator operator) {
        int a = 10;
        int b = 20;
        return operator.applyAsInt(a, b);
    }

    private static int calculateInvoke() {
        int sum = calculateNum(new IntBinaryOperator() {
            @Override
            public int applyAsInt(int left, int right) {
                return left + right;
            }
        });
        return sum;
    }

    private static int calculateLambda() {
        int sum = calculateNum((left, right) -> {
            return left + right;
        });
        return sum;
    }

    private static void printNum(IntPredicate predicate) {
        int[] arr = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        for (int i : arr) {
            if (predicate.test(i)) {
                System.out.print(i + "\t");
            }
        }
    }

    private static void printLambda() {
        printNum((num) -> num <= 5);
        System.out.println();
    }

    private static <S, R> R typeConvert(S s, Function<S, R> function) {
        R result = function.apply(s);
        return result;
    }

    private static void typeConvert() {
        int t1 = typeConvert("123", (new Function<String, Integer>() {
            @Override
            public Integer apply(String t) {
                return Integer.parseInt(t);
            }
        }));
        log.debug("t1 = {}", t1);

        int t2 = typeConvert("123", (t) -> {
            return Integer.parseInt(t);
        });
        log.debug("t2 = {}", t2);

        double t3 = typeConvert("123", (t) -> Double.parseDouble(t));
        log.debug("t2 = {}", t3);
    }

    public void run() {
        int sum = calculateLambda();
        System.out.println(sum);
        printLambda();
        typeConvert();
    }

    public static void main(String[] args) {
        new Lambda().run();
    }

}
