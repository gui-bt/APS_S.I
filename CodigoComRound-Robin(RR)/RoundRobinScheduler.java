package os.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class RoundRobinScheduler implements Scheduler {
    private final Queue<PCB> ready = new ArrayDeque<>();
    private final int quantum;
    private int ticksUsed = 0;

    public RoundRobinScheduler(int quantum) {
        this.quantum = Math.max(1, quantum);
    }

    @Override
    public void add(PCB pcb) {
        pcb.setState(ProcessState.READY);
        ready.offer(pcb);
    }

    @Override
    public PCB next() {
        PCB p = ready.poll();
        if (p != null) {
            p.setState(ProcessState.RUNNING);
            ticksUsed = 0; // Reseta contador quando novo processo entra
        }
        return p;
    }

    @Override
    public void preempt(PCB pcb) {
        pcb.setState(ProcessState.READY);
        ready.offer(pcb);
        ticksUsed = 0;
    }

    @Override
    public String name() { 
        return "Round-Robin (q=" + quantum + ")"; 
    }

    @Override
    public int readySize() { 
        return ready.size(); 
    }

    @Override
    public List<PCB> snapshotReady() {
        return new ArrayList<>(ready);
    }

    // Método auxiliar para verificar se quantum expirou
    public boolean shouldPreempt() {
        ticksUsed++;
        return ticksUsed >= quantum;
    }

    public int getQuantum() {
        return quantum;
    }

    public int getTicksUsed() {
        return ticksUsed;
    }
}
