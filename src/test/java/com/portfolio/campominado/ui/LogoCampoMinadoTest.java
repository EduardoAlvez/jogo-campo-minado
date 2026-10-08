package com.portfolio.campominado.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.Test;

/**
 * O ícone do jogo, conferido no <b>produto</b> e não no gerador.
 *
 * <p>O que se testa aqui é o arquivo que o Windows vai ler de verdade: o
 * {@code logo.ico} que o launch4j embute no executável e o {@code logo-256.png}
 * que a janela carrega. As ferramentas que produzem esses arquivos moram em
 * {@code tools/}, fora do {@code src/}, então nem entram no build do Maven —
 * o que importa não é se o gerador funciona, é se o <b>resultado</b> está certo.
 *
 * <p>A quinta asserção é a que dá sentido a esta classe. O {@code .ico} declara
 * transparência por uma <b>máscara AND</b>, na qual o bit 1 significa
 * "transparente" — e a versão dessa ferramenta que veio do Pong escrevia a
 * máscara zerada, o que declara a imagem inteira opaca. Um arquivo assim sai
 * perfeito no log, o executável é gerado, e o defeito só aparece na barra de
 * tarefas. Por isso o teste exige que a entrada de 16px tenha bits de
 * transparência.
 */
public class LogoCampoMinadoTest {

    private static final int[] TAMANHOS_ESPERADOS = {16, 24, 32, 48, 64, 128, 256};
    private static final int CABECALHO_BMP = 40;
    private static final int TAMANHO_ENTRADA_ICO = 16;
    private static final int LIMIAR_ALFA = 128;

    private static final int COR_FECHADA = 0xBCC1C9;
    private static final int COR_ABERTA = 0x5C626A;
    private static final int AZUL_UM = 0x1D4FD6;
    private static final int COR_BANDEIRA = 0xD03A2B;

    // ------------------------------------------------------------------
    // O .ico
    // ------------------------------------------------------------------

    @Test
    public void oIcoDeclaraAsSeteResolucoes() throws IOException {
        byte[] ico = lerRecurso("/logo.ico");

        int tipo = (ico[2] & 0xFF) | ((ico[3] & 0xFF) << 8);
        assertEquals("o tipo do arquivo tem que ser 1 (ícone)", 1, tipo);

        int entradas = (ico[4] & 0xFF) | ((ico[5] & 0xFF) << 8);
        assertEquals("entradas no diretório do .ico", TAMANHOS_ESPERADOS.length, entradas);

        for (int i = 0; i < TAMANHOS_ESPERADOS.length; i++) {
            int pos = 6 + i * TAMANHO_ENTRADA_ICO;
            int largura = ico[pos] & 0xFF;
            // No formato, 0 no cabeçalho significa 256.
            int ladoDeclarado = largura == 0 ? 256 : largura;
            assertEquals("largura da entrada " + i, TAMANHOS_ESPERADOS[i], ladoDeclarado);
            assertEquals("a entrada " + i + " tem que ser uma imagem, não um ponteiro",
                    1, ico[pos + 4] & 0xFF);
            int bits = (ico[pos + 6] & 0xFF) | ((ico[pos + 7] & 0xFF) << 8);
            assertEquals("a entrada " + i + " tem que ser de 32 bpp, para o canal alpha "
                    + "chegar ao Windows", 32, bits);
        }
    }

    @Test
    public void aEntradaPequenaDoIcoDeclaraTransparencia() throws IOException {
        byte[] ico = lerRecurso("/logo.ico");
        byte[] entrada = entradaDoIco(ico, 0);
        int lado = 16;

        int inicioMascara = CABECALHO_BMP + lado * lado * 4;
        int bytesDaLinhaDaMascara = ((lado + 31) / 32) * 4;
        int esperado = CABECALHO_BMP + lado * lado * 4 + bytesDaLinhaDaMascara * lado;
        assertEquals("a entrada BMP tem cor de 32 bpp mais a máscara AND", esperado, entrada.length);

        int bitsTransparentes = 0;
        for (int b = 0; b < entrada.length - inicioMascara; b++) {
            bitsTransparentes += Integer.bitCount(entrada[inicioMascara + b] & 0xFF);
        }
        assertTrue("a máscara AND da entrada de 16px está zerada, então o ícone é "
                + "quadrado e opaco: o bit 1 da máscara é justamente o que declara "
                + "transparência", bitsTransparentes > 0);
    }

    @Test
    public void aMascaraDoIcoConcordaComOAlphaDoPng() throws IOException {
        byte[] ico = lerRecurso("/logo.ico");
        BufferedImage png = ImageIO.read(abrirRecurso("/logo-32.png"));

        byte[] entrada = entradaDoIco(ico, 2);
        int lado = 32;
        int inicioMascara = CABECALHO_BMP + lado * lado * 4;
        int bytesDaLinhaDaMascara = ((lado + 31) / 32) * 4;

        int divergencias = 0;
        // A máscara é BMP, e BMP é de baixo para cima: a primeira linha gravada é a
        // de baixo da imagem.
        for (int y = lado - 1; y >= 0; y--) {
            for (int b = 0; b < bytesDaLinhaDaMascara; b++) {
                for (int bit = 0; bit < 8; bit++) {
                    int x = b * 8 + bit;
                    if (x >= lado) {
                        continue;
                    }
                    int esperado = png.getRGB(x, y) >>> 24 < LIMIAR_ALFA ? 1 : 0;
                    int real = (entrada[inicioMascara + y * bytesDaLinhaDaMascara + b]
                            >> (7 - bit)) & 1;
                    if (esperado != real) {
                        divergencias++;
                    }
                }
            }
        }
        assertEquals("a máscara AND do .ico e o alpha do PNG têm que concordar pixel a "
                + "pixel, senão o executável e a janela mostram recortes diferentes",
                0, divergencias);
    }

