package os.sim;

import java.util.*;
import java.io.*;

public class SimuladorSO {
    private static final int CPU_MIN_CPU_BOUND = 80;
    private static final int CPU_MAX_CPU_BOUND = 200;
    private static final int IO_MIN_CPU_BOUND  = 5;
    private static final int IO_MAX_CPU_BOUND  = 40;

    private static final int CPU_MIN_IO_BOUND  = 20;
    private static final int CPU_MAX_IO_BOUND  = 90;
    private static final int IO_MIN_IO_BOUND   = 60;
    private static final int IO_MAX_IO_BOUND   = 200;

    private static final int IO_REQ_INTERVAL_IOBOUND_MIN = 2;
    private static final int IO_REQ_INTERVAL_IOBOUND_MAX = 4;
    private static final int IO_REQ_INTERVAL_CPUBOUND_MIN = 6;
    private static final int IO_REQ_INTERVAL_CPUBOUND_MAX = 12;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Quantidade de processos a simular: ");
        int n = readInt(sc, 1, 10_000);

        System.out.print("Percentual CPU-bound (0-100): ");
        int cpuBoundPct = readInt(sc, 0, 100);
        int ioBoundPct = 100 - cpuBoundPct;
        System.out.println("→ IO-bound definido automaticamente como: " + ioBoundPct + "%");

        System.out.print("Modo passo a passo? (s/N): ");
        boolean stepMode = yes(sc.nextLine());

        int snapEvery = 10;
        if (!stepMode) {
            System.out.print("Intervalo de snapshot em execução contínua (ticks, padrão 10): ");
            String se = sc.nextLine().trim();
            if (!se.isEmpty()) {
                try { snapEvery = Math.max(1, Integer.parseInt(se)); } catch (Exception ignored) {}
            }
        }

        System.out.print("Limpar tela a cada tick (ANSI)? (S/n): ");
        boolean ansi = !no(sc.nextLine());

        System.out.print("Mostrar barras de progresso? (S/n): ");
        boolean bars = !no(sc.nextLine());

        // Opção de log CSV
        System.out.print("Gravar log CSV? (s/N): ");
        boolean logCsv = yes(sc.nextLine());
        String baseName = null;
        PrintWriter ticksCsv = null;
        PrintWriter procCsv = null;
        if (logCsv) {
            System.out.print("Nome-base do CSV (padrão 'simulacao'): ");
            baseName = sc.nextLine().trim();
            if (baseName.isEmpty()) baseName = "simulacao";
            try {
                ticksCsv = new PrintWriter(new BufferedWriter(new FileWriter(baseName + "_ticks.csv")));
                ticksCsv.println("clock,running_pid,ready_size,waiting_size,terminated_size");
                procCsv = new PrintWriter(new BufferedWriter(new FileWriter(baseName + "_process.csv")));
                procCsv.println("clock,pid,state,cpu_done,cpu_total,io_done,io_total");
            } catch (IOException e) {
                System.out.println("Não foi possível abrir arquivos CSV para escrita. Prosseguindo sem log...");
                ticksCsv = null;
                procCsv = null;
                logCsv = false;
            }
        }

        Random rnd = new Random();
        Scheduler scheduler = new FifoScheduler();
        ConsoleView view = new ConsoleView(ansi, bars, 24);

        System.out.println("Escalonador: " + scheduler.name());

