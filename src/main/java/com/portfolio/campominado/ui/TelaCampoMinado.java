package com.portfolio.campominado.ui;

import com.portfolio.campominado.core.Dificuldade;
import com.portfolio.campominado.core.Tabuleiro;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * A janela do Campo Minado.
 *
 * <p>Aqui não há regra nenhuma: tudo que decide mora em {@link Tabuleiro}.
 * Esta classe entrega o clique do mouse (esquerda abre, direita marca), as
 * teclas (setas movem o cursor, espaço abre, F marca, R reinicia) e o
 * cronômetro — e repassa o desenho para {@link DesenhoCampoMinado}, que não
 * sabe nada de janela. A geometria do clique é do {@link LayoutCampoMinado}.
 *
 * <p>O cronômetro é um {@link Timer} que só roda entre o primeiro clique e o
 * fim da partida, e o tempo é contado no tique do relógio, nunca dentro do
 * desenho — senão cada repintura inventar-se-ia um segundo novo. Ganhou ou
 * perdeu, o relógio para e o quadro mostra o tempo final.
 */
public final class TelaCampoMinado extends JFrame {

    private static final long serialVersionUID = 1L;

    /** De quanto em quanto o cronômetro anda. */
    static final int INTERVALO_TIMER_MS = 1000;

    private final transient LayoutCampoMinado layout;
    private final transient Dificuldade dificuldade;
    private final transient DesenhoCampoMinado desenho = new DesenhoCampoMinado();

    private transient Tabuleiro tabuleiro;
    private transient Timer timer;
    private int segundos;
    private int cursorLinha;
    private int cursorColuna;

    /** Cria a janela com a dificuldade e o layout que ela pede. */
    public TelaCampoMinado(Dificuldade dificuldade) {
        this(dificuldade, new LayoutCampoMinado(dificuldade));
    }

    /**
     * Cria a janela com um layout dado (para o teste sem abrir janela própria).
     *
     * @param dificuldade a dificuldade do tabuleiro
     * @param layout      onde cada coisa é desenhada
     */
    public TelaCampoMinado(Dificuldade dificuldade, LayoutCampoMinado layout) {
        super("Campo Minado");
        this.dificuldade = dificuldade;
        this.layout = layout;
        this.tabuleiro = novoTabuleiro();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setPreferredSize(new Dimension(layout.getLarguraJanela(), layout.getAlturaJanela()));
        setBackground(DesenhoCampoMinado.FUNDO);
        setFocusable(true);
        setContentPane(new Painel());
        pack();
        setLocationRelativeTo(null);
    }

    /** O painel que sabe se pintar, separado para o teste alcançá-lo sem janela. */
    private final class Painel extends javax.swing.JPanel {

        private static final long serialVersionUID = 1L;

