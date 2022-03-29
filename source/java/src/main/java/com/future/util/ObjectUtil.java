package com.future.util;

import com.future.bean.Person;
import org.apache.lucene.util.RamUsageEstimator;

/**
 * //计算指定对象及其引用树上的所有对象的综合大小，单位字节
 * long RamUsageEstimator.sizeOf(Object obj)
 * <p>
 * //计算指定对象本身在堆空间的大小，单位字节
 * long RamUsageEstimator.shallowSizeOf(Object obj)
 * <p>
 * //计算指定对象及其引用树上的所有对象的综合大小，返回可读的结果，如：2KB
 * String RamUsageEstimator.humanSizeOf(Object obj)
 */
public class ObjectUtil {

    public static long shallowSizeOf(Object obj) {
        return RamUsageEstimator.shallowSizeOf(obj);
    }

    public static void main(String[] args) {
        boolean i = false;
        Integer a = 16;
        Long b = 24L;
        Person p = new Person("LiuYuXi");
        System.out.println("boolean size: " + RamUsageEstimator.sizeOf(i));
        System.out.println("Integer size: " + shallowSizeOf(a));
        System.out.println("Long size: " + shallowSizeOf(b));
        System.out.println("shallow size person: " + shallowSizeOf(p));
    }
}
