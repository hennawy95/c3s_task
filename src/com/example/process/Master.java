package com.example.process;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Master {
    static final int DEFAULT_COORDINATOR_ID = 1;
    static final String SOCKET_HOST = "localhost";
    private ServerSocket serverSocket = null;

    static final int DEFAULT_TIME_OUT = 2000;

    List<Process> allProcesses = new ArrayList<>();
    Process mainProcess;

    public void init() {

        mainProcess = new Process(DEFAULT_COORDINATOR_ID);

        String coordinator_response = sendMessage(mainProcess, "new,", 4000);

        if (coordinator_response == null) {
            mainProcess.setCoordinator(true);
            startListen();
        } else {
            handleReceivedMessages(coordinator_response.split(","));
//            mainProcess.setPid(Integer.parseInt(coordinator_response));
            startListen();
        }

    }


    String[] parseMessage(String message) {
        return message.split(",");
    }

    public void startListen() {
        while (mainProcess.isActive()) {
            if (mainProcess.isCoordinator()) {
                if (serverSocket == null || serverSocket.isClosed())
                    initiateServerSocket();
                receiveMessage(1000);
                sendAlive();
            } else {
                if (serverSocket == null || serverSocket.isClosed())
                    initiateServerSocket();

                receiveMessage(5000);
            }
        }

        System.out.println(mainProcess.getPid() + " no coordinator: " + (mainProcess.isCoordinator() ? "Y" : "N"));
        while (mainProcess.isElecting()) {
            if (!mainProcess.isCoordinator()) {
                startElection();
                receiveMessage(2000);
            }
        }
    }


    void updateProcessList(int pid) {
        Process new_process = new Process(pid);
        allProcesses.add(new_process);
    }

    Process addNewProcess() {
        Process new_process;
        if (allProcesses.size() == 0) {
            new_process = new Process(DEFAULT_COORDINATOR_ID + 1);
            allProcesses.add(new_process);
        } else {
            int size = allProcesses.size();
            new_process = new Process(allProcesses.get(size - 1).getPid() + 1);
            allProcesses.add(new_process);
        }

        return new_process;
    }

    void notifyOthersWithNewProcess() {
        for (int i = 0; i < allProcesses.size() - 1; i++) {
            sendMessage(allProcesses.get(i), "add," + allProcesses.get(allProcesses.size() - 1).getPid(), 1000);
        }
    }

    void initiateServerSocket() {
        System.out.println("Listening to " + mainProcess.getPid());
        try {
            serverSocket = new ServerSocket(mainProcess.getPid());
        } catch (Exception e) {
            System.out.println("can't listen on " + e.getMessage());
        }
    }

    private void sendAlive() {
//        for (Process p : allProcesses) {
//            sendMessage(p, "alive,", 3000);
//        }
        broadcast(mainProcess, "alive,", 200);
    }

    void broadcast(Process mainProcess, String message, int timeOut) {
        for (Process p : allProcesses) {
            System.out.println("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");
            System.out.println("Message: " + message + " i: " + p.getPid() + " Myid: " + mainProcess.getPid());
//            System.out.println("+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-");

//            if (message.equals("election,")) {
//                if (p.getPid() > mainProcess.getPid()) {
//                    Thread t = new Thread(() -> sendMessage(p, message, timeOut));
//                    t.start();
//                }
//            } else {
//            sendMessage(p, message, timeOut);
            String temp[] = message.split(",");
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    String res = sendMessage(p, message, timeOut);
                    if (temp[0].equals("election")) {
                        System.out.println("-----------------------------------");
                        System.out.println("REEEEEEEES: " + res);
                        System.out.println("-----------------------------------");
                    }
                }
            });
            t.start();
