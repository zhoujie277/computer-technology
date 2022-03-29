package com.future.bean;

@SuppressWarnings("unused")
public class Book {

    public static final int TYPE_JAVA = 1;
    public static final int TYPE_LINUX = 2;

    public static int BOOK_MEMBER = 1;

    private String name;
    private double price;
    protected int type;
    private String info;

    public Book(String name) {
        this.name = name;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    static {
        System.out.println("class Book was clInit!");
    }
}
