import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gera os quatro efeitos sonoros do Campo Minado, por código.
 *
 * <p>O mesmo argumento do logo: um {@code .wav} binário que ninguém sabe
 * refazer é defeito esperando. Aqui cada som é uma fórmula, e o build não
 * precisa de nenhum arquivo externo para recriá-los.
 *
 * <p><b>PCM 16 bits, mono, 44 100 Hz</b> — o formato mais simples que o
 * {@code javax.sound} lê sem depender de codec.
 *
 * <p><b>O envelope é a parte que importa.</b> Uma onda que começa ou termina
 * longe de zero solta um estalo na transição: o clique aparece no começo e no
 * fim de cada som, e o teste {@code SonsTest} confere isso nos arquivos
 * gerados.
 */
public final class GerarSons {

    /** Taxa de amostragem. 44 100 Hz é o padrão do Java Sound. */
    private static final int TAXA = 44100;

    /**
     * Amplitude máxima. Fica abaixo de 1 de propósito: em 1.0 o pico vira
     * 32767 e distorce.
     */
    private static final double TETO = 0.89;

    private GerarSons() {
    }

    public static void main(String[] args) throws IOException {
        Path saida = args.length > 0
                ? Paths.get(args[0])
                : Paths.get("src", "main", "resources", "sons");
        Files.createDirectories(saida);

        grava(saida.resolve("abrir.wav"), abrir());
        grava(saida.resolve("marcar.wav"), marcar());
        grava(saida.resolve("explodir.wav"), explodir());
        grava(saida.resolve("vencer.wav"), vencer());
    }

    // ------------------------------------------------------------------
    // Os quatro sons
    // ------------------------------------------------------------------

