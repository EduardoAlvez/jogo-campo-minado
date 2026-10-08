package com.portfolio.campominado.audio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.portfolio.campominado.audio.Sons.Efeito;
import com.portfolio.campominado.core.Tabuleiro;

/**
 * Decide quais sons tocar, a partir do que mudou entre dois momentos da
 * partida.
 *
 * <p>Fica fora da {@code JFrame} pelo mesmo motivo que o resto do jogo: dentro
 * da janela não há como testar, e uma regra de jogo sem teste é uma regra que
 * alguém "ajusta" sem querer. Aqui a regra é uma função pura, de entrada e
 * saída visíveis.
 *
 * <p><b>Por que um retrato, e não o tabuleiro.</b> A pergunta é "o que mudou
 * entre antes e depois da jogada", mas {@code Tabuleiro} é o mesmo objeto
 * mutável nos dois instantes. O retrato é a fotografia do antes; sem ele, lê-se
 * o depois duas vezes e chama-se a segunda leitura de "antes".
 */
public final class Trilha {

    /** O que se observa da partida em um instante. */
    public static final class Retrato {

        final Tabuleiro.Estado estado;
        final int abertas;
        final int bandeiras;

        Retrato(Tabuleiro.Estado estado, int abertas, int bandeiras) {
            this.estado = estado;
            this.abertas = abertas;
            this.bandeiras = bandeiras;
        }

        /** Estado da partida neste instante. */
        public Tabuleiro.Estado getEstado() {
            return estado;
        }

        /** Células abertas até aqui. */
        public int getAbertas() {
            return abertas;
        }

        /** Bandeiras plantadas até aqui. */
        public int getBandeiras() {
            return bandeiras;
        }
    }

    private Trilha() {
    }

    /** Fotografa a partida. */
    public static Retrato tira(Tabuleiro tabuleiro) {
        return new Retrato(tabuleiro.getEstado(),
                tabuleiro.celulasAbertas(),
                tabuleiro.bandeirasUsadas());
    }

    /**
     * Os efeitos que a jogada de {@code antes} para {@code depois} produziu.
     *
     * <p>O fim tem prioridade sobre a abertura: abrir uma mina também abre
     * colunas de células vizinhas no mesmo gesto, mas o som é de derrota — e
     * abrir a última célula segura também é vencer, então a vitória vem
     * sozinha, sem o bip da abertura a reboque.
     *
     * <p>Só há som quando <b>alguma coisa mudou</b>: um clique em célula já
     * aberta ou já resolvida não devolve lista nenhuma.
     *
     * @param antes  o retrato de antes da jogada
     * @param depois o retrato de depois da jogada
     * @return os efeitos a tocar, na ordem em que devem soar
     */
    public static List<Efeito> dePara(Retrato antes, Retrato depois) {
        List<Efeito> sons = new ArrayList<>(1);
        if (antes == null || depois == null) {
            return sons;
        }
        if (antes.getEstado() != Tabuleiro.Estado.PERDEU
                && depois.getEstado() == Tabuleiro.Estado.PERDEU) {
            sons.add(Efeito.EXPLODIR);
            return Collections.unmodifiableList(sons);
        }
        if (antes.getEstado() != Tabuleiro.Estado.GANHOU
                && depois.getEstado() == Tabuleiro.Estado.GANHOU) {
            sons.add(Efeito.VENCER);
            return Collections.unmodifiableList(sons);
        }
        if (depois.getAbertas() > antes.getAbertas()) {
            sons.add(Efeito.ABRIR);
        } else if (depois.getBandeiras() != antes.getBandeiras()) {
            sons.add(Efeito.MARCAR);
        }
        return Collections.unmodifiableList(sons);
    }
}