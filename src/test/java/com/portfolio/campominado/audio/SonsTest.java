package com.portfolio.campominado.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.portfolio.campominado.audio.Sons.Efeito;

/**
 * Os sons, conferidos no <b>produto</b>: o arquivo que o jogo carrega, não o
 * gerador que o produziu.
 *
 * <p>{@code tools/} não entra no build, então um teste do {@code GerarSons}
 * passaria com o que está no disco errado. Aqui tudo é lido pelo classpath e,
 * no caso mais importante, <b>de dentro de um jar de verdade</b>.
 *
 * <p><b>Nenhum teste aqui diz que o som soa bem.</b> Isso exige ouvido humano.
 * O que dá para medir sem ears é se a onda está bem formada: não muda, não
 * clipada, começando e terminando em zero, e os quatro sons diferentes entre si.
 */
public class SonsTest {

    @Rule
    public TemporaryFolder pasta = new TemporaryFolder();

    /** Tamanho do cabeçalho WAV PCM, sem blocos extras. */
    private static final int CABECALHO = 44;

    /** Amostra considerada "não muda": RMS acima disto já é som. */
    private static final double RMS_MINIMO = 0.01;

    // ------------------------------------------------------------------
    // O produto está no classpath
    // ------------------------------------------------------------------

    @Test
    public void osQuatroSonsEstaoNoClasspath() {
        for (Efeito e : Efeito.values()) {
            assertNotNull("o recurso /sons/" + e.getArquivo() + " nao esta no classpath",
                    Sons.class.getResourceAsStream("/sons/" + e.getArquivo()));
        }
    }

    /**
     * O teste que o Pong pagou para ter — e desta vez ele roda o
     * <b>{@code Sons} de verdade</b>.
     *
     * <p>O jar leva a classe compilada junto com os {@code .wav}, e ela é
     * carregada por um {@code URLClassLoader} cujo pai é o
     * <b>platform loader</b>. Assim {@code javax.sound} resolve,
     * {@code target/classes} fica invisível, e o {@code Sons} que roda é o do
     * jar: os recursos dele saem de entradas de jar, que não suportam
     * {@code mark}.
     *
     * <p>Esse detalhe é o cerne do defeito do Pong: de {@code target/classes} o
     * JDK devolve um fluxo com {@code mark} funcionando, e o teste passa. Só de
     * dentro do jar a ausência de {@code mark} aparece.
     */
    @Test
    public void oSonsDeVerdadeCarregaOsSonsDeDentroDeUmJar() throws Exception {
        File jar = jarComOsSonsEOClass();
        try (URLClassLoader cl = new URLClassLoader(
                new URL[]{jar.toURI().toURL()}, ClassLoader.getPlatformClassLoader())) {

            Class<?> sons = cl.loadClass("com.portfolio.campominado.audio.Sons");
            Class<?> efeito = cl.loadClass("com.portfolio.campominado.audio.Sons$Efeito");
            java.lang.reflect.Method carregar = sons.getMethod("carregar", efeito);

            for (Object e : efeito.getEnumConstants()) {
                byte[] dados = (byte[]) carregar.invoke(null, e);
                assertNotNull("o Sons do jar nao carregou " + e, dados);
                assertTrue("o som saiu mudo do jar: " + e, rms(dados) > RMS_MINIMO);
            }
        }
    }

    @Test
    public void semBufferALeituraDoJarFalha() throws Exception {
        File jar = jarComOsSons();
        try (URLClassLoader cl = new URLClassLoader(new URL[]{jar.toURI().toURL()}, null);
             InputStream cru = cl.getResourceAsStream(nomeRelativo(Efeito.ABRIR))) {
            assertTrue("este teste so tem valor se o fluxo cru nao suportar mark/reset",
                    cru != null && !cru.markSupported());
            try {
                AudioInputStream a = AudioSystem.getAudioInputStream(cru);
                AudioSystem.getAudioFileFormat(a);
                fail("leu de um jar sem buffer sem falhar: "
                        + "ou o JDK mudou, ou o teste acima parou de provar o que "
                        + "prova e precisa ser revisto");
            } catch (IOException esperada) {
                assertTrue("a falha tem de ser a de mark/reset, e nao outra coisa",
                        String.valueOf(esperada.getMessage()).contains("mark/reset"));
            }
        }
    }

    // ------------------------------------------------------------------
    // A onda está bem formada
    // ------------------------------------------------------------------

    @Test
    public void começaETerminaEmZero() {
        for (Efeito e : Efeito.values()) {
            double[] amostras = leSamples(e);
            assertEquals("o som " + e + " comeca longe do silencio", 0.0,
                    Math.abs(amostras[0]), 0.002);
            assertEquals("o som " + e + " termina longe do silencio", 0.0,
                    Math.abs(amostras[amostras.length - 1]), 0.002);
        }
    }

