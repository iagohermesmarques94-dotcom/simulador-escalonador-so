import java.util.Locale;
import java.util.Random;

/**
 * TRABALHO FINAL DE SISTEMAS OPERACIONAIS
 * Simulador de Escalonamento Round Robin com Feedback
 *
 * Java SEM orientação a objetos: uma única classe, métodos static,
 * vetores e matrizes no lugar de objetos.
 *
 * Como compilar e executar (a partir da raiz do repositório):
 *   javac src/Main.java
 *   java -cp src Main
 *
 * Divisão do grupo:
 *   Parte A (Kaua)   -> estruturas de dados: PCB, filas e criação dos processos
 *   Parte B (Iago)   -> núcleo do escalonador: laço de tempo, quantum, preempção
 *   Parte C (Murilo) -> I/O, logs e métricas finais
 */
public class Main {

    // =====================================================================
    // 1. PREMISSAS (decididas em grupo; copiar também para README e relatório)
    // =====================================================================
    static final int  QUANTUM        = 3;    // fatia de tempo de CPU
    static final int  N_PROCESSOS    = 5;    // máximo de processos
    static final int  CPU_MIN        = 5;    // tempo de CPU mínimo por processo
    static final int  CPU_MAX        = 5;    // tempo de CPU máximo (igual ao mínimo: todos têm 5 unidades)
    static final int  CHEGADA_MAX    = 0;    // instante máximo de chegada (0 = todos chegam em t=0)
    static final int  IO_DISCO       = 2;    // duração do I/O de disco
    static final int  IO_FITA        = 3;    // duração do I/O de fita magnética
    static final int  IO_IMPRESSORA  = 4;    // duração do I/O de impressora
    static final long SEMENTE        = 40;   // semente aleatória (reprodutibilidade)

    static final Random rnd = new Random(SEMENTE);

    // =====================================================================
    // 2. CONSTANTES
    // =====================================================================
    // Estados do processo
    static final int NOVO = 0, PRONTO = 1, EXECUTANDO = 2, BLOQUEADO = 3, FINALIZADO = 4;

    // Prioridades
    static final int ALTA = 0, BAIXA = 1;

    // Dispositivos de I/O
    static final int NENHUM = 0, DISCO = 1, FITA = 2, IMPRESSORA = 3;

    // Índices das filas
    static final int Q_ALTA = 0, Q_BAIXA = 1, Q_IO = 2;

    // Resultado de executar(p): o que aconteceu com o processo    
    static final int PERDEU_QUANTUM = 0, PEDIU_IO = 1, FINALIZOU = 2;

    // =====================================================================
    // 3. PCB (Process Control Block) - vetores paralelos
    //    A posição i de cada vetor pertence ao processo de PID i + 1 (P1 = índice 0).
    // =====================================================================
    static int[] pid           = new int[N_PROCESSOS];
    static int[] ppid          = new int[N_PROCESSOS];
    static int[] status        = new int[N_PROCESSOS];
    static int[] prioridade    = new int[N_PROCESSOS];
    static int[] tempoRestante = new int[N_PROCESSOS];  // CPU que ainda falta
    static int[] proximoIO     = new int[N_PROCESSOS];  // NENHUM, DISCO, FITA ou IMPRESSORA
    static int[] tempoIO       = new int[N_PROCESSOS];  // duração do próximo I/O
    static int[] chegada       = new int[N_PROCESSOS];  // instante em que o processo chega
    static int[] fimIO         = new int[N_PROCESSOS];  // instante em que o I/O termina
    static int[] cpuAteIO      = new int[N_PROCESSOS];  // CPU executada até pedir I/O (decisão do grupo)

    // Campos extras para as métricas (parte da Pessoa C)
    static int[] tempoEspera   = new int[N_PROCESSOS];
    static int[] tempoFim      = new int[N_PROCESSOS];
    static int[] tempoTotalIO  = new int[N_PROCESSOS];  // (C) soma do tempo bloqueado em I/O
    static int[] cpuTotal      = new int[N_PROCESSOS];  // (A) CPU necessária ao criar o processo

