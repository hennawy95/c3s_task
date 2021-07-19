package com.example.process;

public class Process {
    private int pid;
    private boolean isCoordinator;
    private boolean isActive;
    private boolean isElecting;

    public Process(int pid) {
        this.pid = pid;
        this.isCoordinator = false;
        this.isActive = true;
        this.isElecting = false;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public boolean isCoordinator() {
        return isCoordinator;
    }

    public void setCoordinator(boolean coordinator) {
        isCoordinator = coordinator;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isElecting() {
        return isElecting;
    }

    public void setElecting(boolean electing) {
        isElecting = electing;
    }
}
