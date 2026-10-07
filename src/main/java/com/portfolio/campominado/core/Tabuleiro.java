package com.portfolio.campominado.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * O tabuleiro do Campo Minado e as regras da partida.
 *
 * <p>O defeito que o projeto do curso carregava está corrigido aqui na
 * origem: as minas <b>não</b> existem antes do primeiro clique. Elas são
 * sorteadas no primeiro {@link #abrir}, entre os índices que sobram, com a
 * célula clicada fora do sorteio — ou seja, é impossível morrer no primeiro
 * clique. O sorteio usa embaralhamento de candidatos e nunca fica em laço
 * esperando um índice livre.
 */
public final class Tabuleiro {

    /** Momento da partida. */
    public enum Estado {
        /** Nenhuma célula foi aberta; as minas ainda não foram sorteadas. */
        AGUARDANDO,

        /** Em andamento. */
        JOGANDO,

        /** Todas as células sem mina foram abertas. */
        GANHOU,

        /** Uma mina foi aberta. */
        PERDEU
    }

    private final int linhas;
    private final int colunas;
    private final int minas;
    private final Random aleatorio;

    private final List<Celula> celulas = new ArrayList<>();
    private Estado estado = Estado.AGUARDANDO;
    private boolean minasPlantadas;

    /**
     * Cria um tabuleiro com sorteio não determinístico.
     *
     * @param linhas  quantas linhas, positivas
     * @param colunas quantas colunas, positivas
     * @param minas   quantas minas, ao menos 1 e menor que o total de células
     */
    public Tabuleiro(int linhas, int colunas, int minas) {
        this(linhas, colunas, minas, new Random());
    }

    /**
     * Com a fonte de aleatoriedade informada, para testes determinísticos.
     *
     * @param aleatorio fonte de aleatoriedade do sorteio
     */
    Tabuleiro(int linhas, int colunas, int minas, Random aleatorio) {
        if (linhas < 1 || colunas < 1) {
            throw new IllegalArgumentException(
                    "O tabuleiro precisa de ao menos 1x1, veio " + linhas + "x" + colunas);
        }
        if (minas < 1) {
            throw new IllegalArgumentException("O tabuleiro precisa de ao menos 1 mina, veio " + minas);
        }
        if (minas > linhas * colunas - 1) {
            throw new IllegalArgumentException(
                    minas + " minas não cabem em " + linhas + "x" + colunas
                            + " deixando ao menos 1 célula segura");
        }
        this.linhas = linhas;
        this.colunas = colunas;
        this.minas = minas;
        this.aleatorio = aleatorio;
        gerarCelulas();
        associarVizinhos();
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }

    /** Quantas minas a partida exige. */
    public int getMinas() {
        return minas;
    }

    public Estado getEstado() {
        return estado;
    }

    /** A célula na posição informada, sempre dentro da grade. */
    public Celula getCelula(int linha, int coluna) {
        return celulas.get(indice(linha, coluna));
    }

    /** Quantas células já estão reveladas. */
    public int celulasAbertas() {
        int contagem = 0;
        for (Celula celula : celulas) {
            if (celula.isAberto()) {
                contagem++;
            }
        }
        return contagem;
    }

    /** Quantas bandeiras o jogador plantou (útil para o HUD de minas restantes). */
    public int bandeirasUsadas() {
        int contagem = 0;
        for (Celula celula : celulas) {
            if (celula.isMarcado()) {
                contagem++;
            }
        }
        return contagem;
    }

    /**
     * Tenta abrir a célula.
     *
     * <p>No primeiro clique as minas são sorteadas protegendo a célula clicada.
     * Uma célula marcada não abre. Uma célula com zero minas vizinhas abre a
     * região inteira por fila, parando nas bandeiras e na borda de números.
     * Abrir uma mina encerra como {@link Estado#PERDEU} e revela as demais.
     *
     * @return {@code true} se a partida mudou com a abertura
     */
    public boolean abrir(int linha, int coluna) {
        if (estado == Estado.GANHOU || estado == Estado.PERDEU) {
            return false;
        }
        Celula celula = getCelula(linha, coluna);
        if (celula.isAberto() || celula.isMarcado()) {
            return false;
        }
        if (estado == Estado.AGUARDANDO) {
            if (!minasPlantadas) {
                sortearMinas(linha, coluna);
            }
            estado = Estado.JOGANDO;
        }
        if (celula.isMinado()) {
            celula.abrir();
            revelarMinas();
            estado = Estado.PERDEU;
            return true;
        }
        abrirEmCascata(celula);
        if (ganhou()) {
            estado = Estado.GANHOU;
        }
        return true;
    }

    /** Alterna a bandeira de uma célula fechada, se a partida não acabou. */
    public void alternarMarcacao(int linha, int coluna) {
        if (estado == Estado.GANHOU || estado == Estado.PERDEU) {
            return;
        }
        Celula celula = getCelula(linha, coluna);
        if (!celula.isAberto()) {
            celula.alternarMarca();
        }
    }

    /** Apaga minas, marcas e aberturas, e volta a aguardar o primeiro clique. */
    public void reiniciar() {
        for (Celula celula : celulas) {
            celula.reiniciar();
        }
        estado = Estado.AGUARDANDO;
        minasPlantadas = false;
    }

    /**
     * Planta uma mina à mão. Para os testes montarem posições exatas.
     *
     * @param linha  linha da mina
     * @param coluna coluna da mina
     */
    void minar(int linha, int coluna) {
        getCelula(linha, coluna).minar();
    }

    /**
     * Declara as minas à mão como as definitivas.
     *
     * <p>Sem isso, o primeiro {@link #abrir} sortearia minas por cima das que o
     * teste plantou. Para uso exclusivo dos testes.
     */
    void selarMinas() {
        minasPlantadas = true;
    }

    private void gerarCelulas() {
        for (int linha = 0; linha < linhas; linha++) {
            for (int coluna = 0; coluna < colunas; coluna++) {
                celulas.add(new Celula(linha, coluna));
            }
        }
    }

    private void associarVizinhos() {
        for (Celula celula : celulas) {
            int linha = celula.getLinha();
            int coluna = celula.getColuna();
            for (int dl = -1; dl <= 1; dl++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dl == 0 && dc == 0) {
                        continue;
                    }
                    int l = linha + dl;
                    int c = coluna + dc;
                    if (dentro(l, c)) {
                        celula.adicionarVizinho(getCelula(l, c));
                    }
                }
            }
        }
    }

    private boolean dentro(int linha, int coluna) {
        return linha >= 0 && linha < linhas && coluna >= 0 && coluna < colunas;
    }

    private int indice(int linha, int coluna) {
        return linha * colunas + coluna;
    }

    /**
     * Sorteia as minas entre as células que não são a protegida.
     *
     * <p>Candidatos = todos os índices menos o da célula clicada; embaralha e
     * pega os primeiros {@code minas}. Pode embaralhar a lista inteira (no pior
     * caso 480 células) sem nunca entrar em laço esperando um índice livre.
     */
    private void sortearMinas(int protegidaLinha, int protegidaColuna) {
        int protegida = indice(protegidaLinha, protegidaColuna);
        List<Integer> candidatos = new ArrayList<>(celulas.size() - 1);
        for (int i = 0; i < celulas.size(); i++) {
            if (i != protegida) {
                candidatos.add(i);
            }
        }
        Collections.shuffle(candidatos, aleatorio);
        for (int i = 0; i < minas; i++) {
            celulas.get(candidatos.get(i)).minar();
        }
        minasPlantadas = true;
    }

    /** No fim da derrota, revela as minas que ainda estão escondidas. */
    private void revelarMinas() {
        for (Celula celula : celulas) {
            if (celula.isMinado() && !celula.isMarcado()) {
                celula.abrir();
            }
        }
    }

    /**
     * Abre a célula e, se ela estiver na área de zeros, a região inteira.
     *
     * <p>Usa fila em vez de recursão para o pior caso de um campo cheio de
     * zeros (as 480 células da difícil) não correr risco de estouro de pilha.
     */
    private void abrirEmCascata(Celula inicio) {
        Deque<Celula> fila = new ArrayDeque<>();
        fila.add(inicio);
        while (!fila.isEmpty()) {
            Celula celula = fila.poll();
            if (celula.isAberto() || celula.isMarcado() || celula.isMinado()) {
                continue;
            }
            celula.abrir();
            if (celula.minasNaVizinhanca() != 0) {
                continue;
            }
            for (Celula vizinho : celula.getVizinhos()) {
                if (!vizinho.isAberto() && !vizinho.isMarcado() && !vizinho.isMinado()) {
                    fila.add(vizinho);
                }
            }
        }
    }

    private boolean ganhou() {
        for (Celula celula : celulas) {
            if (!celula.isMinado() && !celula.isAberto()) {
                return false;
            }
        }
        return true;
    }
}