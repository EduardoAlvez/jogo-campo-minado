package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.portfolio.campominado.core.Dificuldade;
import com.portfolio.campominado.core.Tabuleiro;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;

/**
 * A imagem formada, conferida pixel a pixel num {@code BufferedImage}.
 *
 * <p>O desenho não sabe abrir janela e o teste não abre nenhuma: pinta numa
 * imagem do tamanho da janela e pergunta a cor do pixel. O ponto amostrado em
 * cada célula é o interior do canto superior esquerdo ({@code x+5},{@code y+5}),
 * que está sempre longe do relevo, do número e da bandeira — lá só existe a cor
 * de preenchimento. O centro guarda as marcas (bandeira, cursor).
 */
public class DesenhoCampoMinadoTest {

    private LayoutCampoMinado layout;
    private Tabuleiro tabuleiro;

    @Before
    public void campoFacil() {
        layout = new LayoutCampoMinado(Dificuldade.FACIL);
        tabuleiro = new Tabuleiro(Dificuldade.FACIL.getLinhas(),
                Dificuldade.FACIL.getColunas(), Dificuldade.FACIL.getMinas());
    }

    @Test
    public void antesDeAbrirTodasAsCelulasEstaoFechadas() {
        BufferedImage img = pintar(true);
        for (int l = 0; l < 9; l++) {
            for (int c = 0; c < 9; c++) {
                assertEquals("célula " + l + "x" + c + " fechada antes do primeiro clique",
                        DesenhoCampoMinado.COR_FECHADA, cantoDoInterior(img, l, c));
            }
        }
    }

    @Test
    public void foraDoCampoEOCampoPintamOBasicoDaJanela() {
        BufferedImage img = pintar(false);
        assertEquals(DesenhoCampoMinado.FUNDO, new Color(img.getRGB(4, 4)));
        assertEquals(DesenhoCampoMinado.FUNDO,
                new Color(img.getRGB(layout.getLarguraJanela() - 5,
                        layout.getAlturaJanela() - 5)));
        assertEquals(DesenhoCampoMinado.FUNDO, new Color(img.getRGB(5, 100)));
    }

    @Test
    public void primeiroCliqueDeixaACelulaAbertaEAlguemContinuaFechado() {
        tabuleiro.abrir(0, 0);
        BufferedImage img = pintar(false);
        assertEquals(DesenhoCampoMinado.COR_ABERTA, cantoDoInterior(img, 0, 0));
        assertTrue("pelo menos uma mina segue escondida e fechada",
                existeCelulaFechada(img));
    }

    @Test
    public void bandeiraFicaNoCentroDaCelulaFechada() {
        tabuleiro.alternarMarcacao(4, 4);
        BufferedImage img = pintar(false);
        Rectangle r = layout.celula(4, 4);
        assertTrue("a bandeira desenha algo na região do mastro",
                !DesenhoCampoMinado.COR_FECHADA.equals(
                        new Color(img.getRGB(r.x + 15, r.y + 15))));
        assertEquals("a bandeira não vaza para o canto da célula",
                DesenhoCampoMinado.COR_FECHADA, cantoDoInterior(img, 4, 4));
    }

    @Test
    public void cursorDesenhaUmContornoNaCelula() {
        BufferedImage sem = pintar(false);
        BufferedImage com = pintar(true);
        Rectangle r = layout.celula(2, 2);
        Color pontoSem = new Color(sem.getRGB(r.x + 2, r.y + 2));
        Color pontoCom = new Color(com.getRGB(r.x + 2, r.y + 2));
        assertEquals("sem cursor o ponto é o relevo da célula fechada",
                DesenhoCampoMinado.COR_FECHADA_BRILHO, pontoSem);
        assertEquals("com cursor o contorno cobre o mesmo ponto",
                DesenhoCampoMinado.COR_CURSOR, pontoCom);
    }

    @Test
    public void hudNaoFicaEmBranco() {
        tabuleiro.abrir(0, 0);
        BufferedImage img = pintar(true);
        Rectangle hud = layout.hud();
        boolean temTexto = false;
        for (int x = hud.x; x < hud.x + hud.width && !temTexto; x++) {
            for (int y = hud.y; y < hud.y + hud.height; y++) {
                if (!DesenhoCampoMinado.FUNDO.equals(new Color(img.getRGB(x, y)))) {
                    temTexto = true;
                    break;
                }
            }
        }
        assertTrue("o hud desenha pelo menos uma palavra", temTexto);
    }

    @Test
    public void corDoNumeroTemUmaCorParaCadaNumeroClassico() {
        Set<Integer> cores = new HashSet<>();
        for (int n = 1; n <= 8; n++) {
            Color cor = DesenhoCampoMinado.corDoNumero(n);
            assertNotNull("número " + n + " tem cor", cor);
            assertTrue("números 1 a 8 com cores distintas", cores.add(cor.getRGB()));
        }
        assertNull(DesenhoCampoMinado.corDoNumero(0));
        assertNull(DesenhoCampoMinado.corDoNumero(9));
    }

    private boolean existeCelulaFechada(BufferedImage img) {
        for (int l = 0; l < 9; l++) {
            for (int c = 0; c < 9; c++) {
                if (!DesenhoCampoMinado.COR_ABERTA.equals(cantoDoInterior(img, l, c))) {
                    return true;
                }
            }
        }
        return false;
    }

    private BufferedImage pintar(boolean cursor) {
        BufferedImage img = new BufferedImage(layout.getLarguraJanela(),
                layout.getAlturaJanela(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        new DesenhoCampoMinado().pintar(g2, tabuleiro, layout, 2, 2, cursor, 3, "Fácil");
        g2.dispose();
        return img;
    }

    private Color cantoDoInterior(BufferedImage img, int linha, int coluna) {
        Rectangle r = layout.celula(linha, coluna);
        return new Color(img.getRGB(r.x + 5, r.y + 5));
    }
}