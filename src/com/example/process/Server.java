package com.example.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(4949);
        Socket s = ss.accept();

        System.out.println("Client connected");

        InputStreamReader ir = new InputStreamReader(s.getInputStream());
        BufferedReader br = new BufferedReader(ir);

        String str = br.readLine();
        System.out.println("Client: " + str);


        PrintWriter pr = new PrintWriter(s.getOutputStream());
        pr.println("Server Hi");
        pr.flush();
    }
}
