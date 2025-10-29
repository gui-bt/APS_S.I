package os.sim;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class FifoScheduler implements Scheduler {
    private final Queue<PCB> ready = new ArrayDeque<>();

    @Override
    public void add(PCB pcb) {
        pcb.setState(ProcessState.READY);
        ready.offer(pcb);
    }

    @Override
    public PCB next() {
        PCB p = ready.poll();
        if (p != null) p.setState(ProcessState.RUNNING);
        return p;
    }

    @Override
    public String name() { return "FIFO"; }
    
    @Override
    public int readySize() { return ready.size(); }

    @Override
    public List<PCB> snapshotReady() {
        return new ArrayList<>(ready);
    }
}
