# Simulador de Gerência do Processador (Java)

Simula a gerência de processos (NEW, READY, RUNNING, WAITING, TERMINATED) com **visualização passo a passo**, **exportação CSV** e **gráficos**.

## Recursos
- Informe apenas o percentual de **CPU‑bound** (IO‑bound = 100 − CPU‑bound).
- **Passo a passo** (interativo): `[Enter]`, `n <N>`, `a`, `q`.
- **Barras de progresso** (opcional) e *clear* de tela (ANSI) por tick.
- **Logs CSV (opcional):**
  - `<base>_ticks.csv`: `clock,running_pid,ready_size,waiting_size,terminated_size`
  - `<base>_process.csv`: `clock,pid,state,cpu_done,cpu_total,io_done,io_total`
- **Gráficos (plot.py)** a partir dos CSVs.

## Estrutura
```
src/os/sim/
├── ProcessState.java
├── PCB.java
├── Scheduler.java          # expõe snapshotReady()
├── FifoScheduler.java
├── ConsoleView.java        # visualização no console
└── SimuladorSO.java        # sem seed e com CSV opcional
plot.py                     # gera gráficos a partir dos CSVs
```

## Compilação
```bash
javac -d out $(find src -name "*.java")
```

## Execução da simulação
```bash
java -cp out os.sim.SimuladorSO
```
Responda `s` para **Gravar log CSV** e forneça um **nome-base** (ex.: `simulacao`).

## Geração de gráficos
Pré‑requisitos: Python 3, `pandas`, `matplotlib`.
```bash
pip install pandas matplotlib
```
Uso:
```bash
python plot.py               # auto-detecta o *_ticks.csv mais recente no diretório
python plot.py simulacao     # usa simulacao_ticks.csv e simulacao_process.csv
```
Saídas (PNG):
- `ready_size.png`
- `waiting_size.png`
- `terminated_size.png`
- `cpu_utilization.png`
- `throughput_cumulativo.png`
- `cpu_ratio_medio.png` (se `<base>_process.csv` existir)

## Estatísticas ao final da simulação
- Processos executados
- Tempo total de simulação (ticks)
- Throughput (proc/tick)
- Tempo médio de turnaround
- Distribuição CPU‑bound vs IO‑bound

> Observação: em terminais sem suporte ANSI, escolha *não* limpar a tela.
