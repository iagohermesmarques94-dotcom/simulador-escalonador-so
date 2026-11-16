---------Simulador de Escalonamento de Processos---------

Projeto desenvolvido para a disciplina de Sistemas Operacionais, com o objetivo de simular o escalonamento de processos utilizando o algoritmo Round Robin com Feedback, filas de prioridades e operações de I/O.

Grupo:

Aluno

Aluno

Aluno

Aluno

Linguagem: Java

1. Funcionamento

O simulador cria processos e controla sua execução por meio de três filas:

Prioridade alta: recebe novos processos e processos que retornam de determinadas operações de I/O.

Prioridade baixa: recebe processos que utilizam todo o quantum e processos que retornam de determinadas operações de I/O.

I/O: mantém os processos bloqueados enquanto aguardam uma operação.

A fila de prioridade alta tem preferência sobre a fila de prioridade baixa. Cada processo utiliza a CPU durante um tempo limitado pelo quantum. Caso não finalize nesse período, pode ser enviado para uma fila de prioridade inferior.

2. Configurações

Quantum: 3 unidades de tempo

Número de processos: 5

Tempo mínimo de CPU: 5

Tempo máximo de CPU: 5

Tempo máximo de chegada: 0

I/O do disco: 2 unidades

I/O da fita: 3 unidades

I/O da impressora: 4 unidades

Semente aleatória: 40

Os processos são criados automaticamente, recebendo um PID, tempo de CPU, prioridade inicial alta, dispositivo de I/O e momento para solicitar I/O. A semente aleatória permite repetir os resultados com as mesmas configurações.

3. Como executar

Java: é necessário ter o Java instalado. Na pasta principal do projeto, execute:
 --
 --
 --

Docker: é necessário ter o Docker instalado e em funcionamento. Na pasta principal do projeto, execute:

---
---
---
---

Executar o simulador:

---
---
---
---

O Docker prepara o ambiente Java necessário para executar o programa, sem precisar instalar o Java diretamente no computador.

4. Resultado da execução

Durante a simulação, são apresentados eventos como criação de processos, utilização da CPU, mudanças de prioridade, bloqueios e retornos de I/O e finalização dos processos.

Exemplo de saída:

---
---
---

Ao final, o programa apresenta um resumo com:

Quantidade de processos finalizados;

Tempo total da simulação;

Número de preempções;

Quantidade de operações de I/O;

Tempo médio de espera e de retorno;

Percentual de tempo em que a CPU ficou ociosa.

5. Limitações

O simulador utiliza regras simplificadas de escalonamento. A quantidade de processos e os tempos podem ser alterados diretamente no código. A semente aleatória é fixa para facilitar a repetição dos testes.

6. Tecnologias utilizadas

- Java

- Docker

7. Objetivo

Demonstrar, de forma prática, o funcionamento do escalonamento de processos em um sistema operacional, utilizando filas de prioridades, divisão do tempo de CPU e operações de I/O.
