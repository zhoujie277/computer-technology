package com.future.other.generic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 结论参照 WildcardDemo.java <br/>
 * 分析过程请查看该类代码
 * 
 * @author zhoujie
 */
class QualifiedWildcardDemo {

    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    static class Animal {
        String name;
    }

    @NoArgsConstructor
    @ToString(callSuper = true)
    static class Cat extends Animal {
        int age;

        public Cat(String name, int age) {
            super(name);
            this.age = age;
        }
    }

    @NoArgsConstructor
    @ToString(callSuper = true)
    static class MiniCat extends Cat {
        int level;

        public MiniCat(String name, int age, int level) {
            super(name, age);
            this.level = level;
        }
    }

    static class NameComparator implements Comparator<Animal> {
        @Override
        public int compare(Animal o1, Animal o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    static class LevelComparator implements Comparator<MiniCat> {

        @Override
        public int compare(MiniCat o1, MiniCat o2) {
            return o1.level - o2.level;
        }
    }

    @SuppressWarnings("unused")
    public static void addExtendsCats(List<? extends Cat> cats) {
        Animal animal1 = new Animal("animal1");
        Cat cat1 = new Cat("cat1", 18);
        MiniCat miniCat1 = new MiniCat("miniCat1", 21, 1);
        // 由于上限通配符的限定，使得 cats 表示的类型只知道是 Cat 的子类，但确定不了具体是哪个子类，因为 子类与子类之间是类型转换不安全的。所以通过
        // extends 修饰的通配符，不能往其设置任何值。
        // cats.add(animal1);
        // cats.add(cat1);
        // cats.add(miniCat1);
        // null 是任何类型都可以设置的，所以能设置成功。
        cats.add(null);
    }

    public static void showExtendCats(List<? extends Cat> cats) {
        // 能够确定 cats 类存放的是 Cat 或其子类，故可以用 Cat接收其具体存放的类型。
        for (Cat cat : cats) {
            System.out.println(cat);
        }
    }

    @SuppressWarnings("unused")
    public static void addSuperCats(List<? super Cat> cats) {
        Animal animal1 = new Animal("animal1");
        Cat cat1 = new Cat("cat1", 18);
        MiniCat miniCat1 = new MiniCat("miniCat1", 21, 1);
        // animals.add(animal1);
        // 可以 cats 存放的类型一定是 Cat的父类，而 Cat及 Cat的子类都满足继承关系。所以 cats 中能继续添加 Cat 及其 Cat子类的元素
        cats.add(cat1);
        cats.add(miniCat1);
    }

    public static void showSuperCats(List<? super Cat> cats) {
        // 下限通配符 cats 集合存放的是 Cat 或者其父类，由于无法确定该集合中具体存放的是哪一种类型，
        // 但是所有的类都继承 Object，所以可以且只能用 Object 接收
        for (Object cat : cats) {
            System.out.println(cat);
        }
    }

    public static void main(String[] args) {
        Animal animal1 = new Animal("animal1");
        Animal animal2 = new Animal("animal2");
        Cat cat1 = new Cat("cat1", 18);
        Cat cat2 = new Cat("cat2", 16);
        Cat cat3 = new Cat("cat3", 12);
        Cat cat4 = new Cat("cat4", 20);
        MiniCat miniCat1 = new MiniCat("miniCat1", 21, 1);
        MiniCat miniCat2 = new MiniCat("miniCat2", 22, 2);
        MiniCat miniCat3 = new MiniCat("miniCat4", 23, 3);
        MiniCat miniCat4 = new MiniCat("miniCat4", 24, 4);
        MiniCat miniCat5 = new MiniCat("miniCat5", 25, 5);
        List<Animal> animals = new ArrayList<>();
        List<Cat> cats = new ArrayList<>();
        List<MiniCat> miniCats = new ArrayList<>();

        animals.add(animal1);
        animals.add(animal2);

        cats.add(cat1);
        cats.add(cat2);
        cats.add(cat3);
        cats.add(cat4);

        miniCats.add(miniCat1);
        miniCats.add(miniCat2);
        miniCats.add(miniCat3);
        miniCats.add(miniCat4);
        miniCats.add(miniCat5);

        // 上限通配符 只能传递 Cat 或其子类的集合
        // showExtendCats(animals);
        showExtendCats(cats);
        showExtendCats(miniCats);

        // 下限通配符，只能传递 Cat 或其父类的集合。
        showSuperCats(animals);
        showSuperCats(cats);
        // showSuperCats(miniCats);

    }
}