    /**
     * O que este teste <b>não</b> prova, e é preciso escrever para não escrever o
     * contrário: com a transparência simétrica na vertical (cantos arredondados
     * iguais em cima e embaixo), inverter a ordem das linhas da máscara produz um
     * arquivo idêntico e esta suíte não pegaria a regressão.
     */
    @Test
    public void aTransparenciaDoLogoEhSimetricaNaVertical() throws IOException {
        BufferedImage imagem = ImageIO.read(abrirRecurso("/logo-256.png"));
        int lado = 256;

        int divergencias = 0;
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                int emCima = imagem.getRGB(x, y) >>> 24;
                int embaixo = imagem.getRGB(x, lado - 1 - y) >>> 24;
                boolean acimaTransparente = emCima < LIMIAR_ALFA;
                boolean abaixoTransparente = embaixo < LIMIAR_ALFA;
                if (acimaTransparente != abaixoTransparente) {
                    divergencias++;
                }
            }
        }
        assertEquals("a transparência deixou de ser simétrica na vertical: isso é bom"
                + " — o recorte agora distingue a ordem das linhas — mas quebre o"
                + " teste confirme invertendo a leitura da máscara e veja-o cair",
                0, divergencias);
    }

    // ------------------------------------------------------------------
    // O PNG mestre
    // ------------------------------------------------------------------

    @Test
    public void oMestreEdaResolucaoDoLogo() throws IOException {
        BufferedImage imagem = ImageIO.read(abrirRecurso("/logo-256.png"));
        assertNotNull("o logo-256.png não está no classpath", imagem);
        assertEquals(256, imagem.getWidth());
        assertEquals(256, imagem.getHeight());
    }

    @Test
    public void oMestreTemAsCoresDoCampoMinado() throws IOException {
        BufferedImage imagem = ImageIO.read(abrirRecurso("/logo-256.png"));

        Set<Integer> cores = new HashSet<>();
        for (int y = 0; y < imagem.getHeight(); y++) {
            for (int x = 0; x < imagem.getWidth(); x++) {
                if ((imagem.getRGB(x, y) >>> 24) >= LIMIAR_ALFA) {
                    cores.add(imagem.getRGB(x, y) & 0x00FFFFFF);
                }
            }
        }

        assertTrue("o logo tem poucas cores distintas, parece um quadrado preenchido: "
                + cores.size(), cores.size() >= 3);
        assertTrue("falta a célula fechada", cores.contains(COR_FECHADA));
        assertTrue("falta a célula aberta", cores.contains(COR_ABERTA));
        assertTrue("falta o azul do número 1", cores.contains(AZUL_UM));
        assertTrue("falta o vermelho da bandeira", cores.contains(COR_BANDEIRA));
    }

    @Test
    public void oMestreTemCantoTransparente() throws IOException {
        BufferedImage imagem = ImageIO.read(abrirRecurso("/logo-256.png"));

        assertEquals("o canto superior esquerdo tem que estar transparente, senão o "
                + "ícone sai quadrado e chapado", 0, imagem.getRGB(0, 0) >>> 24);
        assertEquals("o meio do logo tem que ser opaco", 255,
                imagem.getRGB(128, 128) >>> 24);
    }

    // ------------------------------------------------------------------
    // Apoio
    // ------------------------------------------------------------------

    private static ByteArrayInputStream abrirRecurso(String caminho) throws IOException {
        return new ByteArrayInputStream(lerRecurso(caminho));
    }

    private static byte[] entradaDoIco(byte[] ico, int indice) throws IOException {
        int pos = 6 + indice * TAMANHO_ENTRADA_ICO;
        int tamanho = lerInteiroLe(ico, pos + 8);
        int deslocamento = lerInteiroLe(ico, pos + 12);
        if (deslocamento < 0 || tamanho < 0 || deslocamento + tamanho > ico.length) {
            throw new IOException("entrada " + indice + " do .ico aponta fora do arquivo");
        }
        byte[] entrada = new byte[tamanho];
        System.arraycopy(ico, deslocamento, entrada, 0, tamanho);
        return entrada;
    }

    private static int lerInteiroLe(byte[] dados, int pos) {
        return (dados[pos] & 0xFF)
                | ((dados[pos + 1] & 0xFF) << 8)
                | ((dados[pos + 2] & 0xFF) << 16)
                | ((dados[pos + 3] & 0xFF) << 24);
    }

    private static byte[] lerRecurso(String caminho) throws IOException {
        InputStream entrada = LogoCampoMinadoTest.class.getResourceAsStream(caminho);
        assertNotNull("o recurso " + caminho + " não está no classpath; ele é gerado por "
                + "tools/DesenharLogoCampoMinado.java e tools/GerarLogo.java", entrada);
        try (InputStream in = entrada) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] pedaco = new byte[8192];
            int lidos;
            while ((lidos = in.read(pedaco)) > 0) {
                buffer.write(pedaco, 0, lidos);
            }
            return buffer.toByteArray();
        }
    }
}