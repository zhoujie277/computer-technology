package com.future.other.generic;

/**
 * Java 泛型提供了编译时类型安全检测机制 <br/>
 * 并且在编译之后会进行类型擦除。<br/>
 * ? 通配符默认会擦除为 Object 类型 <br/>
 * ? extends T 默认情况下会擦除为具体的 T 类型。
 * 
 * 结论：泛型通配符及其上下限通配符通常只在作为方法传递参数的时候使用 <br/>
 * 上限通配符 extends 通常作为只读参数传递。<br/>
 * 下限通配符 super 通常作为可写参数传递。<br/>
 * 注意：下限通配符能读取，但是只能用 Object 来接收。原因是 Java 中所有的类都是继承 Object <br/>
 * 限定通配符的分析过程请参照 WildcardDemo.java <br/>
 * 
 * @author zhoujie
 */
class WildcardDemo {

    static class Box<T> {
        T value;

        T get() {
            return value;
        }

        void set(T t) {
            this.value = t;
        }
    }

    // 没有增加任何通配上限和下限修饰符的泛型 ？
    // 等价于 Box<? extends Object>
    // 即只能用 Object 来接收值，并且不能修改 box 中的值。
    // 因为给定的 box 原来的数据类型，并不能确定具体是哪个类型，如果再添加数据，无法确定类型兼容。
    public static void showBox(Box<?> box) {
        Object object = box.get();
        System.out.println(object);
    }

    public static void main(String[] args) {
        Box<String> box = new Box<>();
        box.set("hello");
        showBox(box);
    }
}
