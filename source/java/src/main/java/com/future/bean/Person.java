package com.future.bean;

@SuppressWarnings("unused")
public class Person {
    private int age;
    private String name;
    private double finance;
    private String school;

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }

    public double getFinance() {
        return finance;
    }

    public String getSchool() {
        return school;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFinance(double finance) {
        this.finance = finance;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    @Override
    public String toString() {
        return "Person{" +
                "age=" + age +
                ", name='" + name + '\'' +
                ", finance=" + finance +
                ", school='" + school + '\'' +
                '}';
    }
}