//            }
        }
    }

    void startElection() {

//        for (Process p : allProcesses) {
//            Thread t = new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    String res = sendMessage(p, "election," + mainProcess.getPid(), 100);
//                    System.out.println("election result " + res);
//
//                    String[] temp = receiveMessage(200);
//                    System.out.println("election result " + temp[0]);
//
//                }
//            });
//            t.start();
//
//
//        }


        broadcast(mainProcess, "election," + mainProcess.getPid(), 100);
        boolean isCoordinator = true;

//        for (int i = 0; i < allProcesses.size() - 1; ++i) {
//            String[] res = receiveMessage(4000);
//            System.out.println("election result " + res[0]);
//
//            if (res[0].equals("victory")) {
//                isCoordinator = false;
//            } else if (res[0].equals("election")) {
//                System.out.println("election from " + res[1]);
//                if (Integer.parseInt(res[1]) < mainProcess.getPid()) {
//                    isCoordinator = false;
//                }
//            }
//        }
//        if (isCoordinator) {
//            System.out.println("VICTOOOOOOOORYYYYYYYYYYYYYY");
////            int oldID = mainProcess.getPid();
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            mainProcess.setActive(true);
//            mainProcess.setCoordinator(true);
//            mainProcess.setPid(DEFAULT_COORDINATOR_ID);
////            removePeer(COORDINATOR_DEFAULT);/// remove coordinator
//            broadcast(mainProcess, "victory,", 100);
//            startListen();
//        }
////            notifyVictory();
//        startListen();
    }


    public String sendMessage(Process process, String message, int timeOut) {

        try {
            Socket socket = new Socket(SOCKET_HOST, process.getPid());
            System.out.println(new Date() + " Sending message " + message + " to: " + process.getPid());
            socket.setSoTimeout(timeOut);

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF(message);
            String response = inputStream.readUTF();
            System.out.println(new Date() + " Process " + process.getPid() + " responded with: " + response);

            outputStream.flush();
            outputStream.close();
            socket.close();

            return response;
        } catch (IOException e) {
//            System.out.println(new Date() + " Time out in notify coordinator, pid:  " + mainProcess.getPid());

        }

        return null;
    }


    String[] receiveMessage(int timeOut) {
        try {
            if (timeOut > 0)
                serverSocket.setSoTimeout(timeOut);

            Socket socket = serverSocket.accept();

            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            String str = inputStream.readUTF();
            System.out.println(new Date() + " Received message: " + str);
            String[] parsed = parseMessage(str);

            String response = handleReceivedMessages(parsed);
            outputStream.writeUTF(response);

            outputStream.flush();
            inputStream.close();
            outputStream.close();

            return parsed;

        } catch (Exception e) {
            if (!mainProcess.isCoordinator()) {
                mainProcess.setActive(false);
                mainProcess.setElecting(true);
            }
//            System.out.println(new Date() + " Didn't get message, my process: " + mainProcess.getPid());
        }

        return null;
    }

    String processesToString() {
        String processes = "";
        for (Process p : allProcesses) {
            processes += p.getPid() + ",";
        }

        return processes;
    }

    void toProcesses(String[] processIds) {

        for (int i = 1; i <= processIds.length - 1; i++) {
            updateProcessList(Integer.parseInt(processIds[i]));
        }
    }

    void setMyPid() {
        int last = allProcesses.size() - 1;
        mainProcess.setPid(allProcesses.get(last).getPid());
        allProcesses.remove(last);
    }

    String handleReceivedMessages(String[] parsed) {
        switch (parsed[0]) {
            case "new" -> {
                Process p = addNewProcess();
                notifyOthersWithNewProcess();
                return "all," + processesToString();
            }
            case "alive" -> {
                return "ok," + mainProcess.getPid();
            }
            case "add" -> {
                updateProcessList(Integer.parseInt(parsed[1]));
                return "ok," + mainProcess.getPid();
            }
            case "all" -> {
                toProcesses(parsed);
                setMyPid();
                return "ok," + mainProcess.getPid();
            }
            case "election" -> {
                System.out.println("===================================");
                System.out.println("Election from: " + parsed[1]);
                System.out.println("===================================");

                if (mainProcess.getPid() > Integer.parseInt(parsed[1])) {
                    return "ok," + mainProcess.getPid();
                }


            }
            case "ok" -> {
                System.out.println("+++++++++++++++++++++++++++++++++++");
                System.out.println("Ok from: " + parsed[1]);
                System.out.println("+++++++++++++++++++++++++++++++++++");
                return "ok," + mainProcess.getPid();


            }
            case "victory," -> {
                removeVictoryProcess(Integer.parseInt(parsed[1]));
                mainProcess.setActive(true);
                return "ok," + mainProcess.getPid();
            }
        }
        return null;
    }

    void removeVictoryProcess(int removedId) {
        allProcesses.removeIf(p -> p.getPid() == removedId);
    }

}
