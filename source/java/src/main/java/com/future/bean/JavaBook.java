package com.future.bean;

@SuppressWarnings("unused")
public class JavaBook extends Book {

    public static int JAVA_BOOK_MEMBER = 1;

    public JavaBook(String name) {
        super(name);
        this.type = Book.TYPE_JAVA;
    }

    static {
        System.out.println("class JavaBook was clInit!");
    }
}
