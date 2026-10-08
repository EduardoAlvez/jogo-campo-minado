package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.portfolio.campominado.audio.Sons;
import com.portfolio.campominado.core.Dificuldade;
import com.portfolio.campominado.core.Tabuleiro;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * A janela, e não o desenho.
 *
 * <p>O que mora aqui é a ligação entre o clique/tecla e o tabuleiro — e agora
 * também a ligação entre as três telas. O desenho sabe pintar uma célula, mas
 * quem decide que o clique da esquerda abre, o da direita marca, o espaço usa o
 * cursor e o botão do menu começa é a tela. O relógio também é dela: o teste
 * conta o tempo chamando {@link TelaCampoMinado#tique()}, sem depender do
 * {@code Timer} de verdade.
 *
 * <p>Vitória e derrota são alcançáveis sem sorteio: depois do primeiro abrir —
 * que dispara o sorteio de minas — o teste pergunta ao tabuleiro quais células
 * são minas e abre o resto (vitória) ou abre uma mina (derrota). O caminho é
 * determinístico e usa só a API pública, igual ao que o jogador faz.
 */
public class TelaCampoMinadoTest {

    private LayoutCampoMinado layout;
    private TelaCampoMinado tela;
    private String homeOriginal;

    @Rule
    public TemporaryFolder pastaDeTempos = new TemporaryFolder();

    @Before
    public void janelaFacil() {
        layout = new LayoutCampoMinado(Dificuldade.FACIL);
        tela = new TelaCampoMinado(Dificuldade.FACIL, layout);
        homeOriginal = System.getProperty("user.home");
        System.setProperty("user.home", pastaDeTempos.getRoot().getAbsolutePath());
        Sons.mudo(true); // o teste não toca som de verdade
    }

    @After
    public void fecharJanela() {
        tela.dispose();
        Sons.mudo(false);
        System.setProperty("user.home", homeOriginal);
    }

    // ------------------------------------------------------------------
    // A janela e o tamanho
    // ------------------------------------------------------------------

    @Test
    public void oConteudoDaJanelaPintaOTamanhoExatoDoLayout() {
        assertEquals("o painel pinta exatamente a largura do layout: se o frame "
                + "declarar o tamanho preferido, o pack() encolhe o conteúdo pelos "
                + "insets e corta o lado direito do campo e do menu",
                layout.getLarguraJanela(), tela.getContentPane().getWidth());
        assertEquals("o painel pinta exatamente a altura do layout: senão a "
                + "barra de título come a parte de baixo do campo",
                layout.getAlturaJanela(), tela.getContentPane().getHeight());
    }

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------

    @Test
    public void nasceNoMenuAguardandoSemMarcarTempo() {
        assertEquals(TelaCampoMinado.Tela.MENU, tela.getTela());
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
        assertEquals(0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
    }

    @Test
    public void cliqueNoComecarDisparaAPartida() {
        Rectangle comecar = layout.menu().comecar();
        tela.processarClique(comecar.x + comecar.width / 2,
                comecar.y + comecar.height / 2, false);

        assertEquals(TelaCampoMinado.Tela.JOGO, tela.getTela());
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
    }

    @Test
    public void cliqueNaDificuldadeTrocaOSelecionadoSemComecar() {
        Rectangle dificil = layout.menu().dificil();
        tela.processarClique(dificil.x + dificil.width / 2,
                dificil.y + dificil.height / 2, false);

        assertEquals(Dificuldade.DIFICIL, tela.getDificuldade());
        assertEquals("escolher não começa a partida",
                TelaCampoMinado.Tela.MENU, tela.getTela());
    }

    @Test
    public void teclas123EscolhemADificuldade() {
        assertTrue(tela.tratarTecla(KeyEvent.VK_3));
        assertEquals(Dificuldade.DIFICIL, tela.getDificuldade());
        assertTrue(tela.tratarTecla(KeyEvent.VK_1));
        assertEquals(Dificuldade.FACIL, tela.getDificuldade());
        assertTrue(tela.tratarTecla(KeyEvent.VK_2));
        assertEquals(Dificuldade.MEDIO, tela.getDificuldade());
        assertEquals(TelaCampoMinado.Tela.MENU, tela.getTela());
    }

    @Test
    public void enterNoMenuComecaNaDificuldadeEscolhida() {
        tela.tratarTecla(KeyEvent.VK_2);
        assertTrue(tela.tratarTecla(KeyEvent.VK_ENTER));
        assertEquals(TelaCampoMinado.Tela.JOGO, tela.getTela());
        assertEquals(Dificuldade.MEDIO, tela.getDificuldade());
        assertEquals(Dificuldade.MEDIO.getLinhas(), tela.getTabuleiro().getLinhas());
    }

    // ------------------------------------------------------------------
    // Mouse
    // ------------------------------------------------------------------

    @Test
    public void cliqueEsquerdoNoCentroDaCelulaComecaAPartida() {
        tela.comecar();
        Rectangle primeira = layout.celula(0, 0);
        tela.processarClique(primeira.x + 15, primeira.y + 15, false);

        assertEquals(Tabuleiro.Estado.JOGANDO, tela.getTabuleiro().getEstado());
        assertEquals("o cursor acompanha a célula clicada",
                0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
    }

    @Test
    public void cliqueEsquerdoEmOutraCelulaAbreNela() {
        tela.comecar();
        Rectangle alvo = layout.celula(3, 5);
        tela.processarClique(alvo.x + 15, alvo.y + 15, false);

        assertTrue("a célula clicada fica aberta",
                tela.getTabuleiro().getCelula(3, 5).isAberto());
        assertEquals(3, tela.getCursorLinha());
        assertEquals(5, tela.getCursorColuna());
    }

    @Test
    public void cliqueDireitoMarcaSemAbrir() {
        tela.comecar();
        Rectangle alvo = layout.celula(4, 4);
        tela.processarClique(alvo.x + 15, alvo.y + 15, true);

        assertTrue(tela.getTabuleiro().getCelula(4, 4).isMarcado());
        assertFalse("marcar não abre a célula",
                tela.getTabuleiro().getCelula(4, 4).isAberto());
        assertEquals("marcar não começa a partida",
                Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
    }

    @Test
    public void cliqueForaDoCampoNoMenuNaoFazNada() {
        tela.processarClique(5, 5, false);
        assertEquals(TelaCampoMinado.Tela.MENU, tela.getTela());
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
    }

    // ------------------------------------------------------------------
    // Teclado do jogo
    // ------------------------------------------------------------------

    @Test
    public void setasMovemOCursorSemAbrirCelula() {
        tela.comecar();
        assertTrue(tela.tratarTecla(KeyEvent.VK_RIGHT));
        assertTrue(tela.tratarTecla(KeyEvent.VK_UP));
        assertEquals(0, tela.getCursorLinha());
        assertEquals(1, tela.getCursorColuna());
        assertEquals("a seta não pode abrir célula",
                Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
    }

    @Test
    public void espacoAbreOndeOCursorEsta() {
        tela.comecar();
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        tela.tratarTecla(KeyEvent.VK_DOWN);

        assertTrue(tela.tratarTecla(KeyEvent.VK_SPACE));
        assertEquals(Tabuleiro.Estado.JOGANDO, tela.getTabuleiro().getEstado());
        assertTrue("a célula (1,1) abre no espaço",
                tela.getTabuleiro().getCelula(1, 1).isAberto());
    }

    @Test
    public void cursorNaoSaiDaGrade() {
        tela.comecar();
        for (int i = 0; i < 5; i++) {
            tela.tratarTecla(KeyEvent.VK_UP);
            tela.tratarTecla(KeyEvent.VK_LEFT);
        }
        assertEquals(0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
        for (int i = 0; i < 15; i++) {
            tela.tratarTecla(KeyEvent.VK_DOWN);
            tela.tratarTecla(KeyEvent.VK_RIGHT);
        }
        assertEquals(layout.getLinhas() - 1, tela.getCursorLinha());
        assertEquals(layout.getColunas() - 1, tela.getCursorColuna());
    }

    @Test
    public void fmarcaNaCelulaDoCursor() {
        tela.comecar();
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        assertTrue(tela.tratarTecla(KeyEvent.VK_F));
        assertTrue(tela.getTabuleiro().getCelula(0, 1).isMarcado());
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
    }

    @Test
    public void escNoJogoVoltaAoMenuSemMatarOProcesso() {
        tela.comecar();
        assertTrue(tela.tratarTecla(KeyEvent.VK_ESCAPE));
        assertEquals(TelaCampoMinado.Tela.MENU, tela.getTela());
        assertEquals("a dificuldade escolhida não se perde ao voltar",
                Dificuldade.FACIL, tela.getDificuldade());
    }

    // ------------------------------------------------------------------
    // Cronômetro e reinício
    // ------------------------------------------------------------------

    @Test
    public void tiqueNaNaoContaAntesDeComecarNemNoMenu() {
        tela.tique();
        assertEquals(0, tela.getSegundos());
        tela.comecar();
        tela.tique();
        assertEquals("antes do primeiro clique a partida não anda",
                0, tela.getSegundos());
    }

    @Test
    public void tiqueContaSegundosSoDepoisDeComecar() {
        tela.comecar();
        tela.processarClique(layout.celula(2, 2).x + 15, layout.celula(2, 2).y + 15, false);
        assertEquals(0, tela.getSegundos());

        tela.tique();
        assertEquals(1, tela.getSegundos());
        tela.tique();
        assertEquals(2, tela.getSegundos());
    }

    @Test
    public void reiniciaTodoOEstado() {
        tela.comecar();
        tela.processarClique(layout.celula(2, 2).x + 15, layout.celula(2, 2).y + 15, false);
        tela.tique();
        tela.tique();
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        tela.processarClique(layout.celula(5, 5).x + 15, layout.celula(5, 5).y + 15, true);

        assertTrue(tela.tratarTecla(KeyEvent.VK_R));
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
        assertEquals(0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
        assertFalse("o reinício não pode guardar bandeira",
                tela.getTabuleiro().getCelula(5, 5).isMarcado());
    }

    // ------------------------------------------------------------------
    // Fim de partida
    // ------------------------------------------------------------------

    @Test
    public void aoVencerOCampoReveladoFicaNaTelaAteOToque() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));

        Tabuleiro tab = tela.getTabuleiro();
        for (int l = 0; l < tab.getLinhas(); l++) {
            for (int c = 0; c < tab.getColunas(); c++) {
                if (!tab.getCelula(l, c).isMinado() && !tab.getCelula(l, c).isAberto()) {
                    abreCelula(layout.celula(l, c));
                }
            }
        }

        assertEquals("a vitória deixa o campo aberto à mostra até o toque",
                TelaCampoMinado.Tela.JOGO, tela.getTela());
        assertTrue("tudo o que não é mina ficou aberto", tab.celulasAbertas() == tab.getLinhas()
                * tab.getColunas() - tab.getMinas());
    }

    @Test
    public void varrerTudoSeguroVaiParaOFimComSeloDeRecorde() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));

        Tabuleiro tab = tela.getTabuleiro();
        for (int l = 0; l < tab.getLinhas(); l++) {
            for (int c = 0; c < tab.getColunas(); c++) {
                if (!tab.getCelula(l, c).isMinado() && !tab.getCelula(l, c).isAberto()) {
                    abreCelula(layout.celula(l, c));
                }
            }
        }
        tela.irParaFim();

        assertEquals(TelaCampoMinado.Tela.FIM, tela.getTela());
        assertTrue("ganhar é vitória", tela.fimVenceu());
        assertTrue("o primeiro recorde da dificuldade nasce selado",
                tela.fimNovoRecorde());
    }

    @Test
    public void aoPerderOCampoRevelaAsMinasAteOToque() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));

        Tabuleiro tab = tela.getTabuleiro();
        int[] comMina = procuraFechada(tab, true);
        abreCelula(layout.celula(comMina[0], comMina[1]));

        assertEquals("a derrota não pode pular para a tela de fim sem mostrar o "
                + "campo: o jogador precisa ver onde estavam as bombas",
                TelaCampoMinado.Tela.JOGO, tela.getTela());
        for (int l = 0; l < tab.getLinhas(); l++) {
            for (int c = 0; c < tab.getColunas(); c++) {
                if (tab.getCelula(l, c).isMinado()) {
                    assertTrue("a mina (" + l + "," + c + ") foi revelada",
                            tab.getCelula(l, c).isAberto());
                }
            }
        }
    }

    @Test
    public void teclaDepoisDaDerrotaLevaATelaDeFimSemAgir() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));
        int[] comMina = procuraFechada(tela.getTabuleiro(), true);
        abreCelula(layout.celula(comMina[0], comMina[1]));
        assertEquals(TelaCampoMinado.Tela.JOGO, tela.getTela());

        assertTrue("o primeiro toque de tecla é tratado e vira a tela de fim",
                tela.tratarTecla(KeyEvent.VK_ENTER));
        assertEquals(TelaCampoMinado.Tela.FIM, tela.getTela());
        assertEquals("o toque só revela o fim, não começa outra partida",
                Dificuldade.FACIL, tela.getDificuldade());
    }

    @Test
    public void cliqueDepoisDaDerrotaLevaATelaDeFimSemAbrirCelula() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));
        int[] comMina = procuraFechada(tela.getTabuleiro(), true);
        abreCelula(layout.celula(comMina[0], comMina[1]));
        assertEquals(TelaCampoMinado.Tela.JOGO, tela.getTela());

        tela.processarClique(layout.celula(1, 1).x + 15, layout.celula(1, 1).y + 15, false);
        assertEquals(TelaCampoMinado.Tela.FIM, tela.getTela());
    }

    @Test
    public void abrirUmaMinaVaiParaOFimSemSelo() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));

        int[] comMina = procuraFechada(tela.getTabuleiro(), true);
        abreCelula(layout.celula(comMina[0], comMina[1]));
        tela.irParaFim();

        assertEquals(TelaCampoMinado.Tela.FIM, tela.getTela());
        assertFalse("perder não é vitória", tela.fimVenceu());
        assertFalse("derrota não ganha selo de recorde", tela.fimNovoRecorde());
    }

    @Test
    public void enterNoFimComecaDeNovoNaMesmaDificuldade() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));
        int[] comMina = procuraFechada(tela.getTabuleiro(), true);
        abreCelula(layout.celula(comMina[0], comMina[1]));
        tela.irParaFim();
        assertEquals(TelaCampoMinado.Tela.FIM, tela.getTela());

        assertTrue(tela.tratarTecla(KeyEvent.VK_ENTER));
        assertEquals(TelaCampoMinado.Tela.JOGO, tela.getTela());
        assertEquals(Dificuldade.FACIL, tela.getDificuldade());
        assertEquals(0, tela.getSegundos());
    }

    @Test
    public void mNoFimVoltaAoMenu() {
        tela.comecar();
        abreCelula(layout.celula(0, 0));
        int[] comMina = procuraFechada(tela.getTabuleiro(), true);
        abreCelula(layout.celula(comMina[0], comMina[1]));
        tela.irParaFim();

        assertTrue(tela.tratarTecla(KeyEvent.VK_M));
        assertEquals(TelaCampoMinado.Tela.MENU, tela.getTela());
    }

    // ------------------------------------------------------------------
    // Ferramentas do teste
    // ------------------------------------------------------------------

    /** Abre a célula pelo centro (ou volta ao menu, se o clique pegar um alvo). */
    private void abreCelula(Rectangle celula) {
        tela.processarClique(celula.x + celula.width / 2,
                celula.y + celula.height / 2, false);
    }

    private int[] procuraFechada(Tabuleiro t, boolean minada) {
        for (int l = 0; l < t.getLinhas(); l++) {
            for (int c = 0; c < t.getColunas(); c++) {
                if (!t.getCelula(l, c).isAberto() && t.getCelula(l, c).isMinado() == minada) {
                    return new int[]{l, c};
                }
            }
        }
        throw new IllegalStateException(
                "célula fechada minada=" + minada + " nao achada");
    }
}