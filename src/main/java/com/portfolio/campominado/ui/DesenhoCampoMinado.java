package com.portfolio.campominado.ui;

import com.portfolio.campominado.core.Celula;
import com.portfolio.campominado.core.Tabuleiro;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Pinta o campo minado num {@code Graphics2D} qualquer, sem saber de janela.
 *
 * <p>Mesma divisão da {@code DesenhoJogo} do trio: quem pinta não sabe abrir
 * portas, e quem abre portas (a {@link TelaCampoMinado}) não pinta. Com o
 * desenho isolado, um teste sem display constrói um {@code BufferedImage} do
 * tamanho da janela, manda esta classe pintar e confere os pixels.
 *
 * <p>Nenhuma decisão de estado mora aqui: o desenho recebe o <b>resultado</b>
 * das regras (o que está aberto, marcado, minado — tudo conta o
 * {@link Tabuleiro}) e o tempo em segundos, e só traduz isso em cor. A tecla
 * apertada, o clique dado e o relógio são coisas da tela.
 */
public final class DesenhoCampoMinado {

    /** O fundo da janela. */
    public static final Color FUNDO = new Color(0x18, 0x1b, 0x20);

    /** A célula fechada, alta como um botão esperando o clique. */
    public static final Color COR_FECHADA = new Color(0xbc, 0xc1, 0xc9);
    /** O brilho do relevo, no canto superior de uma célula fechada. */
    public static final Color COR_FECHADA_BRILHO = new Color(0xe6, 0xe9, 0xee);
    /** A sombra do relevo, no canto inferior. */
    public static final Color COR_FECHADA_SOMBRA = new Color(0x82, 0x88, 0x90);

    /** A célula aberta: as minas vizinhas, o espaço vazio ou a mina. */
    public static final Color COR_ABERTA = new Color(0x5c, 0x62, 0x6a);

    /** O cursor do teclado, que o mouse não usa. */
    public static final Color COR_CURSOR = new Color(0x2f, 0x8f, 0xd2);

    private static final Color COR_TEXTO_HUD = new Color(0xe8, 0xea, 0xed);
    /** A bandeira, vermelha para não parecer com nenhum número. */
    private static final Color COR_BANDEIRA = new Color(0xd0, 0x3a, 0x2b);
    private static final Color COR_MASTRO = new Color(0x2a, 0x2d, 0x31);
    private static final Color COR_MINA = new Color(0x24, 0x26, 0x29);

    /** As cores clássicas dos números, do 1 ao 8. */
    private static final Color[] CORES_NUMERO = {
        new Color(0x1d, 0x4f, 0xd6), // 1 azul
        new Color(0x1c, 0x7a, 0x1c), // 2 verde
        new Color(0xd6, 0x34, 0x34), // 3 vermelho
        new Color(0x1f, 0x2a, 0x6b), // 4 marinho
        new Color(0x6b, 0x22, 0x17), // 5 marrom
        new Color(0x16, 0x5e, 0x5e), // 6 turquesa
        new Color(0x24, 0x26, 0x29), // 7 preto
        new Color(0x6e, 0x72, 0x78), // 8 cinza
    };

    /**
     * A cor de um número de minas vizinhas.
     *
     * @param minas vizinhas, de 1 a 8
     * @return a cor do número, ou {@code null} se não há número válido
     */
    public static Color corDoNumero(int minas) {
        if (minas < 1 || minas > CORES_NUMERO.length) {
            return null;
        }
        return CORES_NUMERO[minas - 1];
    }

    /** Pinta o quadro inteiro, da regra à tela. */
    public void pintar(Graphics2D g2, Tabuleiro tabuleiro, LayoutCampoMinado layout,
            int cursorLinha, int cursorColuna, boolean mostraCursor, int segundos,
            String dificuldade) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(FUNDO);
        g2.fill(layout.janela());

        pintarHud(g2, layout, tabuleiro, segundos, dificuldade);

        for (int linha = 0; linha < layout.getLinhas(); linha++) {
            for (int coluna = 0; coluna < layout.getColunas(); coluna++) {
                Celula celula = tabuleiro.getCelula(linha, coluna);
                pintarCelula(g2, layout.celula(linha, coluna), celula);
            }
        }

