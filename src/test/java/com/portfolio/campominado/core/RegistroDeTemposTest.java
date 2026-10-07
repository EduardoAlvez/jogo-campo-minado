package com.portfolio.campominado.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * O arquivo de tempos: gravação atômica, leitura defensiva e contagem
 * separada por dificuldade. A casa do usuário é redirecionada para uma pasta
 * temporária para o teste não sujar o disco (regra 10 da skill).
 */
public class RegistroDeTemposTest {

    private static final String USUARIO_ORIGINAL = System.getProperty("user.home");

    private Path pastaTemporaria;

    @Before
    public void redirecionaCasaDoUsuario() throws IOException {
        pastaTemporaria = Files.createTempDirectory("tempos-test");
        System.setProperty("user.home", pastaTemporaria.toString());
        RegistroDeTempos.apagar();
    }

    @After
    public void restauraCasaDoUsuario() throws IOException {
        System.setProperty("user.home", USUARIO_ORIGINAL);
        deleteRecursivo(pastaTemporaria);
    }

    @Test
    public void comecaSemRecorde() {
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.FACIL));
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.MEDIO));
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.DIFICIL));
    }

    @Test
    public void registraELeDeVolta() {
        assertTrue(RegistroDeTempos.registrar(Dificuldade.MEDIO, 85));
        assertEquals(85, RegistroDeTempos.melhorTempo(Dificuldade.MEDIO));
    }

    @Test
    public void soSubstituiPorUmTempoMenor() {
        RegistroDeTempos.registrar(Dificuldade.FACIL, 50);
        assertFalse(RegistroDeTempos.registrar(Dificuldade.FACIL, 60));
        assertEquals(50, RegistroDeTempos.melhorTempo(Dificuldade.FACIL));
        assertTrue(RegistroDeTempos.registrar(Dificuldade.FACIL, 30));
        assertEquals(30, RegistroDeTempos.melhorTempo(Dificuldade.FACIL));
    }

    @Test
    public void temposDeDificuldadesNaoSeMisturam() {
        RegistroDeTempos.registrar(Dificuldade.FACIL, 20);
        RegistroDeTempos.registrar(Dificuldade.DIFICIL, 300);
        assertEquals(20, RegistroDeTempos.melhorTempo(Dificuldade.FACIL));
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.MEDIO));
        assertEquals(300, RegistroDeTempos.melhorTempo(Dificuldade.DIFICIL));
    }

    @Test
    public void valorNegativoNaoViraRecorde() {
        assertFalse(RegistroDeTempos.registrar(Dificuldade.FACIL, -5));
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.FACIL));
    }

    @Test
    public void arquivoCorrompidoDevolveZeroSemTravamento() throws IOException {
        Path arquivo = Paths.get(pastaTemporaria.toString(), ".jogo-campo-minado-tempos");
        Files.writeString(arquivo, "isto não é um properties válido {{{");
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.MEDIO));
    }

    @Test
    public void numeroImpossivelDevolveZero() throws IOException {
        Path arquivo = Paths.get(pastaTemporaria.toString(), ".jogo-campo-minado-tempos");
        Files.writeString(arquivo, "medio=-99\n", StandardCharsets.UTF_8);
        assertEquals(0, RegistroDeTempos.melhorTempo(Dificuldade.MEDIO));
    }

    @Test
    public void depoisDeCorrompidoAindaConsegueRegistrar() throws IOException {
        Path arquivo = Paths.get(pastaTemporaria.toString(), ".jogo-campo-minado-tempos");
        Files.writeString(arquivo, "lixo");
        assertTrue(RegistroDeTempos.registrar(Dificuldade.DIFICIL, 400));
        assertEquals(400, RegistroDeTempos.melhorTempo(Dificuldade.DIFICIL));
    }

    private void deleteRecursivo(Path raiz) throws IOException {
        if (raiz == null || !Files.exists(raiz)) {
            return;
        }
        try (var caminhos = Files.walk(raiz)) {
            caminhos.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignorada) {
                            // Arquivo aberto por outro processo: segue.
                        }
                    });
        }
    }
}