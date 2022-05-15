package com.future.algorithm.sort;

import java.util.Arrays;
import java.util.Map;

public class MergeSorter {

    static void divide(int[] array, int left, int right, int[] extra) {
        if (left == right) {
            return;
        }
        int middle = (left + right) >> 1;
        divide(array, left, middle, extra);
        divide(array, middle + 1, right, extra);
        combine(array, left, middle, right, extra);
    }

    static void combine(int[] array, int left, int middle, int right, int[] extra) {
        int i = left, j = middle + 1, k = left;
        while (i <= middle && j <= right) extra[k++] = array[i] < array[j] ? array[i++] : array[j++];
        while (i <= middle) extra[k++] = array[i++];
        while (j <= right) extra[k++] = array[j++];
        k = left;
        for (int l = left; l <= right; l++) {
            array[l] = extra[k++];
        }
    }

    static void topDown(int[] array) {
        int[] extra = new int[array.length];
        divide(array, 0, array.length - 1, extra);
        System.out.println(Arrays.toString(extra));
    }

    static void bottomUp(int[] array) {
        int k = 1;
        int lastIndex = array.length - 1;
        int[] extra = new int[array.length];
        int m = 1;
        while (Math.pow(2, m) < array.length) {
            m++;
        }
        while (k <= m) {
            for (int i = 0; i < array.length; i += Math.pow(2, k)) {
                int right = Math.min((int) Math.pow(2, k) + i - 1, lastIndex);
                int middle = i + right >> 1;
                combine(array, i, middle, right, extra);
            }
            k++;
        }
    }

    public static void main(String[] args) {
        int[] array = new int[]{7, 6, 4, 5, 3, 1, 2};
//        MergeSorter.topDown(array);
        MergeSorter.bottomUp(array);
        System.out.println(Arrays.toString(array));
    }
}
