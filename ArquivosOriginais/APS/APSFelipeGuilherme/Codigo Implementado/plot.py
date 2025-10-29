#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
import glob
import pandas as pd
import matplotlib.pyplot as plt


def find_csvs(base: str | None):
    """
    Localiza arquivos CSV. Se base for fornecido, usa <base>_ticks.csv e <base>_process.csv.
    Caso contrário, tenta detectar automaticamente o par mais recente no diretório atual.
    """
    if base:
        ticks = f"{base}_ticks.csv"
        proc = f"{base}_process.csv"
        if not os.path.exists(ticks):
            raise FileNotFoundError(f"Arquivo não encontrado: {ticks}")
        if not os.path.exists(proc):
            print(f"Aviso: {proc} não encontrado. Alguns gráficos serão pulados.")
            proc = None
        return ticks, proc
    # auto
    candidates = sorted(glob.glob("*_ticks.csv"), key=os.path.getmtime, reverse=True)
    if not candidates:
        raise FileNotFoundError("Nenhum *_ticks.csv encontrado no diretório atual.")
    ticks = candidates[0]
    base = ticks[:-10]  # remove '_ticks.csv'
    proc = base + "_process.csv"
    if not os.path.exists(proc):
        proc = None
    return ticks, proc


def plot_series(x, y, title, ylabel, outpng):
    plt.figure()
    plt.plot(x, y)
    plt.title(title)
    plt.xlabel("clock (ticks)")
    plt.ylabel(ylabel)
    plt.tight_layout()
    plt.savefig(outpng, dpi=120)
    plt.close()


def main():
    """
    Uso:
        python plot.py [NOME_BASE]
    Exemplos:
        python plot.py simulacao
        python plot.py             # auto-detecta o *_ticks.csv mais recente
    Saídas (PNG) no diretório atual.
    """
    base = sys.argv[1] if len(sys.argv) > 1 else None
    ticks_csv, proc_csv = find_csvs(base)

    print(f"Lendo: {ticks_csv}")
    ticks = pd.read_csv(ticks_csv)
    required_cols = {"clock", "running_pid", "ready_size", "waiting_size", "terminated_size"}
    if not required_cols.issubset(set(ticks.columns)):
        raise ValueError(f"Colunas esperadas em {ticks_csv}: {required_cols}")

    # Gráficos básicos por métrica
    plot_series(ticks["clock"], ticks["ready_size"], "Fila READY ao longo do tempo", "READY (processos)", "ready_size.png")
    plot_series(ticks["clock"], ticks["waiting_size"], "Fila WAITING ao longo do tempo", "WAITING (processos)", "waiting_size.png")
    plot_series(ticks["clock"], ticks["terminated_size"], "Processos TERMINATED ao longo do tempo", "TERMINATED (processos)", "terminated_size.png")

    # Utilização de CPU (1 se há processo rodando, 0 se ocioso)
    cpu_util = (ticks["running_pid"] != -1).astype(int)
    plot_series(ticks["clock"], cpu_util, "Utilização de CPU (binária)", "CPU em uso (0/1)", "cpu_utilization.png")

    # Throughput cumulativo = terminados / clock
    throughput = ticks["terminated_size"] / ticks["clock"].replace(0, pd.NA)
    throughput = throughput.fillna(0)
    plot_series(ticks["clock"], throughput, "Throughput acumulado", "proc/tick", "throughput_cumulativo.png")

    # Se houver o process.csv, mostra progresso médio de CPU por tick
    if proc_csv and os.path.exists(proc_csv):
        print(f"Lendo: {proc_csv}")
        proc = pd.read_csv(proc_csv)
        if {"clock", "cpu_done", "cpu_total"}.issubset(proc.columns):
            proc["cpu_ratio"] = proc["cpu_done"] / proc["cpu_total"].replace(0, pd.NA)
            cpu_ratio_by_tick = proc.groupby("clock")["cpu_ratio"].mean().fillna(0)
            plot_series(cpu_ratio_by_tick.index, cpu_ratio_by_tick.values,
                        "Progresso médio de CPU por tick", "cpu_done/total (média)", "cpu_ratio_medio.png")

    print("Gerados: ready_size.png, waiting_size.png, terminated_size.png, cpu_utilization.png, throughput_cumulativo.png" +
          (", cpu_ratio_medio.png" if (proc_csv and os.path.exists(proc_csv)) else ""))


if __name__ == "__main__":
    main()
