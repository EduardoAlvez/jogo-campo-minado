package com.portfolio.campominado.core;

/**
 * As dificuldades do Campo Minado.
 *
 * <p>São as três clássicas: a grade cresce junto com o número de minas. A
 * contagem precisa caber no tabuleiro deixando ao menos uma célula livre — o
 * {@link Tabuleiro} valida isso na construção.
 */
public enum Dificuldade {

    /** 9 linhas, 9 colunas, 10 minas. A partida rápida. */
    FACIL(9, 9, 10, "Fácil"),

    /** 16 linhas, 16 colunas, 40 minas. O equilíbrio clássico. */
    MEDIO(16, 16, 40, "Médio"),

    /** 16 linhas, 30 colunas, 99 minas. A partida longa. */
    DIFICIL(16, 30, 99, "Difícil");

    private final int linhas;
    private final int colunas;
    private final int minas;
    private final String rotulo;

    Dificuldade(int linhas, int colunas, int minas, String rotulo) {
        this.linhas = linhas;
        this.colunas = colunas;
        this.minas = minas;
        this.rotulo = rotulo;
    }

    /** Quantidade de linhas do tabuleiro. */
    public int getLinhas() {
        return linhas;
    }

    /** Quantidade de colunas do tabuleiro. */
    public int getColunas() {
        return colunas;
    }

    /** Quantidade de minas espalhadas pelo tabuleiro. */
    public int getMinas() {
        return minas;
    }

    /** Nome exibido no menu, com acento. */
    public String getRotulo() {
        return rotulo;
    }

    /** Total de células do tabuleiro. */
    public int totalCelulas() {
        return linhas * colunas;
    }
}