    @Test
    public void nenhumSomEstaMudo() {
        for (Efeito e : Efeito.values()) {
            assertTrue("o som " + e + " esta mudo", rms(e) > RMS_MINIMO);
        }
    }

    @Test
    public void nenhumSomSaiClipado() {
        for (Efeito e : Efeito.values()) {
            for (double a : leSamples(e)) {
                assertTrue("o som " + e + " clipou em " + a,
                        a > -0.99 && a < 0.99);
            }
        }
    }

    @Test
    public void osQuatroSonsSaoDiferentesEntreSi() {
        List<String> assinaturas = new ArrayList<>();
        for (Efeito e : Efeito.values()) {
            double[] s1 = leSamples(e);
            assinaturas.add(s1[100] + "," + s1[200] + "," + s1[300]);
        }
        for (int i = 0; i < assinaturas.size(); i++) {
            for (int j = i + 1; j < assinaturas.size(); j++) {
                String s = "os dois sons saem iguais: "
                        + Efeito.values()[i] + " e " + Efeito.values()[j];
                assertFalse(s, assinaturas.get(i).equals(assinaturas.get(j)));
            }
        }
    }

    // ------------------------------------------------------------------
    // Ferramentas do teste
    // ------------------------------------------------------------------

    private static String caminhoDe(Efeito e) {
        return "/sons/" + e.getArquivo();
    }

    /**
     * Nome relativo (sem a barra inicial): é o que um
     * {@code URLClassLoader} resolve de dentro de um jar — com a barra, ele
     * devolve {@code null}. Só o classpath de verdade aceita o caminho com
     * barra.
     */
    private static String nomeRelativo(Efeito e) {
        return "sons/" + e.getArquivo();
    }

    private static double rms(Efeito e) {
        return rms(leBytesBrutos(e));
    }

    private static double rms(byte[] dados) {
        double soma = 0;
        int n = 0;
        for (double s : leSamples(dados)) {
            soma += s * s;
            n++;
        }
        return Math.sqrt(soma / n);
    }

    private static double[] leSamples(Efeito e) {
        return leSamples(leBytesBrutos(e));
    }

    private static byte[] leBytesBrutos(Efeito e) {
        try (InputStream in = Sons.class.getResourceAsStream(caminhoDe(e))) {
            assertNotNull(in);
            return in.readAllBytes();
        } catch (IOException problema) {
            throw new AssertionError(problema);
        }
    }

    /** 16 bits PCM mono: transforma os samples em um {@code double} por amostra. */
    private static double[] leSamples(byte[] wav) {
        int n = (wav.length - CABECALHO) / 2;
        double[] saida = new double[n];
        for (int i = 0; i < n; i++) {
            int b0 = wav[CABECALHO + i * 2] & 0xFF;
            int b1 = wav[CABECALHO + i * 2 + 1] & 0xFF;
            short v = (short) (b0 | (b1 << 8));
            saida[i] = v / 32768.0;
        }
        return saida;
    }

    // ------------------------------------------------------------------
    // O jar com os sons (e a classe) de verdade
    // ------------------------------------------------------------------

    private File jarComOsSons() throws IOException {
        return jar(complementaComSons(false), pasta.getRoot().toPath(), "sons.wav.jar");
    }

    private File jarComOsSonsEOClass() throws IOException {
        return jar(complementaComSons(true), pasta.getRoot().toPath(), "sons.class.jar");
    }

    private File jar(List<Path> entradas, Path destino, String nome) throws IOException {
        File jar = destino.resolve(nome).toFile();
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(jar.toPath()))) {
            for (Path p : entradas) {
                Path classes = Paths.get(System.getProperty("user.dir"), "target", "classes");
                String nomeEntrada = classes.relativize(p).toString().replace('\\', '/');
                z.putNextEntry(new ZipEntry(nomeEntrada));
                Files.copy(p, z);
                z.closeEntry();
            }
        }
        return jar;
    }

    /** Os WAV de {@code target/classes/sons}, e a classe {@code Sons} se pedido. */
    private List<Path> complementaComSons(boolean comClasse) {
        Path classes = Paths.get(System.getProperty("user.dir"), "target", "classes");
        List<Path> entradas = new ArrayList<>();
        for (Efeito e : Efeito.values()) {
            entradas.add(classes.resolve("sons").resolve(e.getArquivo()));
        }
        if (comClasse) {
            for (Path arquivo : listar(classes.resolve("com").resolve("portfolio")
                    .resolve("campominado").resolve("audio"))) {
                if (arquivo.getFileName().toString().startsWith("Sons")) {
                    entradas.add(arquivo);
                }
            }
        }
        return entradas;
    }

    private List<Path> listar(Path dir) {
        try (java.util.stream.Stream<Path> arquivos = Files.list(dir)) {
            return arquivos.toList();
        } catch (IOException problema) {
            throw new AssertionError("target/classes sem a pasta de audio: "
                    + problema.getMessage());
        }
    }
}