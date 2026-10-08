package com.portfolio.campominado.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.portfolio.campominado.audio.Sons.Efeito;
import com.portfolio.campominado.audio.Trilha.Retrato;
import com.portfolio.campominado.core.Tabuleiro;

/**
 * As regras do {@link Trilha}: o que vale som e o que não vale.
 *
 * <p>Não há sorteio aqui: depois do primeiro abrir — que dispara o sorteio de
 * minas — o teste procura uma célula minada já revelada pelo {@code isMinado()}
 * público e abre exatamente aquela. Derrota e vitória são alcançadas por um
 * caminho determinístico, sem depender de seed nem de ordem de sorteio.
 */
public class TrilhaTest {

    /** O Fácil do jogo: 9×9 com 10 minas. */
    private static final int LINHAS = 9, COLUNAS = 9, MINAS = 10;

    private static Tabuleiro facil() {
        return new Tabuleiro(LINHAS, COLUNAS, MINAS);
    }

    // ------------------------------------------------------------------
    // Abrir e marcar
    // ------------------------------------------------------------------

    @Test
    public void abrirUmaCelulaSoltaBonABRIR() {
        Tabuleiro t = facil();
        Retrato antes = Trilha.tira(t);
        t.abrir(0, 0);
        Retrato depois = Trilha.tira(t);
        assertEquals(Arrays.asList(Efeito.ABRIR), Trilha.dePara(antes, depois));
    }

    @Test
    public void marcarDisparaONonoDaBandeira() {
        Tabuleiro t = facil();
        Retrato antes = Trilha.tira(t);
        t.alternarMarcacao(1, 1);
        Retrato depois = Trilha.tira(t);
        assertEquals(Arrays.asList(Efeito.MARCAR), Trilha.dePara(antes, depois));
    }

    @Test
    public void desmarcarUsaOMesmoNonoDaBandeira() {
        Tabuleiro t = facil();
        t.alternarMarcacao(2, 2);
        Retrato antes = Trilha.tira(t);
        t.alternarMarcacao(2, 2);
        Retrato depois = Trilha.tira(t);
        assertEquals(Arrays.asList(Efeito.MARCAR), Trilha.dePara(antes, depois));
    }

    // ------------------------------------------------------------------
    // Fim de partida
    // ------------------------------------------------------------------

    @Test
    public void abrirUmaMinaSoltaExplosaoEVitadodeBipDeAbrir() {
        Tabuleiro t = facil();
        t.abrir(0, 0); // dispara o sorteio de minas
        int[] comMina = procuraFechada(t, true);
        Retrato antes = Trilha.tira(t);
        t.abrir(comMina[0], comMina[1]);
        Retrato depois = Trilha.tira(t);
        assertEquals(Tabuleiro.Estado.PERDEU, depois.getEstado());
        assertEquals(Arrays.asList(Efeito.EXPLODIR), Trilha.dePara(antes, depois));
    }

    @Test
    public void abrirAultimaCelulaSeguraSoltaSoteOArpejo() {
        Tabuleiro t = facil();
        t.abrir(0, 0); // dispara o sorteio de minas

        int[] ultima = abreTudoMenosUma(t);
        Retrato antes = Trilha.tira(t);
        t.abrir(ultima[0], ultima[1]);
        Retrato depois = Trilha.tira(t);
        assertEquals(Tabuleiro.Estado.GANHOU, depois.getEstado());
        assertEquals(Arrays.asList(Efeito.VENCER), Trilha.dePara(antes, depois));
    }

    // ------------------------------------------------------------------
    // Quando nada muda, não há som
    // ------------------------------------------------------------------

    @Test
    public void cliqueInutilNaoSoltaSomNenhum() {
        Tabuleiro t = facil();
        t.abrir(0, 0);
        Retrato antes = Trilha.tira(t);
        t.abrir(0, 0); // já está aberta
        Retrato depois = Trilha.tira(t);
        assertEquals(antes.getAbertas(), depois.getAbertas());
        assertTrue(Trilha.dePara(antes, depois).isEmpty());
    }

    @Test
    public void retratoNuloNaoSoltaSom() {
        assertEquals(List.of(), Trilha.dePara(null, Trilha.tira(facil())));
        assertEquals(List.of(), Trilha.dePara(Trilha.tira(facil()), null));
        assertEquals(List.of(), Trilha.dePara(null, null));
    }

    // ------------------------------------------------------------------
    // Ferramentas do teste
    // ------------------------------------------------------------------

    /**
     * Abre toda célula segura, menos uma, e devolve essa última. A cascata
     * pode ter aberto qualquer célula por conta própria, então a "última" é
     * sempre procurada de novo no estado atual — abrir célula nenhuma re-fecha.
     */
    private static int[] abreTudoMenosUma(Tabuleiro t) {
        while (fechadasSeguras(t) > 1) {
            int[] alvo = procuraFechada(t, false);
            t.abrir(alvo[0], alvo[1]);
        }
        int totalSeguras = LINHAS * COLUNAS - MINAS;
        assertEquals("deve faltar exatamente uma célula segura",
                totalSeguras - 1, t.celulasAbertas());
        return procuraFechada(t, false);
    }

    private static int fechadasSeguras(Tabuleiro t) {
        int contagem = 0;
        for (int l = 0; l < t.getLinhas(); l++) {
            for (int c = 0; c < t.getColunas(); c++) {
                if (!t.getCelula(l, c).isAberto() && !t.getCelula(l, c).isMinado()) {
                    contagem++;
                }
            }
        }
        return contagem;
    }

    /**
     * A primeira célula fechada que seja (ou não) mina — o que o
     * {@link Trilha} precisa para alcançar derrota e vitória sem sorteio.
     */
    private static int[] procuraFechada(Tabuleiro t, boolean minada) {
        for (int l = 0; l < t.getLinhas(); l++) {
            for (int c = 0; c < t.getColunas(); c++) {
                if (!t.getCelula(l, c).isAberto()
                        && t.getCelula(l, c).isMinado() == minada) {
                    return new int[]{l, c};
                }
            }
        }
        throw new IllegalStateException(
                "célula fechada minada=" + minada + " nao achada");
    }
}