    // =====================================================================
    // 4. FILAS (circulares, guardam PIDs)
    //    fila[Q_ALTA], fila[Q_BAIXA] e fila[Q_IO]
    // =====================================================================
    static int[][] fila   = new int[3][N_PROCESSOS];
    static int[]   inicio = new int[3];
    static int[]   tam    = new int[3];

    // =====================================================================
    // 5. ESTADO GLOBAL DA SIMULAÇÃO
    // =====================================================================
    static int tempo = 0;                 // relógio da simulação
    static int cpuOciosa = 0;             // unidades de tempo sem processo para executar
    static int totalPreempcoes = 0;       // quantas vezes um processo perdeu o quantum
    static int[] contagemIO = new int[4]; // eventos de I/O por tipo (índice = DISCO, FITA, IMPRESSORA)

    // =====================================================================
    // PESSOA A - ESTRUTURAS DE DADOS
    // =====================================================================

   /** Operações da fila (A). Usadas pelas partes B e C. */
    static void enfileirar(int q, int p) {
        fila[q][(inicio[q] + tam[q]) % N_PROCESSOS] = p;
        tam[q]++;
    }

    static int desenfileirar(int q) {
        int p = fila[q][inicio[q]];
        inicio[q] = (inicio[q] + 1) % N_PROCESSOS;
        tam[q]--;
        return p;
    }

    static boolean vazia(int q) {
        return tam[q] == 0;
    }

    /**
     * (A) Preenche o PCB de todos os processos.
     *  - pid = i + 1; ppid = 0 (todos criados pelo "sistema"); status NOVO; prioridade ALTA
     *  - tempoRestante: sorteio entre CPU_MIN e CPU_MAX
     *  - chegada: sorteio entre 0 e CHEGADA_MAX
     *  - I/O: dispositivo em rodízio (disco, fita, impressora); o pedido ocorre após
     *    cpuAteIO unidades de CPU (sorteado entre 1 e tempoRestante - 1).
     *    Cada processo faz um único I/O.
     * Processos NOVOS entram na fila ALTA quando chegada[i] <= tempo
     * (ver moverNovosParaFilaAlta).
     */
    static void criarProcessos() {
        for (int i = 0; i < N_PROCESSOS; i++) {
            pid[i] = i + 1;
            ppid[i] = 0;
            status[i] = NOVO;
            prioridade[i] = ALTA;

            tempoRestante[i] = CPU_MIN + rnd.nextInt(CPU_MAX - CPU_MIN + 1);
            cpuTotal[i] = tempoRestante[i];
            chegada[i] = rnd.nextInt(CHEGADA_MAX + 1);

            proximoIO[i] = (i % 3) + 1;                    // disco, fita, impressora em rodízio
            cpuAteIO[i] = 1 + rnd.nextInt(tempoRestante[i] - 1);
        }
    }

    // =====================================================================
    // PESSOA B - NÚCLEO DO ESCALONADOR
    // =====================================================================

    /** (B) Verdadeiro enquanto houver algum processo que ainda não finalizou. */
    static boolean existemNaoFinalizados() {
        for (int i = 0; i < N_PROCESSOS; i++) {
            if (status[i] != FINALIZADO) return true;
        }
        return false;
    }
/**
     * (B) Laço principal da simulação. Segue o pseudocódigo do enunciado:
     *
     *   enquanto existem processos não finalizados:
     *       moverNovosParaFilaAlta(tempo)
     *       atualizarIO(tempo)                  // parte C
     *       p = escolherProximo()
     *       se p == -1: CPU ociosa, tempo++, continua
     *       status[p] = EXECUTANDO
     *       resultado = executar(p)
     *       conforme o resultado:
     *           FINALIZOU       -> status = FINALIZADO
     *           PEDIU_IO        -> bloquearParaIO(p)     // parte C
     *           PERDEU_QUANTUM  -> status = PRONTO, volta para a fila BAIXA
     */
   static void escalonar() {
        while (existemNaoFinalizados()) {
            moverNovosParaFilaAlta(tempo);
            atualizarIO(tempo);

            int p = escolherProximo();
            if (p == -1) {
                log(tempo, "CPU ociosa");
                cpuOciosa++;
                tempo++;
                continue;
            }

            status[p] = EXECUTANDO;
            int resultado = executar(p);

            if (resultado == FINALIZOU) {
                status[p] = FINALIZADO;
                tempoFim[p] = tempo;
                log(tempo, "P" + pid[p] + " finalizou");
            } else if (resultado == PEDIU_IO) {
                bloquearParaIO(p);
            } else {
                status[p] = PRONTO;
                prioridade[p] = BAIXA;
                enfileirar(Q_BAIXA, p);
                totalPreempcoes++;
                log(tempo, "P" + pid[p] + " sofreu preempcao -> fila BAIXA");
            }
        }
    }

