package com.future.other.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class StreamApi {

    private static List<Author> initData() {
        Author author1 = new Author(1L, "郭靖", 40, "射雕英雄传", null);
        Author author2 = new Author(2L, "杨过", 15, "神雕侠侣", null);
        Author author3 = new Author(3L, "小龙女", 16, "终南山下", null);
        Author author4 = new Author(4L, "郭襄", 12, "峨眉派", null);

        List<Book> books1 = new ArrayList<>();
        List<Book> books2 = new ArrayList<>();
        List<Book> books3 = new ArrayList<>();
        // List<Book> books4 = new ArrayList<>();

        books1.add(new Book(1L, "C++ Primer", "编程,语言", 88, "C++ 经典"));
        books1.add(new Book(2L, "Java 编程思想", "编程,语言", 98, "Java 经典"));

        books2.add(new Book(3L, "Linux 高性能服务器", "操作系统,Linux", 80, "Linux 高性能服务器"));
        books2.add(new Book(4L, "Unix 网络编程", "编程,网络", 88, "经典咏流传"));
        books2.add(new Book(5L, "Netty 高并发实战", "Netty,网络", 70, "Netty网络编程入门书籍"));

        books3.add(new Book(6L, "Linux GNU C 程序观察", "Linux,语言,实践", 80, "填补Linux和C语言之间的空洞"));
        books3.add(new Book(7L, "汇编语言", "编程,汇编", 85, "掌握计算机执行机制"));
        books3.add(new Book(8L, "32位保护模式", "底层,汇编", 86, "了解计算机保护模式下的工作机制"));

        author1.setBooks(books1);
        author2.setBooks(books2);
        author3.setBooks(books3);
        author4.setBooks(books3);

        List<Author> result = new ArrayList<>();
        result.add(author1);
        result.add(author2);
        result.add(author3);
        result.add(author4);
        return result;
    }

    // 找到所有年龄小于18的作家的名字，并且要注意去重
    public void resolve1() {
        List<Author> datas = initData();
        Object[] array = datas.stream().filter(author -> author.getAge() < 18).distinct().toArray();
        datas.stream().filter(author -> author.getAge() < 18).distinct().forEach(author -> System.out.println(author));
        for (Object o : array) {
            log.debug("{}", ((Author) o).getName());
        }
    }

    // 打印所有姓名长度大于2的作家的姓名
    private void practice2() {
        log.debug("practice2");
        List<Author> datas = initData();
        datas.stream().filter(author -> author.getName().length() > 2)
                .forEach(author -> System.out.println(author.getName()));
    }

    // 通过map转换，打印作家姓名
    private void practice3() {
        log.debug("practice3");
        List<Author> datas = initData();
        datas.stream().map(author -> author.getName()).forEach(name -> System.out.println(name));
    }

    private void sorted() {
        List<Author> datas = initData();
        datas.stream().sorted((a, b) -> a.getAge() - b.getAge()).forEach(author -> log.debug(author.getName()));
    }

    // 找出年龄最大的两个作家的姓名
    private void limit() {
        List<Author> datas = initData();
        datas.stream().sorted((a, b) -> b.getAge() - a.getAge()).limit(2)
                .forEach(author -> log.debug(author.getName()));
    }

    // 打印除了年龄最大的作家的姓名
    private void skip() {
        List<Author> datas = initData();
        datas.stream().sorted((a, b) -> b.getAge() - a.getAge()).skip(1).forEach(author -> log.debug(author.getName()));
    }

    private void flatMap() {
        // 打印所有书籍的名字，要求对重复的元素进行去重
        List<Author> datas = initData();
        datas.stream().flatMap(author -> author.getBooks().stream()).distinct()
                .forEach(book -> log.debug(book.getName()));
        System.out.println("=------------------------------=");
        // 打印现有书籍的所有分类，要求对分类进行去重，不能出现这种格式：哲学,爱情
        datas.stream().flatMap(author -> author.getBooks().stream())
                .flatMap(book -> Stream.of(book.getCategory().split(","))).distinct().forEach(cc -> log.debug(cc));
    }

    private void stat() {
        List<Author> datas = initData();
        long count = datas.stream().flatMap(author -> author.getBooks().stream()).count();
        Optional<Book> max = datas.stream().flatMap(author -> author.getBooks().stream())
                .max((a, b) -> a.getScore() - b.getScore());
        Optional<Book> min = datas.stream().flatMap(author -> author.getBooks().stream())
                .min((a, b) -> a.getScore() - b.getScore());
        log.debug("stream count {}", count);
        log.debug("stream max {}", max.get());
        log.debug("stream min {}", min.get());
    }

    private void collect() {
        List<Author> datas = initData();
        List<String> names = datas.stream().map(author -> author.getName()).collect(Collectors.toList());
        log.debug("{}", names);

        // toMap
        Map<String, List<Book>> authorMap = datas.stream()
                .collect(Collectors.toMap(author -> author.getName(), author -> author.getBooks()));
        log.debug("{}", authorMap);
    }

    private void match() {
        List<Author> datas = initData();
        // 是否有年龄在29岁以上的作家
        boolean result = datas.stream().anyMatch(author -> author.getAge() > 29);
        log.debug("anyMatch 29 {}", result);
        // 判断作家是否都是成年人
        result = datas.stream().allMatch(author -> author.getAge() >= 18);
        log.debug("allMatch 18 {}", result);

        // 判断作家都小于50岁
        result = datas.stream().noneMatch(author -> author.getAge() > 50);
        log.debug("noneMatch 50 {}", result);
    }

    private void find() {
        List<Author> datas = initData();
        // 找到任意一个成年作家，如果存在就输出
        Optional<Author> findAny = datas.stream().filter(author -> author.getAge() > 18).findAny();
        findAny.ifPresent(author -> log.debug("findAny {}", author));

        // 找到年龄最小的成年作家
        Optional<Author> findFirst = datas.stream().sorted((a, b) -> a.getAge() - b.getAge()).findFirst();
        findFirst.ifPresent(author -> log.debug("findFirst {}", findFirst));
    }

    private void reduce() {
        List<Author> datas = initData();
        // 使用 reduce 求所有作者年龄的和
        Integer sumAge = datas.stream().map(Author::getAge).reduce(0, (acc, other) -> acc + other);
        log.debug("sumAge {}", sumAge);
    }

    private void optional() {
        List<Author> datas = initData();
        Optional<Author> authorOptional = Optional.ofNullable(datas.get(0));
        authorOptional.map(author -> author.getBooks()).ifPresent(books -> log.debug("optional {}", books));
    }

    public void run() {
        resolve1();
        System.out.println("======================");
        practice2();
        System.out.println("======================");
        practice3();
        System.out.println("======================");
        sorted();
        System.out.println("======================");
        limit();
        System.out.println("======================");
        skip();
        System.out.println("======================");
        flatMap();
        System.out.println("======================");
        stat();
        System.out.println("======================");
        collect();
        System.out.println("======================");
        match();
        System.out.println("======================");
        find();
        System.out.println("======================");
        reduce();
        System.out.println("======================");
        optional();
    }

    public static void main(String[] args) {
        new StreamApi().run();
    }

}
