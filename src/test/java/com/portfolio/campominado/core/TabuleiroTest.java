package com.portfolio.campominado.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Random;
import org.junit.Before;
import org.junit.Test;

/**
 * Regras do tabuleiro, verificadas uma a uma.
 *
 * <p>Testes de comportamento usam minas plantadas à mão em posições exatas
 * ({@code minar} + {@code selarMinas}). Testes do sorteio usam uma semente
 * fixa de {@link Random} e o caminho real do primeiro clique.
 */
public class TabuleiroTest {

    private Tabuleiro tabuleiro;

    @Before
    public void novoTabuleiro() {
        tabuleiro = new Tabuleiro(9, 9, 10, new Random(7));
    }

    @Test
    public void criaEspacoComOitoVizinhosNoMeio() {
        List<Celula> vizinhos = tabuleiro.getCelula(4, 4).getVizinhos();
        assertEquals(8, vizinhos.size());
        assertTrue(vizinhos.contains(tabuleiro.getCelula(3, 3)));
        assertTrue(vizinhos.contains(tabuleiro.getCelula(5, 5)));
    }

    @Test
    public void cantoTemTresVizinhos() {
        assertEquals(3, tabuleiro.getCelula(0, 0).getVizinhos().size());
    }

    @Test
    public void bordaTemCincoVizinhos() {
        assertEquals(5, tabuleiro.getCelula(0, 4).getVizinhos().size());
    }

    @Test
    public void rejeitaTabuleiroSemCelulaLivreParaPrimeiroClique() {
        try {
            new Tabuleiro(3, 3, 9);
            throw new AssertionError("deveria ter lançado IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            assertNotNull(esperada.getMessage());
        }
    }

    @Test
    public void rejeitaTabuleiroSemMinas() {
        try {
            new Tabuleiro(9, 9, 0);
            throw new AssertionError("deveria ter lançado IllegalArgumentException");
        } catch (IllegalArgumentException esperada) {
            assertNotNull(esperada.getMessage());
        }
    }

    @Test
    public void aceitaOCasoLimiteDeMinasSobraUmaSegura() {
        new Tabuleiro(2, 2, 3);
    }

    @Test
    public void comecaAguardandoSemMinasSorteadas() {
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tabuleiro.getEstado());
        assertEquals(0, minasAtuais());
    }

    @Test
    public void primeiroCliqueSorteiaEProtegeACelulaClicada() {
        tabuleiro.abrir(0, 0);
        assertEquals(Tabuleiro.Estado.JOGANDO, tabuleiro.getEstado());
        assertFalse(tabuleiro.getCelula(0, 0).isMinado());
    }

    @Test
    public void sorteioPlantaOExatoNumeroDeMinas() {
        tabuleiro.abrir(0, 0);
        assertEquals(10, minasAtuais());
    }

    @Test
    public void sorteioProtegeSoACelulaClicadaNumCampoMinado() {
        Tabuleiro lotado = new Tabuleiro(2, 2, 3, new Random(1));
        lotado.abrir(0, 0);
        assertFalse(lotado.getCelula(0, 0).isMinado());
        assertTrue(lotado.getCelula(0, 1).isMinado());
        assertTrue(lotado.getCelula(1, 0).isMinado());
        assertTrue(lotado.getCelula(1, 1).isMinado());
    }

    @Test
    public void sorteioProtegeOPrimeiroCliqueEmQualquerCelula() {
        Tabuleiro lotado = new Tabuleiro(2, 2, 3, new Random(1));
        lotado.abrir(1, 1);
        assertFalse(lotado.getCelula(1, 1).isMinado());
        assertTrue(lotado.getCelula(0, 0).isMinado());
        assertTrue(lotado.getCelula(0, 1).isMinado());
        assertTrue(lotado.getCelula(1, 0).isMinado());
    }