    /** (B) Processos cuja chegada == tempo entram na fila ALTA (status PRONTO). */
       static void moverNovosParaFilaAlta(int t) {
        for (int i = 0; i < N_PROCESSOS; i++) {
            if (status[i] == NOVO && chegada[i] <= t) {
                status[i] = PRONTO;
                prioridade[i] = ALTA;
                enfileirar(Q_ALTA, i);
                log(t, "P" + pid[i] + " criado -> fila ALTA");
            }
        }
    }

     /**
     * (B) Escolhe o próximo processo: fila ALTA tem preferência;
     * se estiver vazia, usa a fila BAIXA. Devolve -1 se as duas estiverem vazias.
     */
      static int escolherProximo() {
        if (!vazia(Q_ALTA)) return desenfileirar(Q_ALTA);
        if (!vazia(Q_BAIXA)) return desenfileirar(Q_BAIXA);
        return -1;
    }

   /**
     * (B) Executa o processo p por até um QUANTUM, avançando o relógio (tempo).
     * Para a execução quando:
     *   - tempoRestante chegar a 0                  -> devolve FINALIZOU
     *   - o processo chegar ao ponto de pedir I/O   -> devolve PEDIU_IO
     *   - acabar o quantum                          -> devolve PERDEU_QUANTUM
     * Cada unidade de CPU executada diminui tempoRestante[p].
     */
   static int executar(int p) {
        log(tempo, "CPU executa P" + pid[p]);
        int usado = 0;

        while (usado < QUANTUM) {
            tempo++;
            usado++;
            tempoRestante[p]--;

            int cpuExecutada = cpuTotal[p] - tempoRestante[p];

            if (tempoRestante[p] == 0) {
                return FINALIZOU;
            }
            if (proximoIO[p] != NENHUM && cpuExecutada >= cpuAteIO[p]) {
                return PEDIU_IO;
            }

            // Só atualiza eventos no meio do quantum se o processo ainda for continuar na CPU
            if (usado < QUANTUM) {
                moverNovosParaFilaAlta(tempo);
                atualizarIO(tempo);
            }
        }

        return PERDEU_QUANTUM;
    }
    // =====================================================================
    // PESSOA C - I/O, LOGS E MÉTRICAS
    // =====================================================================

    /**
     * (C) Bloqueia o processo p para I/O: define a duração conforme o dispositivo,
     * marca o processo como BLOQUEADO, calcula fimIO, coloca na fila de I/O,
     * contabiliza o evento e registra no log.
     */

    static void bloquearParaIO(int p) {
        int duracao;
        switch (proximoIO[p]) {
            case DISCO:      duracao = IO_DISCO;      break;
            case FITA:       duracao = IO_FITA;       break;
            default:         duracao = IO_IMPRESSORA; break;
        }
        tempoIO[p] = duracao;

        status[p] = BLOQUEADO;
        fimIO[p] = tempo + duracao;
        tempoTotalIO[p] += duracao;
        contagemIO[proximoIO[p]]++;
        enfileirar(Q_IO, p);

        log(tempo, "P" + pid[p] + " solicitou I/O " + nomeDispositivo(proximoIO[p])
                + " por " + duracao + " unidades");
    }

