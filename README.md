# Jogo Campo Minado

Campo Minado do portfólio, portado do projeto do curso Cod3r: 9×9, 16×16 e
16×30, mouse e teclado, recorde de tempo por dificuldade em disco e sons
sintetizados por código. Java 17 + Swing, sem nenhuma dependência em tempo de
execução — o `.exe` carrega o próprio jar.

## O que o jogo tem

- **Três dificuldades**: Fácil 9×9 com 10 minas, Médio 16×16 com 40 e Difícil
  16×30 com 99 — escolhidas no menu por mouse ou pelas teclas `1`, `2` e `3`.
- **Primeiro clique nunca é mina**: as minas são sorteadas no primeiro abrir,
  entre as células que sobram, então nenhuma partida morre no primeiro gesto.
- **Abertura em cascata** (BFS): o vazio destapa a vizinhança inteira, e a
  bandeira protege — célula marcada não abre nem por cascata.
- **Mouse e teclado**: esquerda abre, direita planta a bandeira; setas/`W A S D`
  movem o cursor, `Espaço` abre, `F` marca, `R` reinicia a partida.
- **Menu por botões** (dificuldade + começar) e **tela de fim com botões**
  "Jogar de novo" e "Voltar ao menu", com **selo de novo recorde**.
- **Recorde de tempo por dificuldade** em `~/.jogo-campo-minado-tempos`, com
  gravação atômica (temp + rename) e leitura defensiva.
- **Cronômetro** no HUD: começa no primeiro clique, para quando a partida acaba.
- **Quatro sons sintetizados por código** — abrir, marcar, explodir e vencer —
  e a *trilha* decide o que tocar pelo que mudou entre dois instantes da
  partida. Sem arquivo de áudio externo.
- **Ícone e executável com logo** gerados por código, com máscara AND de
  verdade no `.ico`.

## Controles

| Tecla | Ação |
|---|---|
| Mouse esquerdo | Abrir a célula |
| Mouse direito | Plantar / tirar a bandeira |
| Setas / `W` `A` `S` `D` | Mover o cursor |
| `Espaço` | Abrir a célula do cursor |
| `F` | Marcar a célula do cursor |
| `1` / `2` / `3` (no menu) | Escolher a dificuldade (fácil / médio / difícil) |
| `Enter` (no menu) | Começar a partida |
| `Enter` (no fim) | Jogar de novo |
| `M` (no fim) | Voltar ao menu |
| `R` (no jogo) | Recomeçar a partida |
| `Esc` | Voltar ao menu (no jogo) ou sair (no menu/fim) |

## Como rodar

Requer **Java 17** ou mais novo.

- **Windows**: execute `target/jogo-campo-minado.exe` depois do build, ou:
- **Qualquer plataforma**:

```
mvn clean package
java -jar target/jogo-campo-minado-1.0-SNAPSHOT.jar
```

O build gera também o executável Windows (`target/jogo-campo-minado.exe`) com o
ícone embutido.

## Testes

**109 testes** com JUnit 4, cobertura com JaCoCo:

```
mvn test
```

A suíte cobre as regras do núcleo (sorteio seguro no primeiro clique, cascata,
vitória/derrota, tempos), o áudio (carregado de dentro de um jar real, sem
buffer, como o jogo roda no `.exe`), o logo/máscara do `.ico` e a interface —
geometria do `LayoutCampoMinado` (dirám da grade e dos botões do menu), pixels
do `DesenhoCampoMinado` e comportamentos da janela — tudo sem abrir janela, com
vitórias e derrotas alcançadas por caminhos determinísticos.

## Arquitetura

O código é separado em camadas, e nenhuma regra mora no desenho:

```
src/main/java/com/portfolio/campominado/
├── core/       Regras puras e testáveis, sem Swing
├── audio/      Efeitos sonoros e a trilha que decide o que tocar
└── ui/         A interface gráfica
    ├── LayoutCampoMinado.java   A geometria: onde cada coisa é desenhada
    ├── DesenhoCampoMinado.java  O desenho, sem janela (pinta em qualquer Graphics2D)
    └── TelaCampoMinado.java     A janela: menu, jogo e fim, e o relógio
```

O `core/` não sabe que existe janela; o desenho recebe o resultado das regras e
só traduz em cor; a janela liga clique e tecla ao tabuleiro e não decide nada.
O som tampouco mora na janela: um retrato do tabuleiro antes da jogada e outro
depois dizem qual efeito tocar.

## Licença

MIT — ver [LICENSE](LICENSE).