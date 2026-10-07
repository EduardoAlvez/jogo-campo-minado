package com.portfolio.campominado.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * O melhor tempo de cada dificuldade, guardado num arquivo na pasta do usuário.
 *
 * <p>Um arquivo de propriedades com um número por dificuldade, sem banco. A
 * gravação é atômica: escreve num arquivo temporário e só então renomeia por
 * cima do verdadeiro — um desligamento no meio do caminho deixa o arquivo
 * antigo ou o novo, nunca um pela metade. A leitura é defensiva: arquivo
 * corrompido devolve zero, e o jogo continua jogável.
 */
public final class RegistroDeTempos {

    private static final String NOME_ARQUIVO = ".jogo-campo-minado-tempos";
    private static final int TEMPO_INICIAL = 0;

    private RegistroDeTempos() {
    }

    private static Path caminho() {
        return Paths.get(System.getProperty("user.home", "."), NOME_ARQUIVO);
    }

    /**
     * O melhor tempo da dificuldade, em segundos, ou zero se nunca houve um.
     *
     * @param dificuldade a dificuldade consultada
     * @return o menor tempo já registrado, ou zero
     */
    public static int melhorTempo(Dificuldade dificuldade) {
        try {
            Path p = caminho();
            if (!Files.exists(p)) {
                return TEMPO_INICIAL;
            }
            Properties tempos = new Properties();
            try (InputStream entrada = Files.newInputStream(p)) {
                tempos.load(entrada);
            }
            String valor = tempos.getProperty(chave(dificuldade));
            if (valor == null) {
                return TEMPO_INICIAL;
            }
            return Math.max(TEMPO_INICIAL, Integer.parseInt(valor.trim()));
        } catch (IOException | RuntimeException e) {
            return TEMPO_INICIAL;
        }
    }

    /**
     * Grava o tempo se ele for menor que o melhor já salvo.
     *
     * @param dificuldade a dificuldade da partida
     * @param segundos    o tempo da partida que acabou
     * @return {@code true} se foi um novo recorde
     */
    public static boolean registrar(Dificuldade dificuldade, int segundos) {
        int atual = melhorTempo(dificuldade);
        if (segundos < TEMPO_INICIAL || (atual > TEMPO_INICIAL && segundos >= atual)) {
            return false;
        }
        try {
            Path p = caminho();
            Properties tempos = new Properties();
            if (Files.exists(p)) {
                try (InputStream entrada = Files.newInputStream(p)) {
                    tempos.load(entrada);
                }
            }
            tempos.setProperty(chave(dificuldade), String.valueOf(segundos));
            gravarAtomicamente(p, tempos);
            return true;
        } catch (IOException | RuntimeException e) {
            // Sem permissão de escrita, por exemplo: o jogo continua sem salvar.
            return false;
        }
    }

    /** Apaga o arquivo. Só para os testes. */
    static void apagar() {
        try {
            Files.deleteIfExists(caminho());
        } catch (IOException | RuntimeException e) {
            // Sem efeito: apagar um arquivo que não existe não é erro.
        }
    }

    private static String chave(Dificuldade dificuldade) {
        return dificuldade.name().toLowerCase();
    }

    /**
     * Escreve num arquivo temporário e renomeia por cima do definitivo.
     *
     * <p>O {@link StandardCopyOption#ATOMIC_MOVE} garante a troca no mesmo
     * momento; quando o sistema de arquivos não o suporta, a queda volta para o
     * rename comum, que ainda é a melhor das duas opções sobre a escrita direta.
     */
    private static void gravarAtomicamente(Path destino, Properties tempos) throws IOException {
        Path temporario = destino.resolveSibling(destino.getFileName() + ".tmp");
        try (OutputStream saida = Files.newOutputStream(temporario)) {
            tempos.store(saida, "melhor tempo em segundos por dificuldade");
        }
        try {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}