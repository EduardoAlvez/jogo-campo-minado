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
├── audio/      Sons sintetizados e a trilha que decide o que tocar
│   ├── Sons.java / Trilha.java
└── ui/         Interface gráfica Swing
    ├── LayoutCampoMinado.java   Geometria: onde cada coisa é desenhada
    ├── DesenhoCampoMinado.java  O desenho, sem janela (pinta em qualquer Graphics2D)
    └── TelaCampoMinado.java     A janela: menu, jogo, fim, mouse, teclado e relógio

src/main/resources/
├── sons/*.wav                       # Efeitos sintetizados por tools/GerarSons.java
├── logo-{16,24,32,48,64,128,256}.png   # Ícone da janela, uma por resolução
└── logo.ico                              # Ícone do executável (.exe)

tools/   # Geradores de recursos (fora do build do Maven)
├── GerarSons.java / DesenharLogoCampoMinado.java / GerarLogo.java
```

## Decisões já fechadas

- **Primeiro clique nunca é mina** — só a própria célula é protegida (não a
  vizinhança). As minas são **sorteadas no primeiro clique**, entre os índices
  que sobram, por embaralhamento — sem `while` sem teto (regra 7 da skill).
  É a correção do defeito do curso, que permitia morrer no 1º clique.
- **Mouse e teclado**: esquerdo abre, direito marca; setas (ou `W A S D`) movem o
  cursor, `Espaço` abre e `F` marca — para quem prefere teclado e para testar sem
  janela (regra 9 da skill). No menu, `1/2/3` escolhem a dificuldade e `Enter`
  começa; no fim, `Enter` joga de novo e `M` volta ao menu. `Esc` no jogo volta
  ao menu (não sai); no menu/fim sai. `R` reinicia a partida.
- **Três telas na mesma janela**: `Tela.MENU`, `Tela.JOGO` e `Tela.FIM` decidem
  o que pintar e o que cada tecla/clique faz — incluindo botões com **hover** no
  menu e no fim, e o selo "Novo recorde!".
- **Sons ligados às jogadas, não ao desenho**: um retrato do tabuleiro antes da
  jogada e outro depois dizem, pela mudança (abertas, bandeiras, fim), qual
  efeito tocar — EXPLODIR e VENCER vencem ABRIR e MARCAR. Os efeitos são
  sintetizados por `tools/GerarSons.java` e carregados **de dentro do jar**.
- **Vitória** = toda célula não-minada aberta (bandeiras não obrigatórias).
  **Derrota** = abrir uma mina revela todas.
- **Bandeira não abre**; campo marcado não abre com clique nem cascata.
- **Abertura em cascata** por fila (BFS), não recursão — sem risco de estouro
  em campos grandes cheios de zeros.
- **Cronômetro**: começa no primeiro clique, trava no fim da partida. **Melhor
  tempo por dificuldade** guardado em `~/.jogo-campo-minado-tempos`, com
  gravação atômica (temp + rename) e leitura defensiva (arquivo corrompido
  devolve zero). O recorde ganha selo na tela de fim.
- **Três dificuldades**: Fácil 9×9×10, Médio 16×16×40, Difícil 16×30×99.

## Pronto (por fase)

- [x] **1 — Núcleo**: celula, tabuleiro, sorteio seguro no 1º clique, cascata,
  vitória/derrota, bandeira, reinício — testados e verificados por mutação.
- [x] **2 — Interface**: layout puro (célula inteira de pixels, grade que cabe
  em janelas menores), desenho sem estado, tela com mouse + teclado +
  cronômetro + repaint no *leave*, validada visualmente.
- [x] **3 — Acabamento**:
  - Menu por botões (dificuldade/recomeçar) e tela de fim com selo de recorde.
  - Sons sintetizados (abrir, marcar, explodir, vencer) com trilha que decide o
    que tocar, carregados de dentro do jar.
  - Logo/ícones gerados por código, com máscara AND de verdade no `.ico`.
  - README e este kanban revisados; falta apenas o **push** e conferir o
    artefato publicado.

## A Fazer (Backlog)

| # | Prioridade | Camada | Tarefa | Detalhes |
|---|-----------|--------|--------|----------|
| 9 | Baixa | fx | Partículas de explosão | Reaproveitar do Pong |
| 10 | Baixa | core | Dificuldade personalizada | Grade e minas livres |
| 11 | Baixa | core | Continuar partida | Salvar o tabuleiro em andamento |

## Fases (cada uma com build verde antes do commit)

1. **Núcleo**: scaffold, `core/` completo e testado com mutação. ✔
2. **Interface**: layout, desenho por pixels, tela com mouse/teclado/cronômetro
   e testes de tela. Validação visual **humana** ao final. ✔
3. **Acabamento**: menu e tela de fim, áudio, logo/ícones, README, tag, push e
   verificação do artefato publicado (hash do `.exe` baixado de volta, jar sem
   `*Test.class`). Em andamento — faltam o push e a conferência final.