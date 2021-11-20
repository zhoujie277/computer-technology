package com.future.util;

import java.util.UUID;

public class Utility {

    public static String UUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static void main(String[] args) {
        System.out.println(UUID());
    }
}
