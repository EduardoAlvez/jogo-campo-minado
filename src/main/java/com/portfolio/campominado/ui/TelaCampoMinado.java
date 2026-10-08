package com.portfolio.campominado.ui;

import com.portfolio.campominado.audio.Sons;
import com.portfolio.campominado.audio.Trilha;
import com.portfolio.campominado.core.Dificuldade;
import com.portfolio.campominado.core.RegistroDeTempos;
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
 * A janela do Campo Minado, com as três telas: menu, jogo e fim.
 *
 * <p>Aqui não há regra nenhuma: tudo que decide mora em {@link Tabuleiro},
 * e as telas só escolhem <b>qual</b> desenho usar e <b>onde</b> o clique cai.
 * O menu devolve a dificuldade escolhida ao jogo; o fim mostra o resultado, o
 * tempo e o selo de recorde; o jogo entrega o clique do mouse (esquerda abre,
 * direita marca), as teclas (setas/WASD movem o cursor, espaço abre, F marca,
 * R reinicia, ESC volta ao menu) e o cronômetro. O desenho fica com
 * {@link DesenhoCampoMinado}, que não sabe nada de janela; a geometria é do
 * {@link LayoutCampoMinado}, que também não sabe.
 *
 * <p><b>O som decide-se em {@link Trilha}</b>, fora da janela: um retrato do
 * tabuleiro antes da jogada e outro depois dizem o que tocar. É uma função
 * pura, testada sem display.
 *
 * <p>O cronômetro é um {@link Timer} que só roda entre o primeiro clique e o
 * fim da partida, e o tempo é contado no tique do relógio, nunca dentro do
 * desenho — senão cada repintura inventaria um segundo novo.
 */
public final class TelaCampoMinado extends JFrame {

    private static final long serialVersionUID = 1L;

    /** De quanto em quanto o cronômetro anda. */
    static final int INTERVALO_TIMER_MS = 1000;

    /** A tela que está na frente. */
    public enum Tela {
        MENU, JOGO, FIM
    }

    private LayoutCampoMinado layout;
    private Dificuldade dificuldade;
    private final transient DesenhoCampoMinado desenho = new DesenhoCampoMinado();

    private transient Tabuleiro tabuleiro;
    private transient Timer timer;
    private int segundos;
    private int cursorLinha;
    private int cursorColuna;

    private Tela tela = Tela.MENU;
    private LayoutCampoMinado.Alvo hover;
    private boolean fimVitoria;
    private boolean fimNovoRecorde;
    private int fimMelhorTempo;

    /** Cria a janela com a dificuldade e o layout que ela pede. */
    public TelaCampoMinado(Dificuldade dificuldade) {
        this(dificuldade, new LayoutCampoMinado(dificuldade));
    }