        Painel() {
            setBackground(DesenhoCampoMinado.FUNDO);
            setPreferredSize(new Dimension(layout.getLarguraJanela(), layout.getAlturaJanela()));
            // O mouse é tratado no painel, não na JFrame: as coordenadas do evento
            // precisam ser as do painel, e acertar a origem na mão erra o clique
            // quando a janela é movida.
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    processarClique(e.getX(), e.getY(),
                            SwingUtilities.isRightMouseButton(e));
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                desenho.pintar(g2, tabuleiro, layout,
                        cursorLinha, cursorColuna, mostraCursor(),
                        segundos, dificuldade.getRotulo());
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // Mouse
    // ------------------------------------------------------------------

    /**
     * Um clique no painel: esquerdo abre, direito marca, e o cursor acompanha.
     *
     * <p>Método separado do listener para o teste alcançá-lo sem evento: o que
     * interessa é a regra de qual botão faz o quê e de o clique cair na célula
     * certa pela geometria.
     *
     * @param x            coordenada horizontal do clique
     * @param y            coordenada vertical do clique
     * @param botaoDireito {@code true} marca, {@code false} abre
     */
    void processarClique(int x, int y, boolean botaoDireito) {
        int indice = layout.indiceDoPonto(x, y);
        if (indice < 0) {
            return;
        }
        moverCursorPara(indice);
        int linha = cursorLinha;
        int coluna = cursorColuna;
        if (botaoDireito) {
            tabuleiro.alternarMarcacao(linha, coluna);
        } else {
            tabuleiro.abrir(linha, coluna);
        }
        aposAcao();
    }

    // ------------------------------------------------------------------
    // Cronômetro
    // ------------------------------------------------------------------

    /** O corpo de um tique do relógio, chamado pelo Timer. */
    void tique() {
        if (tabuleiro.getEstado() != Tabuleiro.Estado.JOGANDO) {
            return;
        }
        segundos++;
        repaint();
    }

    /** Liga o cronômetro no primeiro clique que começa a partida. */
    private void iniciarCronometro() {
        if (timer == null) {
            timer = new Timer(INTERVALO_TIMER_MS, e -> tique());
        }
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    private void pararCronometro() {
        if (timer != null) {
            timer.stop();
        }
    }

    /**
     * O que mudou depois de um clique ou tecla: liga o relógio quando a
     * partida começa e o para quando ela acaba. É aqui, e não no desenho, que
     * o tempo é marcado.
     */
    private void aposAcao() {
        Tabuleiro.Estado estado = tabuleiro.getEstado();
        if (estado == Tabuleiro.Estado.JOGANDO) {
            iniciarCronometro();
        } else if (estado == Tabuleiro.Estado.GANHOU || estado == Tabuleiro.Estado.PERDEU) {
            pararCronometro();
        }
        repaint();
    }

    // ------------------------------------------------------------------
    // Teclado
    // ------------------------------------------------------------------

    /**
     * Traduz uma tecla em ação.
     *
     * <p>Separado do tratamento de evento para o teste alcançar o teclado
     * inteiro sem abrir portas. Mover o cursor nunca abre célula: a seta só
     * anda, e o espaço é a única tecla que abre — do contrário o jogador
     * arriscaria o primeiro clique sem querer.
     *
     * @param codigo o {@code getKeyCode} da tecla
     * @return {@code true} se a tecla foi tratada
     */
    public boolean tratarTecla(int codigo) {
        switch (codigo) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                andarCursor(-1, 0);
                repaint();
                return true;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                andarCursor(1, 0);
                repaint();
                return true;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                andarCursor(0, -1);
                repaint();
                return true;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                andarCursor(0, 1);
                repaint();
                return true;
            case KeyEvent.VK_SPACE:
                tabuleiro.abrir(cursorLinha, cursorColuna);
                aposAcao();
                return true;
            case KeyEvent.VK_F:
                tabuleiro.alternarMarcacao(cursorLinha, cursorColuna);
                aposAcao();
                return true;
            case KeyEvent.VK_R:
                reiniciar();
                return true;
            case KeyEvent.VK_ESCAPE:
                pararCronometro();
                dispose();
                System.exit(0);
                return true;
            default:
                return false;
        }
    }

    /** Anda com o cursor, preso na grade. */
    private void andarCursor(int dLinha, int dColuna) {
        cursorLinha = Math.max(0, Math.min(layout.getLinhas() - 1, cursorLinha + dLinha));
        cursorColuna = Math.max(0, Math.min(layout.getColunas() - 1, cursorColuna + dColuna));
    }

    private void moverCursorPara(int indice) {
        cursorLinha = indice / layout.getColunas();
        cursorColuna = indice % layout.getColunas();
    }

    private boolean mostraCursor() {
        Tabuleiro.Estado estado = tabuleiro.getEstado();
        return estado != Tabuleiro.Estado.GANHOU && estado != Tabuleiro.Estado.PERDEU;
    }

    /** Recomeça do zero: tabuleiro novo, relógio parado, cursor na origem. */
    public void reiniciar() {
        pararCronometro();
        tabuleiro = novoTabuleiro();
        segundos = 0;
        cursorLinha = 0;
        cursorColuna = 0;
        repaint();
    }

    private Tabuleiro novoTabuleiro() {
        return new Tabuleiro(dificuldade.getLinhas(), dificuldade.getColunas(),
                dificuldade.getMinas());
    }

    // ------------------------------------------------------------------
    // Arranque
    // ------------------------------------------------------------------

    /**
     * Instala as teclas do jogo.
     *
     * <p>Usa {@code WHEN_IN_FOCUSED_WINDOW} e não um {@code KeyListener} na
     * janela: com listener, um clique em qualquer lugar tira o foco do painel
     * e o teclado morre sem aviso — o defeito mais comum em jogo Swing, que
     * nenhum teste de regra mostra, porque a regra está certa, quem não chega
     * é a tecla. Com {@code WHEN_IN_FOCUSED_WINDOW} a tecla chega com a janela
     * ativa, venha de onde vier o clique.
     */
    public void registrarTeclado() {
        javax.swing.InputMap im = getRootPane().getInputMap(
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getRootPane().getActionMap();

        int[] teclas = {
            KeyEvent.VK_UP, KeyEvent.VK_W,
            KeyEvent.VK_DOWN, KeyEvent.VK_S,
            KeyEvent.VK_LEFT, KeyEvent.VK_A,
            KeyEvent.VK_RIGHT, KeyEvent.VK_D,
            KeyEvent.VK_SPACE, KeyEvent.VK_F, KeyEvent.VK_R, KeyEvent.VK_ESCAPE,
        };
        for (int codigo : teclas) {
            final int tecla = codigo;
            String nome = "tecla-" + tecla;
            im.put(javax.swing.KeyStroke.getKeyStroke(tecla, 0), nome);
            am.put(nome, new javax.swing.AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    tratarTecla(tecla);
                }
            });
        }
        getRootPane().setFocusable(true);
    }

    /** Abre o jogo. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            TelaCampoMinado tela = new TelaCampoMinado(Dificuldade.MEDIO);
            tela.registrarTeclado();
            tela.setVisible(true);
        });
    }

    // Package-private, para o teste desta classe ver a partida e o relógio.

    Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    LayoutCampoMinado getGeometria() {
        return layout;
    }

    int getSegundos() {
        return segundos;
    }

    int getCursorLinha() {
        return cursorLinha;
    }

    int getCursorColuna() {
        return cursorColuna;
    }
}