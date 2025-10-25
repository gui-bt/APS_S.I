package os.sim;

import java.util.concurrent.atomic.AtomicInteger;

public class PCB {
    private static final AtomicInteger SEQ = new AtomicInteger();

    public final int pid;
    public final boolean ioBound;
    public final int totalCpuTime;
    public final int totalIoTime;
    public final int ioRequestInterval;

    int remainingCpu;
    int remainingIo;
    int ticksSinceLastIo = 0;

    ProcessState state = ProcessState.NEW;

    int arrivalTime = 0;
    Integer startTime = null;
    Integer finishTime = null;

    public PCB(boolean ioBound, int totalCpuTime, int totalIoTime, int ioRequestInterval) {
        this.pid = SEQ.incrementAndGet();
        this.ioBound = ioBound;
        this.totalCpuTime = totalCpuTime;
        this.totalIoTime = totalIoTime;
        this.ioRequestInterval = Math.max(1, ioRequestInterval);
        this.remainingCpu = totalCpuTime;
        this.remainingIo = totalIoTime;
    }

    public ProcessState getState() { return state; }
    public void setState(ProcessState st) { this.state = st; }
    public boolean isFinished() { return remainingCpu <= 0 && remainingIo <= 0; }

    public void onCpuTick(int now) {
        if (startTime == null) startTime = now;
        if (remainingCpu > 0) {
            remainingCpu--;
            ticksSinceLastIo++;
        }
    }

    public boolean shouldRequestIo() {
        return remainingIo > 0 && ticksSinceLastIo >= ioRequestInterval;
    }

    public void resetIoCounter() { ticksSinceLastIo = 0; }
    public void onIoTick() { if (remainingIo > 0) remainingIo--; }
    public void setFinish(int now) { this.finishTime = now; }
    public int turnaround() { return (finishTime == null ? 0 : finishTime) - arrivalTime; }

    @Override
    public String toString() {
        return String.format("PID=%d[%s] CPU:%d/%d IO:%d/%d", pid,
                ioBound ? "IO" : "CPU",
                (totalCpuTime - remainingCpu), totalCpuTime,
                (totalIoTime - remainingIo), totalIoTime);
    }
}
