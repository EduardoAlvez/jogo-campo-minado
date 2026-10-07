# Kanban — Jogo Campo Minado

Quarto jogo do portfólio, depois do Pong, da Forca e do Snake. Nasce de um
projeto do curso Cod3r (`Estudo-Java`, pacote `br.com.cod3r.cm`), que já tinha
a separação modelo/visão certa e, junto, os defeitos que os três primeiros
jogos aprenderam a não repetir: zero testes, `Temp.java` commitado, sorteio de
minas sem teto, e pintura duplicada no botão. Este port usa a mesma disciplina
do trio — layout puro, desenho sem regra, janela sem regra — e corrige o pior
defeito do original: **o primeiro clique nunca é numa mina**.

Java 17 + Swing, sem dependências externas: o mesmo `pom.xml` de sempre (JUnit
4.13.2 + surefire + JaCoCo + launch4j).

## Estrutura (subpacotes por camada)

```
src/main/java/com/portfolio/campominado/
├── core/       Núcleo puro e testável (sem Swing)
│   ├── Celula.java / Tabuleiro.java / Dificuldade.java / RegistroDeTempos.java
├── audio/      Efeitos sonoros sintetizados
└── ui/         Interface gráfica Swing
    ├── LayoutCampoMinado.java   Geometria: onde cada coisa é desenhada
    ├── DesenhoCampoMinado.java  O desenho, sem janela (pinta em qualquer Graphics2D)
    └── TelaCampoMinado.java     A janela: mouse, teclado, relógio e repaint

src/main/resources/
├── logo-{16,24,32,48,64,128,256}.png   # Ícone da janela, uma por resolução
└── logo.ico                              # Ícone do executável (.exe)

target/                                   # Saída do build (não versionada)
```

## Decisões já fechadas

- **Primeiro clique nunca é mina** — só a própria célula é protegida (não a
  vizinhança). As minas são **sorteadas no primeiro clique**, entre os índices
  que sobram, por embaralhamento — sem `while` sem teto (regra 7 da skill).
  É a correção do defeito do curso, que permitia morrer no 1º clique.
- **Mouse e teclado**: esquerdo abre, direito marca; setas movem o cursor,
  Espaço abre e `F` marca — para quem prefere teclado e para testar sem janela
  (regra 9 da skill).
- **Vitória** = toda célula não-minada aberta (bandeiras não obrigatórias).
  **Derrota** = abrir uma mina revela todas.
- **Bandeira não abre**; campo marcado não abre com clique nem cascata.
- **Abertura em cascata** por fila (BFS), não recursão — sem risco de estouro
  em campos grandes cheios de zeros.
- **Cronômetro**: começa no primeiro clique, trava no fim da partida. **Melhor
  tempo por dificuldade** guardado em `~/.jogo-campo-minado-tempos`, com
  gravação atômica (temp + rename) e leitura defensiva (arquivo corrompido
  devolve zero).
- **Três dificuldades**: Fácil 9×9×10, Médio 16×16×40, Difícil 16×30×99.

## A Fazer (Backlog)

| # | Prioridade | Camada | Tarefa | Detalhes |
|---|-----------|--------|--------|----------|
| 1 | Alta | core | Regras do jogo | Célula, tabuleiro, sorteio seguro, cascata, vitória/derrota, bandeira, reinício — testados e verificados por mutação |
| 2 | Alta | core | Tempos | `RegistroDeTempos` com gravação atômica e fallback em corrupção |
| 3 | Alta | ui | Layout puro | `LayoutCampoMinado`: célula inteira de pixels, grade que cabe em janelas menores |
| 4 | Alta | ui | Desenho | `DesenhoCampoMinado`: números 1–8 nas cores clássicas, bandeira, mina, sem estado |
| 5 | Alta | ui | Janela | `TelaCampoMinado`: hit-test do mouse por geometria, cursor por teclado, Timer com tempo medido, repaint no mouse *leave* |
| 6 | Média | ui | Telas | Menu por botões (dificuldade/recomeçar), pausa, tela de fim com selo de recorde |
| 7 | Média | audio | Sons | Abrir, marcar, explodir e vencer, sintetizados por código, carregados de dentro de um jar |
| 8 | Média | — | README | Descrever o que o jogo **tem**, e não o que vai ter (regra do Snake) |
| 9 | Baixa | fx | Partículas de explosão | Reaproveitar do Pong |
| 10 | Baixa | core | Dificuldade personalizada | Grade e minas livres |
| 11 | Baixa | core | Continuar partida | Salvar o tabuleiro em andamento |

## Fases (cada uma com build verde antes do commit)

1. **Núcleo**: scaffold, `core/` completo e testado com mutação.
2. **Interface**: layout, desenho por pixels, tela com mouse/teclado/cronômetro
   e testes de tela. Validação visual **humana** ao final.
3. **Acabamento**: menu e tela de fim, áudio, logo/ícones, README, tag, push e
   verificação do artefato publicado (hash do `.exe` baixado de volta, jar sem
   `*Test.class`).