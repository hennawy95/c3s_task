package com.example.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
//        Scanner in = new Scanner(System.in);
        System.out.println(new Date() + " Start program: ");
//        int num_of_processes = in.nextInt();

        Master master = new Master();
        master.init();


    }
}