        List<PCB> all = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            boolean ioBound = rnd.nextInt(100) < ioBoundPct;
            int totalCpu = ioBound ? randIn(rnd, CPU_MIN_IO_BOUND, CPU_MAX_IO_BOUND)
                                   : randIn(rnd, CPU_MIN_CPU_BOUND, CPU_MAX_CPU_BOUND);
            int totalIo  = ioBound ? randIn(rnd, IO_MIN_IO_BOUND, IO_MAX_IO_BOUND)
                                   : randIn(rnd, IO_MIN_CPU_BOUND, IO_MAX_CPU_BOUND);
            int ioInterval = ioBound ? randIn(rnd, IO_REQ_INTERVAL_IOBOUND_MIN, IO_REQ_INTERVAL_IOBOUND_MAX)
                                     : randIn(rnd, IO_REQ_INTERVAL_CPUBOUND_MIN, IO_REQ_INTERVAL_CPUBOUND_MAX);
            PCB p = new PCB(ioBound, totalCpu, totalIo, ioInterval);
            p.setState(ProcessState.NEW);
            all.add(p);
        }

        List<PCB> waiting = new ArrayList<>();
        List<PCB> terminated = new ArrayList<>();
        int clock = 0;
        PCB running = null;

        for (PCB p : all) scheduler.add(p);

        boolean auto = !stepMode;
        int stepBudget = 0;

        while (terminated.size() < all.size()) {
            clock++;

            for (int i = 0; i < waiting.size();) {
                PCB p = waiting.get(i);
                p.onIoTick();
                if (p.remainingIo <= 0) {
                    if (p.remainingCpu <= 0) {
                        p.setState(ProcessState.TERMINATED);
                        p.setFinish(clock);
                        terminated.add(p);
                    } else {
                        scheduler.add(p);
                    }
                    waiting.remove(i);
                } else i++;
            }

            if (running == null) running = scheduler.next();

            if (running != null) {
                running.onCpuTick(clock);
                boolean moveToWait = false;

                if (running.remainingCpu <= 0) {
                    if (running.remainingIo > 0) moveToWait = true;
                    else {
                        running.setState(ProcessState.TERMINATED);
                        running.setFinish(clock);
                        terminated.add(running);
                        running = null;
                    }
                } else if (running.shouldRequestIo()) moveToWait = true;

                if (moveToWait) {
                    running.setState(ProcessState.WAITING);
                    running.resetIoCounter();
                    waiting.add(running);
                    running = null;
                }
            }

            // Logging CSV por tick e por processo
            if (logCsv) {
                int runningPid = (running == null) ? -1 : running.pid;
                if (ticksCsv != null) {
                    ticksCsv.printf(Locale.US, "%d,%d,%d,%d,%d%n", clock, runningPid,
                            scheduler.readySize(), waiting.size(), terminated.size());
                }
                if (procCsv != null) {
                    // READY
                    for (PCB p : scheduler.snapshotReady()) {
                        procCsv.println(procLineCsv(clock, p));
                    }
                    // WAITING
                    for (PCB p : waiting) {
                        procCsv.println(procLineCsv(clock, p));
                    }
                    // RUNNING
                    if (running != null) {
                        procCsv.println(procLineCsv(clock, running));
                    }
                    // TERMINATED (apenas o último bloco, custo O(n) mas aceitável para ensino)
                    for (PCB p : terminated) {
                        procCsv.println(procLineCsv(clock, p));
                    }
                }
            }

            boolean shouldRender = stepMode || (clock % snapEvery == 0) || clock == 1;
            if (shouldRender) {
                view.renderTick(clock, running, scheduler.snapshotReady(), waiting, terminated, scheduler.name());
                if (stepMode) {
                    String cmd = prompt(sc, "> ");
                    if (cmd == null) break;
                    cmd = cmd.trim();
                    if (cmd.isEmpty()) stepBudget = 1;
                    else if (cmd.equalsIgnoreCase("a")) { auto = true; stepMode = false; }
                    else if (cmd.equalsIgnoreCase("q")) break;
                    else if (cmd.matches("n\\s+\\d+")) {
                        try { stepBudget = Integer.parseInt(cmd.split("\\s+")[1]); } catch (Exception ignored) { stepBudget = 1; }
                    } else if (cmd.matches("\\d+")) {
                        try { stepBudget = Integer.parseInt(cmd); } catch (Exception ignored) { stepBudget = 1; }
                    } else {
                        System.out.println("Comando inválido. Use: [Enter], n <N>, a, q");
                        stepBudget = 0;
                    }
                }
            }

            if (stepMode) {
                if (stepBudget > 0) stepBudget--;
                if (stepBudget == 0) continue;
            }
        }

        int totalTime = clock;
        int finished = terminated.size();
        double throughput = (totalTime == 0) ? 0 : (finished / (double) totalTime);
        double avgTurnaround = terminated.stream().mapToInt(PCB::turnaround).average().orElse(0.0);

        System.out.println("\n==== Estatísticas ====");
        System.out.println("Processos executados: " + finished + "/" + all.size());
        System.out.println("Tempo total de simulação (ticks): " + totalTime);
        System.out.printf(Locale.US, "Throughput (proc/tick): %.6f%n", throughput);
        System.out.printf(Locale.US, "Tempo médio de turnaround (ticks): %.2f%n", avgTurnaround);

        long cpuType = terminated.stream().filter(p -> !p.ioBound).count();
        long ioType  = terminated.stream().filter(p ->  p.ioBound).count();
        System.out.println("Distribuição: CPU-bound=" + cpuType + ", IO-bound=" + ioType);

        // Fecha CSVs
        if (ticksCsv != null) ticksCsv.close();
        if (procCsv != null) procCsv.close();
        if (logCsv) {
            System.out.println("Logs gravados em: " + baseName + "_ticks.csv e " + baseName + "_process.csv");
        }
    }

    private static String procLineCsv(int clock, PCB p) {
        int cpuDone = p.totalCpuTime - p.remainingCpu;
        int ioDone  = p.totalIoTime - p.remainingIo;
        return String.format(Locale.US, "%d,%d,%s,%d,%d,%d,%d",
                clock, p.pid, p.getState().name(), cpuDone, p.totalCpuTime, ioDone, p.totalIoTime);
    }

    private static int randIn(Random r, int min, int max) {
        return min + r.nextInt(Math.max(1, (max - min + 1)));
    }

    private static int readInt(Scanner sc, int min, int max) {
        while (true) {
            String s = sc.nextLine().trim();
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) throw new NumberFormatException();
                return v;
            } catch (Exception e) {
                System.out.print("Inválido. Digite um inteiro entre " + min + " e " + max + ": ");
            }
        }
    }

    private static boolean yes(String s) {
        s = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        return s.equals("s") || s.equals("sim") || s.equals("y") || s.equals("yes");
    }
    private static boolean no(String s) {
        s = s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
        return s.equals("n") || s.equals("nao") || s.equals("não") || s.equals("no");
    }
    private static String prompt(Scanner sc, String label) {
        System.out.print(label);
        if (sc.hasNextLine()) return sc.nextLine();
        return null;
    }
}
