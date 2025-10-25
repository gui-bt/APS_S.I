package os.sim;

import java.util.List;

public interface Scheduler {
    void add(PCB pcb);
    PCB next();
    String name();
    int readySize();
    List<PCB> snapshotReady(); // nova: para visualização
}
