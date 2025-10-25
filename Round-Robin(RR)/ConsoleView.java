package os.sim;

import java.util.*;
import java.util.stream.Collectors;

public class ConsoleView {
    private final boolean useAnsiClear;
    private final boolean showBars;
    private final int barWidth;

    public ConsoleView(boolean useAnsiClear, boolean showBars, int barWidth) {
        this.useAnsiClear = useAnsiClear;
        this.showBars = showBars;
        this.barWidth = Math.max(10, barWidth);
    }

    private void clear() {
        if (useAnsiClear) {
            System.out.print("\033[H\033[2J");
            System.out.flush();
        }
    }

    private String bar(int done, int total) {
        if (!showBars || total <= 0) return "";
        int filled = Math.min(barWidth, (int)Math.round((done * 1.0 / total) * barWidth));
        int empty = barWidth - filled;
        StringBuilder sb = new StringBuilder(" [");
        for (int i = 0; i < filled; i++) sb.append('#');
        for (int i = 0; i < empty; i++) sb.append('-');
        sb.append(']');
        return sb.toString();
    }

    private String procLine(PCB p) {
        int cpuDone = p.totalCpuTime - p.remainingCpu;
        int ioDone  = p.totalIoTime - p.remainingIo;
        return String.format("PID=%3d %-3s CPU %3d/%-3d IO %3d/%-3d%s",
                p.pid, (p.ioBound ? "IO" : "CPU"),
                cpuDone, p.totalCpuTime,
                ioDone, p.totalIoTime,
                bar(cpuDone, p.totalCpuTime));
    }

    public void renderTick(int clock, PCB running, List<PCB> ready, List<PCB> waiting, List<PCB> terminated,
                           String schedulerName) {
        clear();
        System.out.printf("t=%d  | Escalonador=%s%n", clock, schedulerName);
        System.out.println("────────────────────────────────────────────────────────────────");
        // RUNNING
        if (running != null) {
            System.out.println("RUNNING:");
            System.out.println("  " + procLine(running));
        } else {
            System.out.println("RUNNING: (vazio)");
        }
        System.out.println();

        // READY
        System.out.println("READY (" + ready.size() + "):");
        if (ready.isEmpty()) System.out.println("  (fila vazia)");
        else {
            int idx = 0;
            for (PCB p : ready) {
                System.out.printf("  %2d) %s%n", ++idx, procLine(p));
            }
        }
        System.out.println();

        // WAITING
        System.out.println("WAITING (" + waiting.size() + "):");
        if (waiting.isEmpty()) System.out.println("  (sem processos aguardando IO)");
        else {
            for (PCB p : waiting) System.out.println("  - " + procLine(p));
        }
        System.out.println();

        // TERMINATED
        System.out.println("TERMINATED (" + terminated.size() + "):");
        if (!terminated.isEmpty()) {
            // mostra últimos 5
            List<PCB> tail = terminated.size() > 5 ? terminated.subList(terminated.size()-5, terminated.size()) : terminated;
            for (PCB p : tail) System.out.println("  ✓ " + procLine(p));
            if (terminated.size() > 5) System.out.println("  ...");
        } else System.out.println("  (nenhum ainda)");
        System.out.println("────────────────────────────────────────────────────────────────");
        System.out.println("Controles: [Enter]=próximo tick | n <N>=avançar N | a=auto | q=sair");
    }
}
