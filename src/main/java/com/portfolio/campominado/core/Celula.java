package com.portfolio.campominado.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma célula do tabuleiro.
 *
 * <p>É um fichário simples de três booleanos — minada, aberta, marcada — e da
 * lista de vizinhos. A regra de quem pode abrir e do que acontece ao abrir
 * fica no {@link Tabuleiro}: aqui só há o estado e os vizinhos, que é o que o
 * desenho e a contagem de minas precisam.
 */
public final class Celula {

    private final int linha;
    private final int coluna;

    private boolean minado;
    private boolean aberto;
    private boolean marcado;

    private final List<Celula> vizinhos = new ArrayList<>();

    Celula(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    /** Linha da célula, de cima para baixo. */
    public int getLinha() {
        return linha;
    }

    /** Coluna da célula, da esquerda para a direita. */
    public int getColuna() {
        return coluna;
    }

    /** {@code true} se a célula esconde uma mina. */
    public boolean isMinado() {
        return minado;
    }

    /** {@code true} se a célula foi revelada. */
    public boolean isAberto() {
        return aberto;
    }

    /** {@code true} se o jogador marcou a célula com uma bandeira. */
    public boolean isMarcado() {
        return marcado;
    }

    /** As até oito células ao redor, sem repetição. */
    public List<Celula> getVizinhos() {
        return Collections.unmodifiableList(vizinhos);
    }

    /** Quantas das células vizinhas estão minadas. */
    public int minasNaVizinhanca() {
        int contagem = 0;
        for (Celula vizinho : vizinhos) {
            if (vizinho.isMinado()) {
                contagem++;
            }
        }
        return contagem;
    }

    void adicionarVizinho(Celula vizinho) {
        vizinhos.add(vizinho);
    }

    void minar() {
        minado = true;
    }

    void abrir() {
        aberto = true;
    }

    void alternarMarca() {
        marcado = !marcado;
    }

    /** Volta ao estado zero, para uma nova partida. */
    void reiniciar() {
        minado = false;
        aberto = false;
        marcado = false;
    }
}