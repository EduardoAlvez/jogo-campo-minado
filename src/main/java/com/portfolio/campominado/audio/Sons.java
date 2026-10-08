package com.portfolio.campominado.audio;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Os efeitos sonoros, e as três coisas que esta classe existe para garantir.
 *
 * <p><b>1. O som nunca derruba o jogo.</b> Tudo aqui é engolido: placa de som
 * ausente, recurso faltando, arquivo corrompido. Um jogo de portfólio que abre
 * uma janela de erro porque a máquina não tem placa de áudio não é um jogo com
 * defeito de som, é um jogo quebrado. Por isso {@link #tocar} não declara
 * nenhuma exceção checada.
 *
 * <p><b>2. O decoder precisa de mark/reset, e o fluxo de um jar não tem.</b>
 * É o defeito que o Pong pagou: {@code AudioSystem.getAudioInputStream}
 * chama {@code mark} e {@code reset}, e a entrada de um jar não suporta
 * nenhum dos dois — o sintoma é {@code IOException: mark/reset not supported}.
 * O que engana é que <b>de {@code target/classes} funciona mesmo sem buffer</b>
 * (o JDK devolve ali um fluxo com {@code mark} funcionando, porque é um
 * diretório). Só de dentro do jar a ausência aparece.
 *
 * <p>Por isso os bytes são lidos para a memória com buffer explícito e a
 * decodificação acontece sobre um {@code ByteArrayInputStream}, que já
 * suporta {@code mark/reset}. O {@code SonsTest} carrega o <b>próprio
 * {@code Sons}</b> de dentro de um jar e exige que funcione.
 *
 * <p><b>3. O laço do jogo não para.</b> Abrir um {@code Clip} por toque é o
 * preço de não carregar o fluxo de áudio inteiro; em troca cada toque é
 * independente e nenhum som segura a EDT.
 *
 * <p>Nenhuma asserção diz que o som "soa bem" — isso exige ouvido. O que os
 * testes verificam é o que dá para medir: que o arquivo decodifica de dentro
 * de um jar, que não está mudo, que não está clipado e que começa e termina
 * em zero.
 */
public final class Sons {

    /** Os quatro efeitos, com o arquivo que cada um carrega. */
    public enum Efeito {

        /** Abrir uma célula: o toque curto. */
        ABRIR("abrir.wav"),

        /** Plantar ou tirar a bandeira: as duas notas graves. */
        MARCAR("marcar.wav"),

        /** Uma mina explodiu: o baque com ruído. */
        EXPLODIR("explodir.wav"),

        /** A partida inteira foi varrida: o arpejo que sobe. */
        VENCER("vencer.wav");

        private final String arquivo;

        Efeito(String arquivo) {
            this.arquivo = arquivo;
        }

        /** Nome do recurso dentro de {@code /sons}. */
        public String getArquivo() {
            return arquivo;
        }
    }

    /** Onde os sons vivem no classpath. */
    private static final String CAMINHO = "/sons/";

    /** Tamanho do cabeçalho de um WAV PCM sem blocos extras: 44 bytes. */
    private static final int CABECALHO_WAV = 44;

    /** Bytes já decodificados, por efeito. */
    private static final Map<Efeito, byte[]> CACHE = new EnumMap<>(Efeito.class);

    /** Efeitos que já falharam uma vez, para não tentar de novo a cada toque. */
    private static final Map<Efeito, Boolean> FALHOU = new EnumMap<>(Efeito.class);

    /** Silenciado pelo jogador. */
    private static volatile boolean mudo;

    /** Ligado na primeira falha de dispositivo, para o jogo não insistir. */
    private static volatile boolean semDispositivo;

    private Sons() {
    }

    /**
     * Carrega os quatro efeitos uma vez, para o primeiro som não pagar a leitura.
     *
     * <p>Síncrono e direto: é ler 102 KB e parsear quatro cabeçalhos WAV,
     * trabalho de milissegundos. O que é caro — abrir a linha de áudio —
     * acontece em {@link #tocar}, na EDT, e continua lá.
     */
    public static void carregarTodos() {
        for (Efeito e : Efeito.values()) {
            carregar(e);
        }
    }

    /**
     * Devolve os bytes do efeito, decodificando uma única vez. Nunca lança.
     *
     * @param efeito o efeito desejado
     * @return os bytes do WAV, ou {@code null}
     */
    public static byte[] carregar(Efeito efeito) {
        if (efeito == null || semDispositivo) {
            return null;
        }
        byte[] jaPronto = CACHE.get(efeito);
        if (jaPronto != null) {
            return jaPronto;
        }
        if (Boolean.TRUE.equals(FALHOU.get(efeito))) {
            return null;
        }
        byte[] dados = leDoClasspath(efeito);
        if (dados == null) {
            FALHOU.put(efeito, Boolean.TRUE);
            return null;
        }
        CACHE.put(efeito, dados);
        return dados;
    }

    /**
     * Lê o WAV inteiro e confirma que ele decodifica, com buffer desde a
     * primeira linha — é o que faz a leitura funcionar de dentro do jar, que é
     * como o jogo roda no {@code .exe}.
     */
    private static byte[] leDoClasspath(Efeito efeito) {
        byte[] bruto = leBytes(CAMINHO + efeito.getArquivo());
        if (bruto == null || bruto.length <= CABECALHO_WAV) {
            return null;
        }
        try (InputStream comBuffer = new BufferedInputStream(new ByteArrayInputStream(bruto));
             AudioInputStream a = AudioSystem.getAudioInputStream(comBuffer)) {
            return a.getFormat() == null ? null : bruto;
        } catch (IOException | UnsupportedAudioFileException | RuntimeException e) {
            return null;
        }
    }

    /** Lê um recurso do classpath inteiro, com buffer. */
    private static byte[] leBytes(String recurso) {
        try (InputStream bruto = Sons.class.getResourceAsStream(recurso)) {
            if (bruto == null) {
                return null;
            }
            try (InputStream comBuffer = new BufferedInputStream(bruto)) {
                ByteArrayOutputStream saida = new ByteArrayOutputStream();
                byte[] pedaco = new byte[8192];
                int lido;
                while ((lido = comBuffer.read(pedaco, 0, pedaco.length)) > 0) {
                    saida.write(pedaco, 0, lido);
                }
                return saida.toByteArray();
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * Toca um efeito. Não faz nada se estiver mudo, sem dispositivo, ou se o
     * efeito não carregou.
     */
    public static void tocar(Efeito efeito) {
        if (efeito == null || mudo || semDispositivo) {
            return;
        }
        byte[] dados = carregar(efeito);
        if (dados == null) {
            return;
        }
        Clip clip = null;
        try {
            clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(
                    new BufferedInputStream(new ByteArrayInputStream(dados))));
            final Clip alvo = clip;
            clip.addLineListener(e -> {
                if (e.getType() == LineEvent.Type.STOP) {
                    alvo.close();
                }
            });
            clip.start();
        } catch (LineUnavailableException e) {
            semDispositivo = true;
            fecha(clip);
        } catch (IOException | UnsupportedAudioFileException
                | IllegalArgumentException | SecurityException e) {
            semDispositivo = true;
            fecha(clip);
        }
    }

    private static void fecha(Clip clip) {
        if (clip != null && clip.isOpen()) {
            try {
                clip.close();
            } catch (RuntimeException ignorada) {
                // fechar um clip que já morreu não pode virar erro
            }
        }
    }

    /** Liga ou desliga o som. O jogo começa ligado. */
    public static void mudo(boolean valor) {
        mudo = valor;
    }

    /** {@code true} se o jogador silenciou. */
    public static boolean isMudo() {
        return mudo;
    }

    /** {@code true} enquanto o áudio puder tocar. */
    public static boolean disponivel() {
        return !semDispositivo && !mudo;
    }
}