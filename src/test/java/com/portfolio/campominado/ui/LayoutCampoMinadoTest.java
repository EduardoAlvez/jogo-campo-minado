package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

    // ------------------------------------------------------------------
    // Menu e fim de partida
    // ------------------------------------------------------------------

    @Test
    public void menuCabeInteiroNaJanelaDohorCampoFacil() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        LayoutCampoMinado.Menu menu = layout.menu();
        assertTrue("o painel do menu dentro da janela",
                layout.dentroDaJanela(menu.painel()));
        for (LayoutCampoMinado.Alvo alvo : LayoutCampoMinado.Alvo.values()) {
            Rectangle r = menu.retangulo(alvo);
            if (r != null) {
                assertTrue("alvo " + alvo + " dentro da janela",
                        layout.dentroDaJanela(r));
                assertTrue("alvo " + alvo + " dentro do painel",
                        menu.painel().contains(r));
            }
        }
    }

    @Test
    public void menuTemAsTresDificuldadesNaMesmaLinhaEComecarEmbaixo() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        LayoutCampoMinado.Menu menu = layout.menu();
        assertEquals(menu.facil().y, menu.medio().y);
        assertEquals(menu.medio().y, menu.dificil().y);
        assertTrue("FÁCIL à esquerda de MÉDIO",
                menu.facil().x + menu.facil().width <= menu.medio().x);
        assertTrue("MÉDIO à esquerda de DIFÍCIL",
                menu.medio().x + menu.medio().width <= menu.dificil().x);
        assertTrue("COMECAR abaixo dos botões de dificuldade",
                menu.comecar().y >= menu.dificil().y + menu.dificil().height);
        assertTrue("os três botões não sobram do painel",
                menu.dificil().x + menu.dificil().width <= menu.painel().x
                        + menu.painel().width);
    }

    @Test
    public void menuAlvoEmAcertaOCentroDeCadaBotao() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        LayoutCampoMinado.Menu menu = layout.menu();
        assertAcerta(menu, menu.facil(), LayoutCampoMinado.Alvo.FACIL);
        assertAcerta(menu, menu.medio(), LayoutCampoMinado.Alvo.MEDIO);
        assertAcerta(menu, menu.dificil(), LayoutCampoMinado.Alvo.DIFICIL);
        assertAcerta(menu, menu.comecar(), LayoutCampoMinado.Alvo.COMECAR);
    }

    @Test
    public void menuForaDeBotaoNaoAcertaAlvoNenhum() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        LayoutCampoMinado.Menu menu = layout.menu();
        assertNull("o vão entre os botões é respiro, não clique",
                LayoutCampoMinado.alvoEm(menu, menu.facil().x + menu.facil().width + 5,
                        menu.facil().y + 5));
        assertNull("o título não é um alvo",
                LayoutCampoMinado.alvoEm(menu, menu.titulo().x + 10,
                        menu.titulo().y + 10));
    }

    @Test
    public void fimCabeInteiroENaoDeixaOsDoisBotoesSeTocarem() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        LayoutCampoMinado.Fim fim = layout.fim();
        assertTrue(layout.dentroDaJanela(fim.painel()));
        assertTrue(fim.painel().contains(fim.recorde()));
        assertTrue("JOGAR DE NOVO acima de VOLTAR AO MENU",
                fim.jogarDeNovo().y + fim.jogarDeNovo().height <= fim.voltarAoMenu().y);
        assertFalse("o selo do recorde não invade o botão de cima",
                fim.recorde().intersects(fim.jogarDeNovo()));
        assertAcerta(fim, fim.jogarDeNovo(), LayoutCampoMinado.Alvo.JOGAR_DE_NOVO);
        assertAcerta(fim, fim.voltarAoMenu(), LayoutCampoMinado.Alvo.VOLTAR_AO_MENU);
    }

    @Test
    public void alvoDeUmaTelaNaoVazaNaOutra() {
        LayoutCampoMinado layout = new LayoutCampoMinado(LINHAS, COLUNAS, 30, 12, 64);
        assertNull("o menu não tem JOGAR DE NOVO",
                layout.menu().retangulo(LayoutCampoMinado.Alvo.JOGAR_DE_NOVO));
        assertNull("o fim não tem COMECAR",
                layout.fim().retangulo(LayoutCampoMinado.Alvo.COMECAR));
        assertNull("o fim não tem dificuldade",
                layout.fim().retangulo(LayoutCampoMinado.Alvo.FACIL));
    }

    private void assertAcerta(LayoutCampoMinado.Alvos tela, Rectangle botao,
            LayoutCampoMinado.Alvo alvo) {
        assertNotNull("o alvo " + alvo + " existe", botao);
        assertEquals("o centro do botão acerta " + alvo, alvo,
                LayoutCampoMinado.alvoEm(tela, botao.x + botao.width / 2,
                        botao.y + botao.height / 2));
    }
}