    @Test
    public void abrirCampoJaAbertoNaoMudaNada() {
        tabuleiro.abrir(0, 0);
        int abertas = tabuleiro.celulasAbertas();
        assertFalse(tabuleiro.abrir(0, 0));
        assertEquals(abertas, tabuleiro.celulasAbertas());
    }

    @Test
    public void celulaMarcadaNaoAbre() {
        tabuleiro.abrir(3, 4);
        tabuleiro.alternarMarcacao(8, 8);
        assertFalse(tabuleiro.abrir(8, 8));
        assertFalse(tabuleiro.getCelula(8, 8).isAberto());
        assertTrue(tabuleiro.getCelula(8, 8).isMarcado());
    }

    @Test
    public void alternarMarcaDuasVezesVoltaAoEstadoOriginal() {
        tabuleiro.alternarMarcacao(1, 1);
        assertTrue(tabuleiro.getCelula(1, 1).isMarcado());
        tabuleiro.alternarMarcacao(1, 1);
        assertFalse(tabuleiro.getCelula(1, 1).isMarcado());
    }

    @Test
    public void bandeirasUsadasAcompanhaMarcas() {
        assertEquals(0, tabuleiro.bandeirasUsadas());
        tabuleiro.alternarMarcacao(0, 1);
        tabuleiro.alternarMarcacao(0, 2);
        assertEquals(2, tabuleiro.bandeirasUsadas());
        tabuleiro.alternarMarcacao(0, 1);
        assertEquals(1, tabuleiro.bandeirasUsadas());
    }

    @Test
    public void abrirMinaEncerraComoDerrota() {
        tabuleiro.minar(1, 2);
        tabuleiro.selarMinas();
        assertTrue(tabuleiro.abrir(1, 2));
        assertEquals(Tabuleiro.Estado.PERDEU, tabuleiro.getEstado());
    }

    @Test
    public void derrotaRevelaTodasAsMinasEscondidas() {
        tabuleiro.minar(1, 2);
        tabuleiro.minar(5, 5);
        tabuleiro.selarMinas();
        assertTrue(tabuleiro.abrir(1, 2));
        assertTrue(tabuleiro.getCelula(1, 2).isAberto());
        assertTrue(tabuleiro.getCelula(5, 5).isAberto());
    }

    @Test
    public void derrotaNaoRevelaMinaMarcada() {
        tabuleiro.minar(1, 2);
        tabuleiro.minar(5, 5);
        tabuleiro.alternarMarcacao(5, 5);
        tabuleiro.selarMinas();
        assertTrue(tabuleiro.abrir(1, 2));
        assertFalse(tabuleiro.getCelula(5, 5).isAberto());
        assertTrue(tabuleiro.getCelula(5, 5).isMarcado());
    }

    @Test
    public void partidaEncerradaNaoAbreNemMarca() {
        tabuleiro.minar(1, 2);
        tabuleiro.selarMinas();
        tabuleiro.abrir(1, 2);
        assertFalse(tabuleiro.abrir(3, 3));
        int abertas = tabuleiro.celulasAbertas();
        tabuleiro.alternarMarcacao(4, 4);
        assertEquals(abertas, tabuleiro.celulasAbertas());
        assertFalse(tabuleiro.getCelula(4, 4).isMarcado());
    }

    @Test
    public void minasNaVizinhancaContaSomenteVizinhosReais() {
        tabuleiro.minar(0, 1);
        tabuleiro.minar(1, 0);
        tabuleiro.selarMinas();
        assertEquals(0, tabuleiro.getCelula(3, 3).minasNaVizinhanca());
        assertEquals(2, tabuleiro.getCelula(0, 0).minasNaVizinhanca());
        assertEquals(2, tabuleiro.getCelula(1, 1).minasNaVizinhanca());
        assertEquals(1, tabuleiro.getCelula(0, 2).minasNaVizinhanca());
    }

