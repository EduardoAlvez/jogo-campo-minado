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
    // Menu e fim de partida
    // ------------------------------------------------------------------

    /** O que um clique sobre o menu ou o fim pode acertar. */
    public enum Alvo {
        FACIL, MEDIO, DIFICIL, COMECAR, JOGAR_DE_NOVO, VOLTAR_AO_MENU
    }

    /**
     * A geometria de uma tela com alvos clicáveis.
     *
     * <p>O menu e o fim são duas telas diferentes com a mesma pergunta: "onde
     * mora o alvo {dificuldade, começar, jogar de novo, voltar}?". Uma
     * interface para essa pergunta é o que permite um só algoritmo de
     * hit-test — {@link #alvoEm} — servir as duas.
     */
    public interface Alvos {

        /**
         * O retângulo do alvo, ou {@code null} se esta tela não tem esse alvo.
         */
        Rectangle retangulo(Alvo alvo);
    }

    /**
     * O menu principal: a escolha de dificuldade e o começar.
     *
     * <p>Vive inteiro dentro do menor campo (o Fácil), porque é desenhado por
     * cima dele — os botões não sobram nem em 294 px de largura.
     */
    public static final class Menu implements Alvos {

        private final Rectangle janela;
        private final Rectangle painel;
        private final Rectangle titulo;
        private final Rectangle rotulo;
        private final Rectangle dica;
        private final Rectangle[] alvos;

        Menu(Rectangle janela, Rectangle painel, Rectangle titulo, Rectangle rotulo,
                Rectangle dica, Rectangle[] alvos) {
            this.janela = janela;
            this.painel = painel;
            this.titulo = titulo;
            this.rotulo = rotulo;
            this.dica = dica;
            this.alvos = alvos;
        }

        /** A janela inteira, para o pincel limpar tudo antes do painel. */
        public Rectangle janela() {
            return janela;
        }

        public Rectangle painel() {
            return painel;
        }

        public Rectangle titulo() {
            return titulo;
        }

        public Rectangle rotulo() {
            return rotulo;
        }

        public Rectangle dica() {
            return dica;
        }

        public Rectangle facil() {
            return retangulo(Alvo.FACIL);
        }

        public Rectangle medio() {
            return retangulo(Alvo.MEDIO);
        }

        public Rectangle dificil() {
            return retangulo(Alvo.DIFICIL);
        }

        public Rectangle comecar() {
            return retangulo(Alvo.COMECAR);
        }

        @Override
        public Rectangle retangulo(Alvo alvo) {
            if (alvo == null) {
                return null;
            }
            int i = alvo.ordinal();
            return i < alvos.length ? alvos[i] : null;
        }
    }

    /**
     * A tela de fim: o resultado, o tempo, o recorde e as duas saídas.
     */
    public static final class Fim implements Alvos {

        private final Rectangle janela;
        private final Rectangle painel;
        private final Rectangle titulo;
        private final Rectangle subtitulo;
        private final Rectangle recorde;
        private final Rectangle dica;
        private final Rectangle[] alvos;

        Fim(Rectangle janela, Rectangle painel, Rectangle titulo, Rectangle subtitulo,
                Rectangle recorde, Rectangle dica, Rectangle[] alvos) {
            this.janela = janela;
            this.painel = painel;
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.recorde = recorde;
            this.dica = dica;
            this.alvos = alvos;
        }

        /** A janela inteira, para o pincel poder repintar o fundo. */
        public Rectangle janela() {
            return janela;
        }

        public Rectangle painel() {
            return painel;
        }

        public Rectangle titulo() {
            return titulo;
        }

        public Rectangle subtitulo() {
            return subtitulo;
        }

        /** A faixa do recorde, que só tem tinta quando houve um novo. */
        public Rectangle recorde() {
            return recorde;
        }

        public Rectangle dica() {
            return dica;
        }

        public Rectangle jogarDeNovo() {
            return retangulo(Alvo.JOGAR_DE_NOVO);
        }

        public Rectangle voltarAoMenu() {
            return retangulo(Alvo.VOLTAR_AO_MENU);
        }

        @Override
        public Rectangle retangulo(Alvo alvo) {
            if (alvo == null) {
                return null;
            }
            int i = alvo.ordinal();
            return i < alvos.length ? alvos[i] : null;
        }
    }

    /** O menu, cabe nas três larguras de janela (a menor é o Fácil). */
    public Menu menu() {
        int pw = larguraJanela - 2 * margem;
        int ph = 240;
        int px = margem;
        int py = Math.max(margem, (alturaJanela - ph) / 2);

        Rectangle painel = new Rectangle(px, py, pw, ph);
        Rectangle titulo = new Rectangle(px, py + 14, pw, 36);
        Rectangle rotulo = new Rectangle(px + 12, py + 64, pw - 24, 24);

        int btnW = 80;
        int btnH = 28;
        int gap = 10;
        int total = 3 * btnW + 2 * gap;
        int bx = px + (pw - total) / 2;
        int by = py + 100;
        Rectangle[] alvos = new Rectangle[Alvo.values().length];
        alvos[Alvo.FACIL.ordinal()] = new Rectangle(bx, by, btnW, btnH);
        alvos[Alvo.MEDIO.ordinal()] = new Rectangle(bx + btnW + gap, by, btnW, btnH);
        alvos[Alvo.DIFICIL.ordinal()] = new Rectangle(bx + 2 * (btnW + gap), by, btnW, btnH);
        int comecarW = Math.min(140, pw - 24);
        alvos[Alvo.COMECAR.ordinal()] = new Rectangle(
                px + (pw - comecarW) / 2, py + 150, comecarW, 32);

        Rectangle dica = new Rectangle(px, py + ph - 20, pw, 16);
        return new Menu(janela(), painel, titulo, rotulo, dica, alvos);
    }

    /** A tela de fim, desenhada por cima do campo que a gerou. */
    public Fim fim() {
        int pw = larguraJanela - 2 * margem;
        int ph = 230;
        int px = margem;
        int py = Math.max(margem, (alturaJanela - ph) / 2);

        Rectangle painel = new Rectangle(px, py, pw, ph);
        Rectangle titulo = new Rectangle(px, py + 14, pw, 36);
        Rectangle subtitulo = new Rectangle(px + 12, py + 60, pw - 24, 24);

        int largo = Math.min(170, pw - 24);
        Rectangle recorde = new Rectangle(px + (pw - largo) / 2, py + 96, largo, 30);

        Rectangle[] alvos = new Rectangle[Alvo.values().length];
        int btnW = Math.min(150, pw - 24);
        alvos[Alvo.JOGAR_DE_NOVO.ordinal()] = new Rectangle(
                px + (pw - btnW) / 2, py + 140, btnW, 30);
        alvos[Alvo.VOLTAR_AO_MENU.ordinal()] = new Rectangle(
                px + (pw - btnW) / 2, py + 178, btnW, 26);

        Rectangle dica = new Rectangle(px, py + ph - 20, pw, 16);
        return new Fim(janela(), painel, titulo, subtitulo, recorde, dica, alvos);
    }

    /**
     * O alvo que contém o ponto, ou {@code null} se nenhum.
     *
     * @param tela o menu ou o fim
     * @param x    coordenada horizontal do ponto
     * @param y    coordenada vertical do ponto
     * @return o alvo acertado, ou {@code null}
     */
    public static Alvo alvoEm(Alvos tela, int x, int y) {
        if (tela == null) {
            return null;
        }
        for (Alvo alvo : Alvo.values()) {
            Rectangle r = tela.retangulo(alvo);
            if (r != null && r.contains(x, y)) {
                return alvo;
            }
        }
        return null;
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