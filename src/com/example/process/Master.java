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

        if (!mainProcess.isCoordinator()) {
            startElection();
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


    private void sendAlive() {
        sendToAll(mainProcess, "alive,", 200);
    }

    void sendToAll(Process mainProcess, String message, int timeOut) {
        for (Process p : allProcesses) {

            String[] temp = message.split(",");
            if (temp[0].equals("election")) {
                if (p.getPid() > mainProcess.getPid()) {
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sendMessage(p, message, timeOut);
                        }
                    });
                    t.start();
                }
            } else {
                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sendMessage(p, message, timeOut);
                    }
                });
                t.start();
            }
        }
    }

    void startElection() {

        sendToAll(mainProcess, "election," + mainProcess.getPid(), 500);
        boolean isCoordinator = false;
        String[] res = receiveMessage(1000);

        if (res == null)
            isCoordinator = true;

        if (isCoordinator) {
            System.out.println("VICTOOOOOOOORYYYYYYYYYYYYYY");
            int oldID = mainProcess.getPid();
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mainProcess.setActive(true);
            mainProcess.setCoordinator(true);
            mainProcess.setPid(DEFAULT_COORDINATOR_ID);
            sendToAll(mainProcess, "victory," + oldID, 100);
            startListen();
        }
        startListen();
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

    String[] parseMessage(String message) {
        return message.split(",");
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

    void initiateServerSocket() {
        System.out.println("Listening to " + mainProcess.getPid());
        try {
            serverSocket = new ServerSocket(mainProcess.getPid());
        } catch (Exception e) {
            System.out.println("can't listen on " + e.getMessage());
        }
    }
}
