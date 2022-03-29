package com.future.jvm.bytecode;

import com.future.bean.Container;
import com.future.bean.IType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * 泛型分析器
 */
@SuppressWarnings("all")
public class GenericBytecode {


    static class StringType implements IType<String> {

        /*
         *  0 aload_0
         *  1 invokevirtual #18 <java/lang/Object.getClass : ()Ljava/lang/Class;>
         *  4 invokevirtual #22 <java/lang/Class.getGenericInterfaces : ()[Ljava/lang/reflect/Type;>
         *  7 astore_2
         *  8 aload_2
         *  9 dup
         * 10 astore 6
         * 12 arraylength
         * 13 istore 5
         * 15 iconst_0
         * 16 istore 4
         * 18 goto 37 (+19)
         * 21 aload 6
         * 23 iload 4
         * 25 aaload
         * 26 astore_3
         * 27 getstatic #28 <java/lang/System.out : Ljava/io/PrintStream;>
         * 30 aload_3
         * 31 invokevirtual #34 <java/io/PrintStream.println : (Ljava/lang/Object;)V>
         * 34 iinc 4 by 1
         * 37 iload 4
         * 39 iload 5
         * 41 if_icmplt 21 (-20)
         * 44 getstatic #28 <java/lang/System.out : Ljava/io/PrintStream;>
         * 47 aload_1
         * 48 invokevirtual #40 <java/io/PrintStream.println : (Ljava/lang/String;)V>
         * 51 return
         */
        @Override
        public void run(String s) {
            Type[] genericInterfaces = this.getClass().getGenericInterfaces();
            // 获取参数化类型
            for (Type type : genericInterfaces) {
                System.out.println(type);
            }
            System.out.println(s);
        }

        /*
         * 内部生成了以下方法。
         *
         * 0 aload_0
         * 1 aload_1
         * 2 checkcast #49 <java/lang/String>
         * 5 invokevirtual #52 <com/future/jvm/GenericAnalyzer$StringType.run : (Ljava/lang/String;)V>
         * 8 return
         */
        // public synthetic bridge void run(Object t) {
        //      this.run(String)t);
        // }
    }

    public static void main(String[] args) {
        Container<Integer> subClass = new Container<Integer>() {
            // 之能从子类获取父类的泛型类型
        };
        Type type = subClass.getClass().getGenericSuperclass();
        System.out.println(type);
        System.out.println("-------------------");
        ParameterizedType parameterized = (ParameterizedType) type;
        System.out.println((parameterized.getActualTypeArguments()[0]));
        System.out.println("-------------------");
        StringType stringType = new StringType();
        Type[] types = stringType.getClass().getGenericInterfaces();
        System.out.println(Arrays.toString(types));
        stringType.run("hello");
    }
}
