package com.lzxx.lojcodesandboxsdk;

import java.util.Objects;
import java.util.Scanner;

public class SimpleCompute {

    public static void main(String[] args) {
        Integer a = Integer.parseInt(args[0]);
        Integer b = Integer.parseInt(args[1]);
        System.out.println(a + b);
    }

    public static void main2(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Integer a = scanner.nextInt();
        Integer b = scanner.nextInt();
        System.out.println(a + b);
    }
}


class Main {
    public int addTwoNum(int num1, int num2) {
        return num1 + num2;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        int num1 = scanner.nextInt();
        int num2 = scanner.nextInt();

        Main main = new Main();
        int ret = main.addTwoNum(num1, num2);
        System.out.println(ret);
    }
}