# Simulador de Escalonamento de Processos — Round Robin com Feedback

Simulador desenvolvido em **Java** para a disciplina de **Sistemas Operacionais**, projetado para reproduzir o comportamento do núcleo (kernel) de um sistema operacional no gerenciamento de processos, alternância de contexto, filas de múltiplas prioridades e tratamento de interrupções de Entrada e Saída (E/S).

O projeto foi construído sem o uso de bibliotecas externas ou orientação a objetos tradicional, utilizando **vetores paralelos (PCB)** e **filas circulares estáticas** para demonstrar o controle de processos” e algoritmos de baixo nível.

## Equipe

* **Iago Hermes Marques** — Núcleo do escalonador e preempção (Parte B)
* **Kaua da Silva Schimit** — Estruturas de dados e filas circulares (Parte A)
* **Murilo Born Roos** — Gerenciamento de E/S e métricas (Parte C)
* **Pedro Henrique Rohde Marques** — Infraestrutura Docker e documentação
  
  ---

## Como o Programa Funciona (Baseado na Execução)

A simulação gerencia o ciclo de vida completo de **5 processos (`P1` a `P5`)** desde a criação até a finalização, controlando um relógio lógico discreto (`t=000` até `t=025`):

1. **Criação e Fila de Alta Prioridade:** Todos os processos são inicializados em `t=000` na fila `ALTA`, que possui preferência absoluta de execução sobre a fila `BAIXA`.
2. **Fatia de Tempo (Quantum = 3):** Cada processo ocupa a CPU por no máximo 3 unidades de tempo por vez. Caso utilize todo o quantum sem finalizar ou pedir E/S (como ocorreu com `P5` em `t=017` e `P1` em `t=020`), sofre **preempção** e é rebaixado para a fila `BAIXA`.
3. **Bloqueio e Retorno de E/S (I/O):** Antes de concluir sua carga de CPU, cada processo solicita uma operação de E/S em rodízio entre três dispositivos, liberando a CPU imediatamente:
   * **Disco (2 unidades):** Ao concluir, o processo retorna para a **fila `BAIXA`** (`P1` e `P4`), simulando dispositivos que exigem menor prioridade imediata.
   * **Fita Magnética (3 unidades):** Ao concluir, o processo retorna para a **fila `ALTA`** (`P2` e `P5`).
   * **Impressora (4 unidades):** Ao concluir, o processo retorna para a **fila `ALTA`** (`P3`).
4. **Métricas de Desempenho:** Ao encerrar todos os processos em `t=025`, o sistema consolida os indicadores de eficiência do escalonador:
   * **Preempções:** 2 trocas de contexto forçadas por fim de quantum.
   * **Operações de E/S:** 5 eventos atendidos (2 de disco, 2 de fita e 1 de impressora).
   * **Tempo Médio de Espera:** `11.60` unidades de tempo na fila de prontos.
   * **Tempo Médio de Retorno (Turnaround):** `19.40` unidades de tempo entre a chegada e a conclusão.
   * **Ociosidade da CPU:** `0.00%` (aproveitamento integral do processador durante os 25 ciclos).
---

## Configurações e Premissas

Os parâmetros abaixo são definidos como constantes no código-fonte e utilizam uma semente fixa (`SEMENTE = 40`) para garantir que os testes possam ser reproduzidos com os mesmos resultados:

| Parâmetro | Valor | Descrição |
| :--- | :---: | :--- |
| **Quantum** | `3` | Fatia máxima contínua de CPU por turno |
| **Número de Processos** | `5` | Quantidade total de processos simulados (`P1` a `P5`) |
| **Criação dos Processos** | `t=000` | Todos os processos são criados no início da simulação e entram na fila `ALTA` |
| **Tempo Mínimo de CPU** | `5` | Carga mínima de processamento por processo |
| **Tempo Máximo de CPU** | `5` | Carga máxima de processamento por processo |
| **Tempo Máximo de Chegada** | `0` | Instante de chegada (`0` = todos chegam em `t=000`) |
| **I/O do Disco** | `2` | Duração da operação de disco (retorno para fila `BAIXA`) |
| **I/O da Fita** | `3` | Duração da operação de fita magnética (retorno para fila `ALTA`) |
| **I/O da Impressora** | `4` | Duração da operação de impressora (retorno para fila `ALTA`) |
| **Semente Aleatória** | `40` | Garante determinismo na geração dos eventos |
| **Fim da Simulação** | `FINALIZADO` | A simulação termina quando todos os processos estão no estado `FINALIZADO` |

