package com.future.javac;

import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.ScannerFactory;
import com.sun.tools.javac.util.Context;

class JavacParserDemo {

    private void scanner() {
        ScannerFactory factory = ScannerFactory.instance(new Context());
        Scanner scanner = factory.newScanner("int k = i + j;", false);

        scanner.nextToken();
        System.out.println(scanner.token().kind);       // int
        scanner.nextToken();
        System.out.println(scanner.token().name());     // k
        scanner.nextToken();
        System.out.println(scanner.token().kind);       // =
        scanner.nextToken();
        System.out.println(scanner.token().name());     // i
        scanner.nextToken();
        System.out.println(scanner.token().kind);       // +
        scanner.nextToken();
        System.out.println(scanner.token().name());     // j
        scanner.nextToken();
        System.out.println(scanner.token().kind);       // ;
    }

    public static void main(String[] args) {
        JavacParserDemo demo = new JavacParserDemo();
        demo.scanner();
    }
}