    /**
     * Abrir uma célula: o toque curto que sobe um pouco.
     *
     * <p>É o som que mais repete — uma cascata pode abrir dezenas em sequência.
     * Por isso é o mais curto (60 ms) e o mais quieto dos quatro.
     */
    private static double[] abrir() {
        int n = amostras(0.060);
        double[] saida = new double[n];
        double inicio = 660, fim = 880;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = inicio + (fim - inicio) * t;
            saida[i] = Math.sin(2 * Math.PI * f * i / TAXA)
                    * envelope(i, n, 0.03, 0.45) * 0.35;
        }
        return saida;
    }

    /**
     * Marcar (ou desmarcar) uma bandeira: as duas notas graves que sobem.
     *
     * <p>Uma nota só seria igual ao som de abrir; o par grave dá a sensação de
     * "encai­xar" a bandeira, e o mesmo som serve para tirar.
     */
    private static double[] marcar() {
        int n = amostras(0.130);
        double[] saida = new double[n];
        double[] notas = {220.00, 329.63};
        int porNota = n / notas.length;
        for (int i = 0; i < n; i++) {
            int qual = Math.min(notas.length - 1, i / porNota);
            int dentro = i - qual * porNota;
            double f = notas[qual];
            saida[i] = Math.sin(2 * Math.PI * f * i / TAXA)
                    * envelope(dentro, porNota, 0.08, 0.45) * 0.5;
        }
        return saida;
    }

    /**
     * Explodir: o baque grave com a correria de ruído na frente.
     *
     * <p>O ruído é um LCG de propósito: o mesmo código produz sempre o mesmo
     * arquivo. É o som de fim da partida — o mais encorpado dos três.
     */
    private static double[] explodir() {
        int n = amostras(0.420);
        double[] saida = new double[n];
        Lcg ruido = new Lcg(20261007L);
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double grave = Math.sin(2 * Math.PI * 90 * i / TAXA);
            double estalo = t < 0.30 ? ruido.proximo() * (1 - t / 0.30) * 0.6 : 0;
            saida[i] = (grave * 0.9 + estalo) * envelope(i, n, 0.01, 0.75) * 0.7;
        }
        return saida;
    }

    /**
     * Vencer: o arpejo ascendente de dó, mi, sol.
     *
     * <p>Quatrocentos e cinquenta milissegundos de três notas que sobem dizem
     * "conseguiu" sem depender de texto, e não se confundem com a derrota — o
     * baque da explosão desce, o arpejo sobe.
     */
    private static double[] vencer() {
        int n = amostras(0.450);
        double[] saida = new double[n];
        double[] notas = {523.25, 659.25, 783.99}; // dó, mi, sol
        int porNota = n / notas.length;
        for (int i = 0; i < n; i++) {
            int qual = Math.min(notas.length - 1, i / porNota);
            int dentro = i - qual * porNota;
            double f = notas[qual];
            double cauda = qual == notas.length - 1 ? 0.70 : 0.30;
            saida[i] = Math.sin(2 * Math.PI * f * i / TAXA)
                    * envelope(dentro, porNota, 0.08, cauda) * 0.5
                    + Math.sin(2 * Math.PI * f * 2 * i / TAXA)
                    * envelope(dentro, porNota, 0.08, cauda) * 0.15;
        }
        return saida;
    }

    // ------------------------------------------------------------------
    // Ferramentas de síntese
    // ------------------------------------------------------------------

    private static int amostras(double segundos) {
        return (int) Math.round(segundos * TAXA);
    }

    /**
     * Ataque e decaimento, com ataque e cauda em zero.
     *
     * <p>Os dois extremos em zero é o que impede o clique: a amostra 0 e a
     * última saem praticamente nulas, então a onda começa e termina no silêncio.
     */
    private static double envelope(int i, int total, double ataque, double cauda) {
        int ataqueN = Math.max(1, (int) Math.round(ataque * total));
        int caudaN = Math.max(1, (int) Math.round(cauda * total));
        if (i < ataqueN) {
            return i / (double) ataqueN;
        }
        int desdeFim = total - 1 - i;
        if (desdeFim < caudaN) {
            return desdeFim / (double) caudaN;
        }
        return 1.0;
    }

    /** Gerador linear congruente, para o ruído ser reprodutível. */
    private static final class Lcg {
        private long estado;

        Lcg(long semente) {
            this.estado = semente;
        }

        double proximo() {
            estado = (estado * 6364136223846793005L + 1442695040888963407L);
            return ((estado >> 11) / (double) (1L << 53)) * 2 - 1;
        }
    }

    // ------------------------------------------------------------------
    // Escrita do WAV
    // ------------------------------------------------------------------

    /**
     * Grava um WAV PCM de 16 bits, mono, com os tamanhos do cabeçalho escritos
     * à mão — para o formato não depender do que a máquina tem instalado.
     */
    private static void grava(Path arquivo, double[] amostras) throws IOException {
        double pico = 0;
        for (double a : amostras) {
            pico = Math.max(pico, Math.abs(a));
        }
        double escala = pico > TETO ? TETO / pico : 1.0;

        int dados = amostras.length * 2;
        try (OutputStream saida = Files.newOutputStream(arquivo)) {
            DataOutputStream d = new DataOutputStream(saida);
            d.writeBytes("RIFF");
            writeLE32(d, 36 + dados);
            d.writeBytes("WAVE");
            d.writeBytes("fmt ");
            writeLE32(d, 16);
            writeLE16(d, 1); // PCM sem compressão
            writeLE16(d, 1); // mono
            writeLE32(d, TAXA);
            writeLE32(d, TAXA * 2); // bytes por segundo
            writeLE16(d, 2); // bytes por amostra entrelaçada
            writeLE16(d, 16); // bits por amostra
            d.writeBytes("data");
            writeLE32(d, dados);
            for (double a : amostras) {
                int v = (int) Math.round(a * escala * 32767);
                v = Math.max(-32768, Math.min(32767, v));
                writeLE16(d, v);
            }
        }
        System.out.printf("%-12s %5d amostras  %6.1f ms  pico %.2f%n",
                arquivo.getFileName(), amostras.length, amostras.length * 1000.0 / TAXA,
                pico * escala);
    }

    private static void writeLE16(DataOutputStream d, int v) throws IOException {
        d.write(v & 0xFF);
        d.write((v >> 8) & 0xFF);
    }

    private static void writeLE32(DataOutputStream d, int v) throws IOException {
        d.write(v & 0xFF);
        d.write((v >> 8) & 0xFF);
        d.write((v >> 16) & 0xFF);
        d.write((v >> 24) & 0xFF);
    }
}