     /**
     * (C) Verifica quem terminou o I/O no instante t e devolve para a fila correta:
     *   DISCO      -> fila BAIXA
     *   FITA       -> fila ALTA
     *   IMPRESSORA -> fila ALTA
     * Depois: status[p] = PRONTO, proximoIO[p] = NENHUM, log do retorno.
     */
    static void atualizarIO(int t) {
        int quantos = tam[Q_IO];
        for (int k = 0; k < quantos; k++) {
            int p = desenfileirar(Q_IO);

            if (fimIO[p] <= t) {
                int dispositivo = proximoIO[p];
                String destino;

                if (dispositivo == DISCO) {
                    prioridade[p] = BAIXA;
                    enfileirar(Q_BAIXA, p);
                    destino = "BAIXA";
                } else {
                    prioridade[p] = ALTA;
                    enfileirar(Q_ALTA, p);
                    destino = "ALTA";
                }

                status[p] = PRONTO;
                proximoIO[p] = NENHUM;
                log(t, "P" + pid[p] + " retornou do " + nomeDispositivo(dispositivo)
                        + " -> fila " + destino);
            } else {
                enfileirar(Q_IO, p);
            }
        }
    }

    /** (C) Nome legível do dispositivo, para os logs. */
     static String nomeDispositivo(int d) {
        switch (d) {
            case DISCO:      return "DISCO";
            case FITA:       return "FITA";
            case IMPRESSORA: return "IMPRESSORA";
            default:         return "NENHUM";
        }
    }

    /** (C) Imprime um evento no formato do enunciado: "[t=003] mensagem". */
    static void log(int t, String mensagem) {
        System.out.printf("[t=%03d] %s%n", t, mensagem);
    }

   /**
     * (C) Imprime o resumo final:
     *  - processos finalizados e tempo total
     *  - quantidade de preempções (totalPreempcoes)
     *  - eventos de I/O por tipo (contagemIO)
     *  - tempo médio de espera e de retorno (turnaround)
     *  - percentual de CPU ociosa (cpuOciosa / tempo)
     */
    static void imprimirResumo() {
        int finalizados = 0;
        double somaRetorno = 0;
        double somaEspera = 0;

        for (int p = 0; p < N_PROCESSOS; p++) {
            if (status[p] == FINALIZADO) {
                finalizados++;
                int retorno = tempoFim[p] - chegada[p];
                int espera  = retorno - cpuTotal[p] - tempoTotalIO[p];
                tempoEspera[p] = espera;
                somaRetorno += retorno;
                somaEspera  += espera;
            }
        }

        System.out.println();
        System.out.println("===== RESUMO FINAL =====");
        System.out.println("[fim] Processos finalizados: " + finalizados + "/" + N_PROCESSOS
                + " | tempo total: " + tempo);
        System.out.println("Preempcoes (fim de quantum): " + totalPreempcoes);
        System.out.println("Eventos de I/O -> disco: " + contagemIO[DISCO]
                + " | fita: " + contagemIO[FITA]
                + " | impressora: " + contagemIO[IMPRESSORA]);

        if (finalizados > 0) {
         System.out.printf(Locale.US, "Tempo medio de espera: %.2f%n", somaEspera / finalizados);
System.out.printf(Locale.US, "Tempo medio de retorno (turnaround): %.2f%n", somaRetorno / finalizados);
        }
        if (tempo > 0) {
            System.out.printf(Locale.US, "CPU ociosa: %.2f%% (%d de %d unidades)%n",
        100.0 * cpuOciosa / tempo, cpuOciosa, tempo);
        }
    }

    // =====================================================================
    // PROGRAMA PRINCIPAL
    // =====================================================================
    public static void main(String[] args) {
        System.out.println("[config] quantum=" + QUANTUM
                + " | processos=" + N_PROCESSOS
                + " | seed=" + SEMENTE);

        criarProcessos();   // A
        escalonar();        // B (usa funções de C)
        imprimirResumo();   // C
    }
}