    /**
     * Cria a janela com um layout dado (para o teste sem abrir janela própria).
     */
    public TelaCampoMinado(Dificuldade dificuldade, LayoutCampoMinado layout) {
        super("Campo Minado");
        this.dificuldade = dificuldade;
        this.layout = layout;
        this.tabuleiro = novoTabuleiro();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        // O tamanho preferido é do painel de conteúdo, não do frame: se o frame
        // declarar o tamanho do layout, o pack() usa esse valor como janela
        // externa e o conteúdo encolhe pelos insets da barra de título — o lado
        // direito e o de baixo do campo ficam cortados.
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

                @Override
                public void mouseExited(MouseEvent e) {
                    if (hover != null) {
                        hover = null;
                        repaint();
                    }
                }
            });
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    moverPonteiroPara(e.getX(), e.getY());
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                if (tela == Tela.MENU) {
                    desenho.pintarMenu(g2, layout.menu(), dificuldade, hover);
                } else if (tela == Tela.FIM) {
                    desenho.pintarFim(g2, layout.fim(), tituloDoFim(),
                            subtituloDoFim(), fimNovoRecorde, hover);
                } else {
                    desenho.pintar(g2, tabuleiro, layout,
                            cursorLinha, cursorColuna, mostraCursor(),
                            segundos, dificuldade.getRotulo());
                }
            } finally {
                g2.dispose();
            }
        }
    }

    // ------------------------------------------------------------------
    // As três telas
    // ------------------------------------------------------------------

    /**
     * Clique no painel: no menu acerta um alvo, no jogo uma célula.
     *
     * <p>Método separado do listener para o teste alcançá-lo sem evento.
     */
    void processarClique(int x, int y, boolean botaoDireito) {
        if (tela == Tela.JOGO && partidaAcabou()) {
            irParaFim();
            return;
        }
        if (tela == Tela.MENU) {
            tratarCliqueNaAlvo(LayoutCampoMinado.alvoEm(layout.menu(), x, y));
            return;
        }
        if (tela == Tela.FIM) {
            tratarCliqueNaAlvo(LayoutCampoMinado.alvoEm(layout.fim(), x, y));
            return;
        }
        int indice = layout.indiceDoPonto(x, y);
        if (indice < 0) {
            return;
        }
        moverCursorPara(indice);
        int linha = cursorLinha;
        int coluna = cursorColuna;
        if (botaoDireito) {
            agir(() -> tabuleiro.alternarMarcacao(linha, coluna));
        } else {
            agir(() -> tabuleiro.abrir(linha, coluna));
        }
    }

    private void tratarCliqueNaAlvo(LayoutCampoMinado.Alvo alvo) {
        if (alvo == null) {
            return;
        }
        switch (alvo) {
            case FACIL:
                escolher(Dificuldade.FACIL);
                break;
            case MEDIO:
                escolher(Dificuldade.MEDIO);
                break;
            case DIFICIL:
                escolher(Dificuldade.DIFICIL);
                break;
            case COMECAR:
            case JOGAR_DE_NOVO:
                comecar();
                break;
            case VOLTAR_AO_MENU:
                voltarAoMenu();
                break;
            default:
                // nenhum outro alvo existe
        }
    }

    private void escolher(Dificuldade escolhida) {
        if (dificuldade != escolhida) {
            dificuldade = escolhida;
            repaint();
        }
    }

    /** Começa uma partida na dificuldade escolhida, da tela de menu ou de fim. */
    void comecar() {
        this.layout = new LayoutCampoMinado(dificuldade);
        this.tabuleiro = novoTabuleiro();
        segundos = 0;
        cursorLinha = 0;
        cursorColuna = 0;
        pararCronometro();
        tela = Tela.JOGO;
        hover = null;
        ajustarJanela();
        repaint();
    }

    /** Volta ao menu, deixando a dificuldade escolhida guardada. */
    void voltarAoMenu() {
        pararCronometro();
        tela = Tela.MENU;
        hover = null;
        repaint();
    }

    private void sair() {
        dispose();
        System.exit(0);
    }

    /** Reajusta o painel ao tamanho da dificuldade nova, e a janela com ele. */
    private void ajustarJanela() {
        Dimension novo = new Dimension(layout.getLarguraJanela(), layout.getAlturaJanela());
        getContentPane().setPreferredSize(novo);
        pack();
    }

    private String tituloDoFim() {
        return fimVitoria ? "Você venceu!" : "Você perdeu";
    }

    private String subtituloDoFim() {
        if (fimVitoria) {
            return "Tempo: " + segundos + "s   Melhor: " + fimMelhorTempo + "s";
        }
        return "Tempo: " + segundos + "s";
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
     * O que mudou depois de um clique ou tecla: liga o relógio quando a partida
     * começa e, quando ela acaba, para o relógio e vai para a tela de fim.
     * É aqui, e não no desenho, que o tempo é marcado.
     */
    private void aposAcao() {
        Tabuleiro.Estado estado = tabuleiro.getEstado();
        if (estado == Tabuleiro.Estado.JOGANDO) {
            iniciarCronometro();
        } else if (estado == Tabuleiro.Estado.GANHOU
                || estado == Tabuleiro.Estado.PERDEU) {
            pararCronometro();
            mostrarFim(estado);
        }
        repaint();
    }

    /** Monta a tela de fim: o resultado, e o recorde se a vitória foi dele. */
    private void mostrarFim(Tabuleiro.Estado estado) {
        fimVitoria = estado == Tabuleiro.Estado.GANHOU;
        fimMelhorTempo = RegistroDeTempos.melhorTempo(dificuldade);
        fimNovoRecorde = false;
        if (fimVitoria) {
            fimNovoRecorde = RegistroDeTempos.registrar(dificuldade, segundos);
            fimMelhorTempo = RegistroDeTempos.melhorTempo(dificuldade);
        }
        // Não troca de tela na hora: a partida acabou, mas o campo resolvido
        // (as minas reveladas na derrota, tudo aberto na vitória) fica à mostra
        // até o primeiro toque — tecla ou clique — que leva à tela de fim.
    }

    /** A partida acabou, e o campo resolvido ainda está à mostra. */
    private boolean partidaAcabou() {
        Tabuleiro.Estado estado = tabuleiro.getEstado();
        return estado == Tabuleiro.Estado.GANHOU || estado == Tabuleiro.Estado.PERDEU;
    }

    /** Vai, enfim, para a tela de fim. Chamado pelo toque e pelo teste. */
    void irParaFim() {
        tela = Tela.FIM;
        hover = null;
        repaint();
    }

    // ------------------------------------------------------------------
    // Som
    // ------------------------------------------------------------------

    /**
     * Executa a jogada fotografando o antes, o depois e tocando o que mudou.
     *
     * <p>O retrato do antes é obrigatório: sem ele, ler o tabuleiro depois da
     * jogada e chamar aquilo de "antes" passa o teste da regra certa no efeito
     * errado — o som de abrir tocando junto do som de derrota, por exemplo.
     */
    private void agir(Runnable acao) {
        Trilha.Retrato antes = Trilha.tira(tabuleiro);
        acao.run();
        for (Sons.Efeito efeito : Trilha.dePara(antes, Trilha.tira(tabuleiro))) {
            Sons.tocar(efeito);
        }
        aposAcao();
    }

    // ------------------------------------------------------------------
    // Teclado
    // ------------------------------------------------------------------

    /**
     * Traduz uma tecla em ação, conforme a tela da frente.
     *
     * <p>No menu: 1-3 escolhem a dificuldade, Enter começa. No jogo: setas/WASD
     * movem o cursor — sem abrir célula —, espaço abre, F marca, R reinicia e
     * ESC volta ao menu. No fim: Enter joga de novo, M volta ao menu.
     *
     * @param codigo o {@code getKeyCode} da tecla
     * @return {@code true} se a tecla foi tratada
     */
    public boolean tratarTecla(int codigo) {
        if (tela == Tela.MENU) {
            switch (codigo) {
                case KeyEvent.VK_1:
                    escolher(Dificuldade.FACIL);
                    return true;
                case KeyEvent.VK_2:
                    escolher(Dificuldade.MEDIO);
                    return true;
                case KeyEvent.VK_3:
                    escolher(Dificuldade.DIFICIL);
                    return true;
                case KeyEvent.VK_ENTER:
                case KeyEvent.VK_SPACE:
                    comecar();
                    return true;
                case KeyEvent.VK_ESCAPE:
                    sair();
                    return true;
                default:
                    return false;
            }
        }
        if (tela == Tela.FIM) {
            switch (codigo) {
                case KeyEvent.VK_ENTER:
                    comecar();
                    return true;
                case KeyEvent.VK_M:
                    voltarAoMenu();
                    return true;
                case KeyEvent.VK_ESCAPE:
                    sair();
                    return true;
                default:
                    return false;
            }
        }
        if (tela == Tela.JOGO && partidaAcabou()) {
            irParaFim();
            return true;
        }
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
                agir(() -> tabuleiro.abrir(cursorLinha, cursorColuna));
                return true;
            case KeyEvent.VK_F:
                agir(() -> tabuleiro.alternarMarcacao(cursorLinha, cursorColuna));
                return true;
            case KeyEvent.VK_R:
                reiniciar();
                return true;
            case KeyEvent.VK_ESCAPE:
                voltarAoMenu();
                return true;
            default:
                return false;
        }
    }

    // ------------------------------------------------------------------
    // Cursor e ponteiro
    // ------------------------------------------------------------------

    /** Anda com o cursor, preso na grade. */
    private void andarCursor(int dLinha, int dColuna) {
        cursorLinha = Math.max(0, Math.min(layout.getLinhas() - 1, cursorLinha + dLinha));
        cursorColuna = Math.max(0, Math.min(layout.getColunas() - 1, cursorColuna + dColuna));
    }

    private void moverCursorPara(int indice) {
        cursorLinha = indice / layout.getColunas();
        cursorColuna = indice % layout.getColunas();
    }

    private void moverPonteiroPara(int x, int y) {
        LayoutCampoMinado.Alvo novo = null;
        if (tela == Tela.MENU) {
            novo = LayoutCampoMinado.alvoEm(layout.menu(), x, y);
        } else if (tela == Tela.FIM) {
            novo = LayoutCampoMinado.alvoEm(layout.fim(), x, y);
        }
        if (novo != hover) {
            hover = novo;
            repaint();
        }
    }

    private boolean mostraCursor() {
        Tabuleiro.Estado estado = tabuleiro.getEstado();
        return estado != Tabuleiro.Estado.GANHOU && estado != Tabuleiro.Estado.PERDEU;
    }

    /** Recomeça a partida atual: tabuleiro novo, relógio parado, cursor na origem. */
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
     * e o teclado morre sem aviso — o defeito mais comum em jogo Swing. Com
     * {@code WHEN_IN_FOCUSED_WINDOW} a tecla chega com a janela ativa, venha
     * de onde vier o clique.
     */
    public void registrarTeclado() {
        javax.swing.InputMap im = getRootPane().getInputMap(
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW);
        javax.swing.ActionMap am = getRootPane().getActionMap();

        int[] teclas = {
            KeyEvent.VK_1, KeyEvent.VK_2, KeyEvent.VK_3,
            KeyEvent.VK_UP, KeyEvent.VK_W,
            KeyEvent.VK_DOWN, KeyEvent.VK_S,
            KeyEvent.VK_LEFT, KeyEvent.VK_A,
            KeyEvent.VK_RIGHT, KeyEvent.VK_D,
            KeyEvent.VK_SPACE, KeyEvent.VK_F, KeyEvent.VK_R,
            KeyEvent.VK_M, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE,
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

    Tela getTela() {
        return tela;
    }

    Dificuldade getDificuldade() {
        return dificuldade;
    }

    boolean fimVenceu() {
        return fimVitoria;
    }

    boolean fimNovoRecorde() {
        return fimNovoRecorde;
    }
}