        if (mostraCursor) {
            Rectangle r = layout.celula(cursorLinha, cursorColuna);
            g2.setColor(COR_CURSOR);
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
        }
    }

    private void pintarHud(Graphics2D g2, LayoutCampoMinado layout, Tabuleiro tabuleiro,
            int segundos, String dificuldade) {
        int restantes = tabuleiro.getMinas() - tabuleiro.bandeirasUsadas();
        g2.setColor(COR_TEXTO_HUD);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g2.getFontMetrics();
        Rectangle hud = layout.hud();

        String esquerda = "Minas: " + restantes;
        String tempo = "Tempo: " + segundos + "s";
        g2.drawString(esquerda, hud.x + 8, hud.y + (hud.height + fm.getAscent()) / 2);
        g2.drawString(tempo, hud.x + hud.width - fm.stringWidth(tempo) - 8,
                hud.y + (hud.height + fm.getAscent()) / 2);
        g2.drawString(dificuldade, hud.x + (hud.width - fm.stringWidth(dificuldade)) / 2,
                hud.y + (hud.height + fm.getAscent()) / 2);
    }

    private void pintarCelula(Graphics2D g2, Rectangle r, Celula celula) {
        if (celula.isAberto()) {
            pintarAberta(g2, r, celula);
            return;
        }
        pintarFechada(g2, r, celula);
    }

    private void pintarFechada(Graphics2D g2, Rectangle r, Celula celula) {
        g2.setColor(COR_FECHADA);
        g2.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        g2.setColor(COR_FECHADA_BRILHO);
        g2.drawLine(r.x + 2, r.y + 2, r.x + r.width - 3, r.y + 2);
        g2.drawLine(r.x + 2, r.y + 2, r.x + 2, r.y + r.height - 3);
        g2.setColor(COR_FECHADA_SOMBRA);
        g2.drawLine(r.x + 2, r.y + r.height - 3, r.x + r.width - 3, r.y + r.height - 3);
        g2.drawLine(r.x + r.width - 3, r.y + 2, r.x + r.width - 3, r.y + r.height - 3);
        if (celula.isMarcado()) {
            pintarBandeira(g2, r);
        }
    }

    private void pintarBandeira(Graphics2D g2, Rectangle r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        g2.setColor(COR_MASTRO);
        g2.fillRect(cx - 1, r.y + 6, 2, r.height - 12);
        g2.setColor(COR_BANDEIRA);
        // o pano da bandeira no canto superior direito do mastro, sem encostar
        // nas cores que já estão pintadas por baixo do relevo
        g2.fillPolygon(new int[] {cx, cx + r.width / 2 - 2, cx},
                new int[] {r.y + 6, cy - 2, cy - 2}, 3);
    }

    private void pintarAberta(Graphics2D g2, Rectangle r, Celula celula) {
        g2.setColor(COR_ABERTA);
        g2.fillRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        if (celula.isMinado()) {
            pintarMina(g2, r);
            return;
        }
        int vizinhos = celula.minasNaVizinhanca();
        if (vizinhos == 0) {
            return;
        }
        g2.setColor(corDoNumero(vizinhos));
        g2.setFont(new Font("SansSerif", Font.BOLD, r.width - 10));
        FontMetrics fm = g2.getFontMetrics();
        String numero = String.valueOf(vizinhos);
        int x = r.x + (r.width - fm.stringWidth(numero)) / 2;
        int y = r.y + (r.height + fm.getAscent()) / 2;
        g2.drawString(numero, x, y);
    }

    private void pintarMina(Graphics2D g2, Rectangle r) {
        int cx = r.x + r.width / 2;
        int cy = r.y + r.height / 2;
        int raio = r.width / 3;
        g2.setColor(COR_MINA);
        int raioEspinho = raio * 5 / 3;
        for (int i = 0; i < 8; i++) {
            double angulo = Math.PI / 4 * i;
            g2.fillOval(cx + (int) Math.round(Math.cos(angulo) * raioEspinho) - 1,
                    cy + (int) Math.round(Math.sin(angulo) * raioEspinho) - 1, 3, 3);
        }
        g2.fillOval(cx - raio, cy - raio, raio * 2, raio * 2);
        g2.setColor(new Color(0x8d, 0x92, 0x98));
        g2.fillOval(cx - raio * 2 / 3, cy - raio * 2 / 3, raio * 4 / 3, raio * 4 / 3);
    }
}