    @Test
    public void cascataAbreARegiaoDeZerosAteABordaDeNumeros() {
        tabuleiro.minar(2, 1);
        tabuleiro.minar(2, 2);
        tabuleiro.minar(1, 2);
        tabuleiro.selarMinas();
        assertTrue(tabuleiro.abrir(0, 0));
        assertTrue(tabuleiro.getCelula(0, 0).isAberto());
        assertTrue(tabuleiro.getCelula(0, 1).isAberto());
        assertTrue(tabuleiro.getCelula(1, 0).isAberto());
        assertTrue(tabuleiro.getCelula(1, 1).isAberto());
        assertFalse(tabuleiro.getCelula(0, 2).isAberto());
        assertFalse(tabuleiro.getCelula(1, 3).isAberto());
        assertFalse(tabuleiro.getCelula(2, 0).isAberto());
        assertFalse(tabuleiro.getCelula(8, 8).isAberto());
        assertEquals(4, tabuleiro.celulasAbertas());
    }

    @Test
    public void cascataNaoAtravessaBandeira() {
        tabuleiro.minar(2, 2);
        tabuleiro.alternarMarcacao(1, 1);
        tabuleiro.selarMinas();
        tabuleiro.abrir(0, 0);
        assertFalse(tabuleiro.getCelula(1, 1).isAberto());
        assertTrue(tabuleiro.getCelula(1, 1).isMarcado());
    }

    @Test
    public void cascataAbreAEncostaDaMinaEParaNela() {
        tabuleiro.minar(3, 3);
        tabuleiro.selarMinas();
        tabuleiro.abrir(0, 0);
        assertTrue(tabuleiro.getCelula(2, 2).isAberto());
        assertFalse(tabuleiro.getCelula(3, 3).isAberto());
        assertTrue(tabuleiro.getCelula(3, 3).isMinado());
    }

    @Test
    public void ganhaQuandoTodaAreaSemMinaFicaAberta() {
        tabuleiro.minar(1, 1);
        tabuleiro.selarMinas();
        abrirAreasRestantes(tabuleiro, 1, 1);
        assertEquals(Tabuleiro.Estado.GANHOU, tabuleiro.getEstado());
    }

    @Test
    public void celulasAbertasSomamCerto() {
        tabuleiro.minar(2, 2);
        tabuleiro.selarMinas();
        tabuleiro.abrir(0, 0);
        assertEquals(80, tabuleiro.celulasAbertas());
    }

    @Test
    public void reiniciarLimpaTudoEVoltaAoEstadoInicial() {
        tabuleiro.minar(1, 2);
        tabuleiro.alternarMarcacao(0, 0);
        tabuleiro.selarMinas();
        tabuleiro.abrir(1, 2);
        tabuleiro.reiniciar();
        assertEquals(Tabuleiro.Estado.AGUARDANDO, tabuleiro.getEstado());
        assertEquals(0, minasAtuais());
        assertEquals(0, tabuleiro.celulasAbertas());
        assertEquals(0, tabuleiro.bandeirasUsadas());
    }

    @Test
    public void novoPrimeiroCliqueDepoisDoReinicioSorteiaDeNovo() {
        tabuleiro.abrir(0, 0);
        tabuleiro.reiniciar();
        tabuleiro.abrir(8, 8);
        assertEquals(Tabuleiro.Estado.JOGANDO, tabuleiro.getEstado());
        assertEquals(10, minasAtuais());
    }

    private void abrirAreasRestantes(Tabuleiro t, int minaLinha, int minaColuna) {
        for (int l = 0; l < 9; l++) {
            for (int c = 0; c < 9; c++) {
                if (l == minaLinha && c == minaColuna) {
                    continue;
                }
                t.abrir(l, c);
            }
        }
    }

    private int minasAtuais() {
        int contagem = 0;
        for (int l = 0; l < 9; l++) {
            for (int c = 0; c < 9; c++) {
                if (tabuleiro.getCelula(l, c).isMinado()) {
                    contagem++;
                }
            }
        }
        return contagem;
    }
}