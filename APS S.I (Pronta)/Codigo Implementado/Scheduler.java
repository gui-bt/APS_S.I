package os.sim;

import java.util.List;

public interface Scheduler {
    void add(PCB pcb);
    PCB next();
    String name();
    int readySize();
    List<PCB> snapshotReady();
    
    // Novo método para Round-Robin: retorna processo preemptado à fila
    default void preempt(PCB pcb) {
        add(pcb);
    }
}