---

## Estrutura do Repositório
```text
simulador-escalonador-so/
├── src/
│   └── Main.java       # Código-fonte completo do simulador (Partes A, B e C)
├── .dockerignore       # Arquivos ignorados na construção da imagem Docker
├── .gitignore          # Arquivos ignorados pelo controle de versão Git
├── Dockerfile          # Configuração para compilação e execução em container
└── README.md          # Documentação técnica, premissas e instruções
```
---

## Como Executar

### Opção 1: Via Docker (Recomendado)
É necessário ter o **Docker** instalado e em funcionamento. O container prepara todo o ambiente Java automaticamente, sem precisar instalar o JDK no computador. Na pasta principal do projeto, execute:

```bash
docker build -t simulador-escalonador-so .
docker run --rm simulador-escalonador-so
```

### Opção 2: Via Java (Local)
É necessário ter o **Java (JDK)** instalado. Na pasta principal do projeto, execute:

```bash
javac src/Main.java
java -cp src Main
```

---

## Resultado da Execução

Durante a simulação, o terminal exibe cronologicamente a criação dos processos, o uso da CPU, as preempções por quantum, os bloqueios e retornos de I/O e as finalizações.

### Exemplo de Saída

<img width="1101" height="908" alt="image" src="https://github.com/user-attachments/assets/92aaccd8-d1e5-423a-b783-63d4192cf7aa" />

Ao final da execução, o programa apresenta um resumo consolidado contendo:

* Quantidade de processos finalizados e tempo total da simulação;
* Número total de preempções por fim de quantum;
* Quantidade de operações de I/O realizadas por dispositivo;
* Tempo médio de espera e tempo médio de retorno (*turnaround*);
* Percentual de tempo em que a CPU permaneceu ociosa.
---

## Limitações e Escopo

* O simulador utiliza regras simplificadas de escalonamento para fins didáticos, sem simulação de memória virtual ou múltiplos núcleos de processamento.
* Cada processo realiza **um único I/O** durante a sua execução, em um dispositivo atribuído em rodízio (disco, fita e impressora).
* Todos os dispositivos compartilham **uma única fila de I/O**; não há fila separada por dispositivo.
* Existem apenas duas prioridades (`ALTA` e `BAIXA`). Um processo em execução **não é interrompido** quando outro entra na fila `ALTA`; ele só deixa a CPU ao fim do quantum, ao pedir I/O ou ao finalizar.
* Com as premissas atuais (todos com 5 unidades de CPU e chegada em `t=000`), a CPU nunca fica ociosa. O tratamento de CPU ociosa existe no código, mas não aparece nesta execução.
* O `PPID` de todos os processos é `0` (criados pelo "sistema").
* A quantidade de processos e os tempos podem ser ajustados nas constantes de `src/Main.java`. O tempo mínimo de CPU (`CPU_MIN`) deve ser **maior ou igual a 2**, pois cada processo precisa executar ao menos uma unidade antes de pedir I/O.
* A semente aleatória é fixa por padrão para facilitar a validação e a reprodução dos cenários de teste.

---

## Tecnologias Utilizadas

* **Java (JDK):** Implementação da lógica do escalonador, vetores paralelos (PCB) e filas circulares.
* **Docker:** Containerização e padronização do ambiente de execução.
* **Git & GitHub:** Controle de versão e colaboração entre os integrantes da equipe.

---

## Objetivo

Demonstrar, na prática, o funcionamento do escalonamento de processos em um sistema operacional utilizando filas de múltiplas prioridades, divisão de tempo de CPU por quantum e tratamento de interrupções de E/S.
