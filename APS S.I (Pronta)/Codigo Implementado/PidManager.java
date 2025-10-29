package os.sim;

import java.util.ArrayDeque;
import java.util.Queue;

public class PidManager {
    private int nextPid = 1;
    private final Queue<Integer> free = new ArrayDeque<>();

    public int alloc() {
        return free.isEmpty() ? nextPid++ : free.poll();
    }

    public void free(int pid) { free.offer(pid); }
}
