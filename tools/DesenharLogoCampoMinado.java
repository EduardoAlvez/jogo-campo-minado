import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * Desenha o logo mestre do Campo Minado, o {@code logo-256.png}.
 *
 * <p>O mestre é gerado por código em vez de feito à mão pelo mesmo motivo do
 * Snake: um PNG binário que ninguém sabe refazer é defeito esperando — o
 * desenho é um frame do jogo, montado com a paleta que o jogo já usa.
 *
 * <p>Cores de {@code DesenhoCampoMinado}: fundo {@code 0x181b20 -> 0x262c35},
 * célula fechada {@code COR_FECHADA} com brilho e sombra do relevo, célula
 * aberta {@code COR_ABERTA} com o "1" azul de {@code CORES_NUMERO}, bandeira
 * {@code COR_BANDEIRA} sobre o mastro {@code COR_MASTRO}.
 *
 * <p>O fundo é um retângulo arredondado; fora dele tudo fica com alpha 0, e é
 * essa transparência que o {@code GerarLogo} transforma na máscara AND do
 * {@code .ico} e que o teste confere no arquivo final.
 *
 * <p>Uso: {@code java tools/DesenharLogoCampoMinado.java}
 */
public class DesenharLogoCampoMinado {

    private static final int LADO = 256;
    private static final int MARGEM_FUNDO = 12;
    private static final int RAIO_CANTO = 48;

    private static final Color FUNDO_TOPO = new Color(0x18, 0x1b, 0x20);
    private static final Color FUNDO_BASE = new Color(0x26, 0x2c, 0x35);
    private static final Color FECHADA = new Color(0xbc, 0xc1, 0xc9);
    private static final Color BRILHO = new Color(0xe6, 0xe9, 0xee);
    private static final Color SOMBRA = new Color(0x82, 0x88, 0x90);
    private static final Color ABERTA = new Color(0x5c, 0x62, 0x6a);
    private static final Color AZUL_UM = new Color(0x1d, 0x4f, 0xd6);
    private static final Color BANDEIRA = new Color(0xd0, 0x3a, 0x2b);
    private static final Color MASTRO = new Color(0x2a, 0x2d, 0x31);

    private static final int CELULA = 52;
    private static final int VAO = 10;

    public static void main(String[] args) throws IOException {
        Path recursos = Path.of(args.length > 0 ? args[0] : "src/main/resources");
        BufferedImage logo = desenhar();

        Path destino = recursos.resolve("logo-" + LADO + ".png");
        if (!ImageIO.write(logo, "png", destino.toFile())) {
            throw new IOException("ImageIO nao tem writer de PNG");
        }
        System.out.println("Mestre: " + destino + "  " + LADO + "x" + LADO
                + "  " + Files.size(destino) + " bytes");
    }

    private static BufferedImage desenhar() {
        BufferedImage imagem = new BufferedImage(LADO, LADO, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setPaint(new GradientPaint(0, MARGEM_FUNDO, FUNDO_TOPO, 0, LADO - MARGEM_FUNDO,
                FUNDO_BASE));
        g2.fill(new RoundRectangle2D.Double(MARGEM_FUNDO, MARGEM_FUNDO,
                LADO - 2 * MARGEM_FUNDO, LADO - 2 * MARGEM_FUNDO, RAIO_CANTO, RAIO_CANTO));

        // Uma grade 3x3 como a do jogo: a do meio aberta com "1", a embaixo
        // dela com a bandeira, as outras fechadas com o relevo.
        int total = 3 * CELULA + 2 * VAO;
        int origem = (LADO - total) / 2;
        for (int linha = 0; linha < 3; linha++) {
            for (int coluna = 0; coluna < 3; coluna++) {
                int x = origem + coluna * (CELULA + VAO);
                int y = origem + linha * (CELULA + VAO);
                pintarCelula(g2, x, y,
                        linha == 0 && coluna == 1,
                        linha == 1 && coluna == 1);
            }
        }

        g2.dispose();
        return imagem;
    }

    private static void pintarCelula(Graphics2D g2, int x, int y, boolean aberta,
            boolean comBandeira) {
        if (aberta) {
            g2.setColor(ABERTA);
            g2.fillRoundRect(x + 3, y + 3, CELULA - 6, CELULA - 6, 10, 10);
            g2.setColor(AZUL_UM);
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            String um = "1";
            int largura = g2.getFontMetrics().stringWidth(um);
            int dy = (g2.getFontMetrics().getAscent()
                    - g2.getFontMetrics().getDescent()) / 2;
            g2.drawString(um, x + (CELULA - largura) / 2, y + CELULA / 2 + dy);
            return;
        }

        g2.setColor(FECHADA);
        g2.fillRoundRect(x + 3, y + 3, CELULA - 6, CELULA - 6, 10, 10);
        g2.setColor(BRILHO);
        g2.setStroke(new java.awt.BasicStroke(3));
        g2.drawLine(x + 4, y + 5, x + CELULA - 6, y + 5);
        g2.drawLine(x + 5, y + 4, x + 5, y + CELULA - 6);
        g2.setColor(SOMBRA);
        g2.drawLine(x + 4, y + CELULA - 6, x + CELULA - 6, y + CELULA - 6);
        g2.drawLine(x + CELULA - 6, y + 4, x + CELULA - 6, y + CELULA - 6);

        if (comBandeira) {
            int cx = x + CELULA / 2;
            int cy = y + CELULA / 2;
            g2.setColor(MASTRO);
            g2.fillRect(cx - 2, y + 8, 4, CELULA - 16);
            g2.setColor(BANDEIRA);
            g2.fillPolygon(new int[] {cx, cx + 20, cx},
                    new int[] {y + 8, cy - 6, cy - 6}, 3);
        }
    }
}