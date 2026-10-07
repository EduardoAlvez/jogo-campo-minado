package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.portfolio.campominado.core.Dificuldade;
import java.awt.Rectangle;
import org.junit.Test;

/**
 * A geometria da grade: células inteiras cobrindo o campo sem fresta, e a
 * conta inversa (ponto → célula) certa na origem e na ponta de cada célula.
 *
 * <p>Tudo calculado sem abrir janela: é exatamente para os testes alcançarem
 * a resposta que a posição não mora no método de pintura.
 */
public class LayoutCampoMinadoTest {

    private static final int LINHAS = 9;
    private static final int COLUNAS = 9;

    @Test
    public void janelaNasceDoCampoComCelulaInteira() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        assertEquals("largura da janela = celula x colunas + 2 margens",
                30 * COLUNAS + 24, layout.getLarguraJanela());
        assertEquals("altura da janela = celula x linhas + hud + 2 margens",
                30 * LINHAS + 64 + 24, layout.getAlturaJanela());
        assertEquals(30, layout.getCelula());
    }

    @Test
    public void celulasCobremOCampoSemFrestaNemSobreposicao() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        Rectangle a = layout.celula(2, 3);
        Rectangle b = layout.celula(2, 4);
        assertEquals("a vizinha da direita nasce onde a anterior termina",
                a.x + 30, b.x);
        assertEquals(a.y, b.y);
        assertEquals(30, a.width);
        assertEquals(30, a.height);
        Rectangle embaixo = layout.celula(3, 3);
        assertEquals("a vizinha de baixo nasce onde a anterior termina",
                a.y + 30, embaixo.y);
    }

    @Test
    public void todasAsCelulasFicamDentroDaJanela() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        for (int l = 0; l < LINHAS; l++) {
            for (int c = 0; c < COLUNAS; c++) {
                assertTrue("célula " + l + "x" + c + " dentro da janela",
                        layout.dentroDaJanela(layout.celula(l, c)));
            }
        }
    }

    @Test
    public void hudEFaixaDeStatusFicamDentroDaJanela() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        assertTrue(layout.dentroDaJanela(layout.hud()));
        assertTrue(layout.hud().y >= 0);
        assertTrue("o tabuleiro não pode invadir o hud",
                layout.tabuleiro().y >= layout.hud().y + layout.hud().height);
    }

    @Test
    public void indiceDoPontoAcertaOrigemMeioEPontaDeCadaCelula() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        int[] celulas = {0, 1, 10, 11, 40, 80};
        for (int indice : celulas) {
            Rectangle r = layout.celulaDoIndice(indice);
            assertNotNull("célula " + indice + " existe", r);
            assertEquals("o canto interno aponta para a célula "
                    + indice, indice, layout.indiceDoPonto(r.x + 1, r.y + 1));
            assertEquals("o centro aponta para a célula "
                    + indice, indice, layout.indiceDoPonto(r.x + 15, r.y + 15));
            assertEquals("o canto externo aponta para a célula "
                    + indice, indice, layout.indiceDoPonto(r.x + 29, r.y + 29));
        }
    }

    @Test
    public void indiceDoPontoForaDoCampoDevolveMenosUm() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        assertNull(layout.celulaDoIndice(layout.indiceDoPonto(-10, -10)));
        assertNull(layout.celulaDoIndice(layout.indiceDoPonto(10, 10)));
        assertNull(layout.celulaDoIndice(-1));
        assertNull(layout.celulaDoIndice(LINHAS * COLUNAS));
        assertEquals(-1, layout.indiceDoPonto(layout.tabuleiro().x - 1,
                layout.tabuleiro().y));
        assertEquals(-1, layout.indiceDoPonto(layout.tabuleiro().x,
                layout.tabuleiro().y + layout.tabuleiro().height));
    }

    @Test
    public void cantoDoHudNaoViraCelula() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        assertEquals(-1, layout.indiceDoPonto(layout.hud().x, layout.hud().y));
        assertEquals(-1, layout.indiceDoPonto(layout.hud().x + 5, layout.hud().y + 5));
    }

    @Test
    public void layoutDaDificuldadeUsaAGradeElaPede() {
        LayoutCampoMinado facil = new LayoutCampoMinado(Dificuldade.FACIL);
        assertEquals(Dificuldade.FACIL.getLinhas(), facil.getLinhas());
        assertEquals(Dificuldade.FACIL.getColunas(), facil.getColunas());

        LayoutCampoMinado dificil = new LayoutCampoMinado(Dificuldade.DIFICIL);
        assertEquals(Dificuldade.DIFICIL.getLinhas(), dificil.getLinhas());
        assertEquals(Dificuldade.DIFICIL.getColunas(), dificil.getColunas());
    }

    @Test
    public void rejeitaCampoOuCelulaPequenosDemais() {
        try {
            new LayoutCampoMinado(0, 9, 30, 12, 64);
            throw new AssertionError("deveria ter lançado IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            assertNotNull(esperada.getMessage());
        }
        try {
            new LayoutCampoMinado(9, 9, 4, 12, 64);
            throw new AssertionError("deveria ter lançado IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            assertNotNull(esperada.getMessage());
        }
    }
}