package com.portfolio.campominado.ui;

import com.portfolio.campominado.core.Dificuldade;

import java.awt.Rectangle;

/**
 * Onde cada célula do campo é desenhada, calculado sem Swing e sem abrir janela.
 *
 * <p>Esta classe existe pelo mesmo motivo da {@code LayoutSnake} do trio: nada
 * de posição é calculada dentro do método de pintura. A {@link TelaCampoMinado}
 * pergunta a esta classe onde cada célula fica — inclusive qual célula contém
 * um ponto do mouse — e a resposta é um {@link Rectangle} que um teste sem
 * display consegue conferir. Um clique errando a célula só volta como bug se a
 * conta morar na tela; aqui um teste acerta a origem e a ponta de cada célula.
 *
 * <p>A janela nasce do campo: a célula tem tamanho fixo e a janela cresce em
 * múltiplos inteiros de célula ({@code celula * colunas}), em vez de a célula
 * sair de uma janela fixa dividida por colunas. Arredondar por célula é o que
 * garante a grade exata, sem fresta entre células vizinhas.
 */
public final class LayoutCampoMinado {

    private static final int CELULA_PADRAO = 30;
    private static final int MARGEM_PADRAO = 12;
    private static final int ALTURA_HUD_PADRAO = 64;

    private final int larguraJanela;
    private final int alturaJanela;
    private final int linhas;
    private final int colunas;
    private final int margem;
    private final int alturaHud;

    private final int celula;
    private final int larguraTabuleiro;
    private final int alturaTabuleiro;
    private final int tabuleiroX;
    private final int tabuleiroY;

    /** Cria o layout que esta dificuldade pede, com célula de 30 px. */
    public LayoutCampoMinado(Dificuldade dificuldade) {
        this(dificuldade.getLinhas(), dificuldade.getColunas(), CELULA_PADRAO,
                MARGEM_PADRAO, ALTURA_HUD_PADRAO);
    }

    /**
     * Cria um layout com as medidas dadas.
     *
     * @param linhas   células na vertical
     * @param colunas  células na horizontal
     * @param celula   o lado de cada célula em pixels
     * @param margem   respiro entre a janela e o conteúdo
     * @param alturaHud altura da faixa de status no topo
     */
    public LayoutCampoMinado(int linhas, int colunas, int celula, int margem, int alturaHud) {
        if (linhas < 1 || colunas < 1) {
            throw new IllegalArgumentException("campo pequeno demais: "
                    + linhas + "x" + colunas);
        }
        if (celula < 8) {
            throw new IllegalArgumentException("celula pequena demais: " + celula + "px");
        }
        if (margem < 4 || alturaHud < 24) {
            throw new IllegalArgumentException("area de respiro pequena demais: margem "
                    + margem + ", hud " + alturaHud);
        }
        this.linhas = linhas;
        this.colunas = colunas;
        this.celula = celula;
        this.margem = margem;
        this.alturaHud = alturaHud;

        // A janela nasce do campo: célula inteira * quantidade, mais os respiros.
        this.larguraTabuleiro = celula * colunas;
        this.alturaTabuleiro = celula * linhas;
        this.larguraJanela = larguraTabuleiro + 2 * margem;
        this.alturaJanela = alturaTabuleiro + 2 * margem + alturaHud;
        this.tabuleiroX = (larguraJanela - larguraTabuleiro) / 2;
        this.tabuleiroY = margem + alturaHud;
    }

    // ------------------------------------------------------------------
    // As áreas
    // ------------------------------------------------------------------

    /** A janela inteira, a fronteira que nada pode atravessar. */
    public Rectangle janela() {
        return new Rectangle(0, 0, larguraJanela, alturaJanela);
    }

    /** A faixa de status no topo: minas restantes, tempo e dificuldade. */
    public Rectangle hud() {
        return new Rectangle(margem, margem, larguraJanela - 2 * margem, alturaHud);
    }

    /** A área jogável, com as células. */
    public Rectangle tabuleiro() {
        return new Rectangle(tabuleiroX, tabuleiroY, larguraTabuleiro, alturaTabuleiro);
    }

    /** A área de uma célula da grade. */
    public Rectangle celula(int linha, int coluna) {
        return new Rectangle(tabuleiroX + coluna * celula, tabuleiroY + linha * celula,
                celula, celula);
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /**
     * Qual célula contém o ponto, na ordem linha × colunas.
     *
     * @param x coordenada horizontal do ponto
     * @param y coordenada vertical do ponto
     * @return o índice da célula, ou {@code -1} se o ponto está fora do campo
     */
    public int indiceDoPonto(int x, int y) {
        if (!tabuleiro().contains(x, y)) {
            return -1;
        }
        int coluna = (x - tabuleiroX) / celula;
        int linha = (y - tabuleiroY) / celula;
        return linha * colunas + coluna;
    }

    /** A célula de um índice, na ordem linha × colunas. */
    public Rectangle celulaDoIndice(int indice) {
        if (indice < 0 || indice >= linhas * colunas) {
            return null;
        }
        return celula(indice / colunas, indice % colunas);
    }

    /**
     * Um retângulo está inteiramente dentro da janela?
     *
     * <p>Público para o teste perguntar em vez de reescrever a conta: uma
     * célula saindo do campo é o defeito do Pong repetido, e ele não pode
     * depender do gráfico de quem confere.
     */
    public boolean dentroDaJanela(Rectangle r) {
        return r.x >= 0 && r.y >= 0
                && r.x + r.width <= larguraJanela
                && r.y + r.height <= alturaJanela;
    }

    public int getLarguraJanela() {
        return larguraJanela;
    }

    public int getAlturaJanela() {
        return alturaJanela;
    }

    public int getLinhas() {
        return linhas;
    }

    public int getColunas() {
        return colunas;
    }

    public int getCelula() {
        return celula;
    }
}