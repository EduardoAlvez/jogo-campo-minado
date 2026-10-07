package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.portfolio.campominado.core.Dificuldade;
import com.portfolio.campominado.core.Tabuleiro;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * A janela, e não o desenho.
 *
 * <p>O que mora aqui é a ligação entre o clique/tecla e o tabuleiro: o desenho
 * sabe pintar uma célula, mas quem decide que o clique da esquerda abre, o da
 * direita marca e o espaço usa o cursor é a tela. O relógio também é dela — o
 * teste conta o tempo chamando {@link TelaCampoMinado#tique()}, sem depender do
 * {@code Timer} de verdade.
 */
public class TelaCampoMinadoTest {

    private LayoutCampoMinado layout;
    private TelaCampoMinado tela;

    @Before
    public void janelaFacil() {
        layout = new LayoutCampoMinado(Dificuldade.FACIL);
        tela = new TelaCampoMinado(Dificuldade.FACIL, layout);
    }

    @After
    public void fecharJanela() {
        tela.dispose();
    }

    // ------------------------------------------------------------------
    // Começo
    // ------------------------------------------------------------------

    @Test
    public void nasceAguardandoSemMarcarTempo() {
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
        assertEquals(0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
    }

    @Test
    public void tiqueNaNaoContaNadaAntesDoPrimeiroClique() {
        tela.tique();
        assertEquals(0, tela.getSegundos());
    }

    // ------------------------------------------------------------------
    // Mouse
    // ------------------------------------------------------------------

    @Test
    public void cliqueEsquerdoNoCentroDaCelulaComecaAPartida() {
        Rectangle primeira = layout.celula(0, 0);
        tela.processarClique(primeira.x + 15, primeira.y + 15, false);

        assertEquals(Tabuleiro.Estado.JOGANDO, tela.getTabuleiro().getEstado());
        assertEquals("o cursor acompanha a célula clicada",
                0, tela.getCursorLinha());
        assertEquals(0, tela.getCursorColuna());
    }

    @Test
    public void cliqueEsquerdoEmOutraCelulaAbreNela() {
        Rectangle alvo = layout.celula(3, 5);
        tela.processarClique(alvo.x + 15, alvo.y + 15, false);

        assertTrue("a célula clicada fica aberta",
                tela.getTabuleiro().getCelula(3, 5).isAberto());
        assertEquals(3, tela.getCursorLinha());
        assertEquals(5, tela.getCursorColuna());
    }

    @Test
    public void cliqueDireitoMarcaSemAbrir() {
        Rectangle alvo = layout.celula(4, 4);
        tela.processarClique(alvo.x + 15, alvo.y + 15, true);

        assertTrue(tela.getTabuleiro().getCelula(4, 4).isMarcado());
        assertFalse("marcar não abre a célula",
                tela.getTabuleiro().getCelula(4, 4).isAberto());
        assertEquals("marcar não começa a partida",
                Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
    }

    @Test
    public void cliqueForaDoCampoNaoFazNada() {
        tela.processarClique(5, 5, false);
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
        assertEquals(0, tela.getSegundos());
    }

    // ------------------------------------------------------------------
    // Teclado
    // ------------------------------------------------------------------

    @Test
    public void setasMovemOCursorSemAbrirCelula() {
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
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        tela.tratarTecla(KeyEvent.VK_DOWN);

        assertTrue(tela.tratarTecla(KeyEvent.VK_SPACE));
        assertEquals(Tabuleiro.Estado.JOGANDO, tela.getTabuleiro().getEstado());
        assertTrue("a célula (1,1) abre no espaço",
                tela.getTabuleiro().getCelula(1, 1).isAberto());
    }

    @Test
    public void cursorNaoSaiDaGrade() {
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
        tela.tratarTecla(KeyEvent.VK_RIGHT);
        assertTrue(tela.tratarTecla(KeyEvent.VK_F));
        assertTrue(tela.getTabuleiro().getCelula(0, 1).isMarcado());
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tela.getTabuleiro().getEstado());
    }

    // ------------------------------------------------------------------
    // Cronômetro e reinício
    // ------------------------------------------------------------------

    @Test
    public void tiqueContaSegundosSoDepoisDeComecar() {
        tela.processarClique(layout.celula(2, 2).x + 15, layout.celula(2, 2).y + 15, false);
        assertEquals(0, tela.getSegundos());

        tela.tique();
        assertEquals(1, tela.getSegundos());
        tela.tique();
        assertEquals(2, tela.getSegundos());
    }

    @Test
    public void reiniciaTodoOEstado() {
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
}