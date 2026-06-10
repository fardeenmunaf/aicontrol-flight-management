# US108 — Enforce step-by-step simulation synchronization

---

## Fluxo geral do código

### Visão de alto nível

O motor de simulação é composto por **um processo pai** e **N processos filho** (um por voo), mais **três threads** dentro do processo pai. Todos comunicam através de um bloco de **memória partilhada POSIX**.

```
┌─────────────────────────────────────────────────────────────┐
│  PROCESSO PAI (simulation_engine.c)                         │
│                                                             │
│  Thread main ──────────────────────────────────────────>    │
│       │  loop por step                                      │
│       │  acorda Dispatcher → espera step_done              │
│       │                                                     │
│  Thread Dispatcher ──> sem_post(step_go) para cada filho    │
│       │                                                     │
│  Thread Results ───> sem_wait(step_done) para cada filho    │
│       │                                                     │
│  Thread Violations ─> deteta colisões, avança relógio       │
│                                                             │
│         ↕ memória partilhada (/aisafe_sim)                  │
├──────────┬──────────┬──────────┬──────────┬────────────────┤
│ FILHO 0  │ FILHO 1  │ FILHO 2  │ FILHO 3  │  ...           │
│  Voo A   │  Voo B   │  Voo C   │  Voo D   │                │
└──────────┴──────────┴──────────┴──────────┴────────────────┘
```

---

### Fase 1 — Arranque (`main` em `simulation_engine.c`)

**1. Criação da memória partilhada**

```c
int shm_fd = shm_open("/aisafe_sim", O_CREAT | O_RDWR, 0600);
ftruncate(shm_fd, sizeof(SharedBlock));
SharedBlock *shm = mmap(NULL, sizeof(SharedBlock), PROT_READ | PROT_WRITE, MAP_SHARED, shm_fd, 0);
```

`shm_open` cria um objeto de memória partilhada identificado pelo nome `/aisafe_sim`. `ftruncate` define o seu tamanho. `mmap` mapeia esse objeto para o espaço de endereçamento do processo — após `fork`, o filho herda o mesmo mapeamento, por isso pai e filhos acedem fisicamente às mesmas posições de memória.

**2. Carregamento de dados**

```c
load_simulation_settings("simulation_settings.json", &sim->config);
load_all_flight_plans("flightplansSim", sim);
```

Lê o ficheiro de configuração (duração, time step, limiares de segurança) e os planos de voo em JSON. Todos estes dados ficam dentro do `SharedBlock` para serem acessíveis pelos filhos sem IPC adicional.

**3. Fork dos processos filho**

```c
for (int i = 0; i < sim->num_flights; i++) {
    pid_t pid = fork();
    if (pid == 0) {
        run_flight_process(shm, i);   // filho entra aqui e nunca regressa
        exit(EXIT_SUCCESS);
    }
    pids[i] = pid;
}
```

`fork()` duplica o processo. No filho (`pid == 0`) chama-se `run_flight_process` que contém o ciclo de vida do voo. No pai o `fork` retorna o PID do filho, que é guardado para poder enviar sinais e fazer `waitpid` mais tarde.

**4. Criação das threads do pai**

```c
pthread_create(&thr_dispatcher,  NULL, dispatcher_thread,  &ctx);
pthread_create(&thr_results,     NULL, results_thread,     &ctx);
pthread_create(&thr_violations,  NULL, violations_thread,  &ctx);
```

As três threads são criadas e ficam imediatamente bloqueadas em `pthread_cond_wait`, à espera que a thread main as acorde uma a uma em cada step.

---

### Fase 2 — Ciclo de vida do processo filho (`flight_simulation.c`)

Cada filho corre `run_flight_process`:

```
filho arranca
    ↓
regista signal handlers (SIGUSR1, SIGTERM)
    ↓
sem_post(step_done)   ← avisa pai que está pronto
    ↓
┌─── loop infinito ──────────────────────────────────┐
│  sem_wait(step_go)    ← bloqueia até pai autorizar  │
│  compute_physics_step()  ← calcula física do voo    │
│  atualiza estado em shm (posição, massa, fase)       │
│  sem_post(step_done)  ← avisa pai que terminou       │
│  se (!is_active) → break                             │
└────────────────────────────────────────────────────┘
    ↓
exit
```

`compute_physics_step` calcula para cada time step: temperatura e pressão à altitude atual, sustentação e arrasto (fórmulas de `physics.c`), empuxo dos motores, taxa de subida/descida, combustível consumido, nova posição (lat/lon/alt). Todos os resultados são escritos diretamente no `SharedBlock` em memória partilhada.

---

### Fase 3 — Loop principal (`main` em `simulation_engine.c`)

```c
while (sim->current_time < sim->config.end_time) {

    // Determina quais os voos ativos neste step
    for (int i = 0; i < sim->num_flights; i++)
        was_active[i] = sim->flights[i].is_active &&
                        step_time >= sim->flights[i].departure_offset_s;

    // Passa controlo ao Dispatcher e bloqueia até ao fim do step
    pthread_mutex_lock(&ctx.mutex);
    memcpy(ctx.was_active, was_active, sizeof(was_active));
    ctx.current_thread = THREAD_DISPATCHER;
    pthread_cond_signal(&ctx.cond_dispatch);
    pthread_cond_wait(&ctx.cond_step_done, &ctx.mutex);   // ← bloqueia aqui
    pthread_mutex_unlock(&ctx.mutex);
}
```

A thread main é o **maestro**: para cada step define quais os voos ativos, acorda o Dispatcher e bloqueia em `cond_step_done`. Só acorda quando a thread Violations termina o seu trabalho e faz `pthread_cond_signal(&ctx.cond_step_done)`.

---

### Fase 4 — Sequência das três threads dentro de um step

As três threads executam **em sequência**, controladas por uma variável de estado (`ctx.current_thread`) e variáveis de condição. Nunca correm duas ao mesmo tempo.

```
main sinaliza THREAD_DISPATCHER
        ↓
Dispatcher acorda
  └─ para cada voo ativo: sem_post(step_go[i])  ← dispara os filhos
  └─ sinaliza THREAD_RESULTS
        ↓
Results acorda
  └─ para cada voo ativo: sem_wait(step_done[i])  ← espera todos os filhos
  └─ regista posições em histories[]
  └─ sinaliza THREAD_VIOLATIONS
        ↓
Violations acorda
  └─ check_violations(): deteta colisões e esgotamento de combustível
  └─ avança sim->current_time += time_step
  └─ imprime tabela de física do step
  └─ imprime mensagens de aterragem/escala
  └─ sinaliza THREAD_IDLE (cond_step_done) → acorda main
        ↓
main verifica termination_requested, avança para próximo step
```

---

### Fase 5 — Deteção de violações (`check_violations`)

Corre dentro da thread Violations após todos os filhos terem terminado o step:

1. **Colisão** — para cada par de voos ativos calcula a distância mínima de separação usando as posições antes e depois do step (`min_separation_m`). Se for inferior a `collision_distance_threshold`, envia SIGTERM a ambos e marca-os como destruídos.

2. **Aviso de proximidade** — se a distância for inferior a `safety_distance_threshold` mas acima de `collision_distance_threshold`, imprime um aviso sem terminar os voos.

3. **Esgotamento de combustível** — se `current_mass - empty_weight - payload <= 0`, o voo é destruído.

4. **Reroute preditivo** — se `config.reroute` estiver ativo, projeta a posição futura de cada par de voos 500 km à frente. Se a separação prevista for inferior ao limiar, aplica uma viragem de 15° (regra ICAO para voos em rota de colisão frontal).

5. **Limiar de violações** — se o total de violações exceder `max_violations_threshold`, envia SIGTERM a todos os voos ativos e ativa `termination_requested`.

---

### Fase 6 — Encerramento

```
main sai do loop
    ↓
ctx.sim_done = true
pthread_cond_broadcast (acorda todas as threads)
    ↓
threads verificam sim_done e terminam
    ↓
pthread_join (espera que todas terminem)
    ↓
cleanup(): shm->terminate = true + sem_post(step_go) + waitpid
    ↓
finalize_and_write_report(): fork + pipe → report_generator
    ↓
sem_destroy + munmap + shm_unlink
```

`finalize_and_write_report` faz um `fork` extra para o processo `report_generator`. Envia-lhe `SimulationConfig` e `SimulationReport` através de um `pipe` (não de memória partilhada, porque o `report_generator` é um executável separado lançado com `execl`). O report_generator escreve o ficheiro JSON de resultados.

---

## User Story

> As a PO, I want the simulation engine to synchronize the simulation's step-by-step progression using semaphores, so that all flight processes and parent threads advance in lockstep through each simulation time step.

**Acceptance criteria:**
- Semaphores are used to control the progression of each simulation time step.

---

## O que foi desenvolvido

A sincronização passo-a-passo foi implementada em C usando **semáforos POSIX** em memória partilhada. O mecanismo garante que nenhum processo de voo avança para o próximo time step sem autorização do processo pai, e que o pai não avança sem que todos os filhos ativos tenham concluído o step atual.

Os ficheiros relevantes são:
- `structs.h` — define os semáforos dentro do bloco de memória partilhada
- `simulation_engine.c` — inicializa, usa e destrói os semáforos; contém as threads Dispatcher e Results
- `flight_simulation.c` — lado filho: espera autorização, calcula física, sinaliza conclusão

---

## Conceito: o que é um semáforo POSIX

Um semáforo é uma variável inteira não-negativa com duas operações atómicas:

- **`sem_wait(sem)`** — decrementa o valor. Se o valor for 0, bloqueia até outro processo/thread incrementar.
- **`sem_post(sem)`** — incrementa o valor e acorda um processo/thread bloqueado em `sem_wait`.

Neste projeto os semáforos são inicializados com valor `0`, o que significa que qualquer `sem_wait` bloqueia imediatamente — só desbloqueia quando alguém fizer `sem_post`.

---

## 1. Declaração dos semáforos — `structs.h`

```c
typedef struct {
    SimulationData sim;
    sem_t          step_go[MAX_FLIGHTS];    /* parent posts → child wakes  */
    sem_t          step_done[MAX_FLIGHTS];  /* child posts  → parent wakes */
    bool           terminate;
} SharedBlock;
```

### Explicação

| Campo | Tipo | Papel |
|---|---|---|
| `step_go[MAX_FLIGHTS]` | `sem_t[]` | O pai faz `sem_post` para autorizar o filho `i` a começar o seu step |
| `step_done[MAX_FLIGHTS]` | `sem_t[]` | O filho faz `sem_post` quando termina o step; o pai espera com `sem_wait` |

`SharedBlock` fica em **memória partilhada POSIX** (`shm_open` + `mmap`), por isso os semáforos são fisicamente acessíveis tanto pelo processo pai como por todos os processos filho — é este facto que torna possível a sincronização entre processos distintos.

`sem_t` é o tipo opaco definido pelo standard POSIX para representar um semáforo. O seu tamanho e representação interna variam com o sistema operativo.

---

## 2. Inicialização — `simulation_engine.c`

```c
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_init(&shm->step_go[i],   1, 0);
    sem_init(&shm->step_done[i], 1, 0);
}
```

### Assinatura de `sem_init`

```c
int sem_init(sem_t *sem, int pshared, unsigned int value);
```

| Argumento | Valor usado | Significado |
|---|---|---|
| `sem` | `&shm->step_go[i]` | Endereço do semáforo a inicializar |
| `pshared` | `1` | `1` = semáforo partilhado entre **processos** (precisa de estar em memória partilhada); `0` seria apenas entre threads do mesmo processo |
| `value` | `0` | Valor inicial. `0` significa que o primeiro `sem_wait` bloqueia de imediato — nenhum filho pode avançar antes de o pai fazer `sem_post` |

O loop cobre todos os `MAX_FLIGHTS` slots mesmo que nem todos sejam usados, para garantir que nenhum slot contém lixo de memória.

---

## 3. Barreira de arranque — `simulation_engine.c`

```c
/* Startup barrier: wait for all children to post step_done once */
for (int i = 0; i < sim->num_flights; i++)
    if (pids[i] > 0)
        sem_wait(&shm->step_done[i]);
```

### O que faz

Após o `fork` de todos os processos filho, o pai bloqueia aqui até cada filho ter sinalizado que está pronto (ver secção 4 abaixo). Sem esta barreira, o Dispatcher poderia começar a enviar steps antes de alguns filhos terem registado os seus signal handlers ou inicializado o seu estado.

`pids[i] > 0` garante que só se espera por slots onde foi feito `fork` com sucesso.

---

## 4. Processo filho — `flight_simulation.c`

```c
/* Sinaliza ao pai que o filho está pronto */
sem_post(&shm->step_done[flight_index]);

while (1) {
    int r;
    do {
        r = sem_wait(&shm->step_go[flight_index]);
    } while (r < 0 && errno == EINTR && !g_terminate);

    if (r < 0 || g_terminate || shm->terminate || !f->is_active)
        break;

    /* Executa a física do voo para este time step */
    compute_physics_step(&shm->sim, flight_index, &step);

    /* ... atualiza estado ... */

    sem_post(&shm->step_done[flight_index]);

    if (!f->is_active)
        break;
}
```

### Linha a linha

```c
sem_post(&shm->step_done[flight_index]);
```
O filho recém-criado faz `sem_post` em `step_done` para avisar o pai que terminou a inicialização e está à espera do primeiro step. Reutiliza `step_done` em vez de criar um terceiro semáforo.

---

```c
do {
    r = sem_wait(&shm->step_go[flight_index]);
} while (r < 0 && errno == EINTR && !g_terminate);
```
`sem_wait` bloqueia até o pai fazer `sem_post` em `step_go[flight_index]`.

O loop `do/while` trata a interrupção por sinal: quando um sinal chega (como SIGUSR1), `sem_wait` pode regressar com `r = -1` e `errno = EINTR` sem que o semáforo tenha sido decrementado — nesse caso tenta-se de novo. Se `g_terminate` foi ativado por SIGTERM, sai do loop sem retry.

---

```c
compute_physics_step(&shm->sim, flight_index, &step);
```
Calcula a física do voo (posição, velocidade, combustível) para o time step atual. Escreve o resultado em `shm->sim` (memória partilhada), onde o pai pode ler.

---

```c
sem_post(&shm->step_done[flight_index]);
```
Sinaliza ao pai que este step está concluído. O pai está bloqueado em `sem_wait(&step_done[i])` na thread Results (ver secção 6) e acorda com este `sem_post`.

---

## 5. Thread Dispatcher — `simulation_engine.c`

```c
static void *dispatcher_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;
    while (1) {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_DISPATCHER && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_dispatch, &ctx->mutex);
        if (ctx->sim_done) {
            pthread_mutex_unlock(&ctx->mutex);
            break;
        }

        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active));
        pthread_mutex_unlock(&ctx->mutex);

        for (int i = 0; i < ctx->num_flights; i++) {
            if (active[i])
                sem_post(&ctx->shm->step_go[i]);
        }

        pthread_mutex_lock(&ctx->mutex);
        ctx->current_thread = THREAD_RESULTS;
        pthread_cond_signal(&ctx->cond_results);
        pthread_mutex_unlock(&ctx->mutex);
    }
    return NULL;
}
```

### Linha a linha (parte dos semáforos)

```c
memcpy(active, ctx->was_active, sizeof(active));
pthread_mutex_unlock(&ctx->mutex);
```
Copia a lista de voos ativos para uma variável local **antes** de libertar o mutex. Desta forma o loop de `sem_post` abaixo não precisa de manter o mutex bloqueado — os semáforos são atómicos por si próprios e não precisam de proteção adicional.

---

```c
for (int i = 0; i < ctx->num_flights; i++) {
    if (active[i])
        sem_post(&ctx->shm->step_go[i]);
}
```
Para cada voo ativo, faz `sem_post` em `step_go[i]`. Cada `sem_post` incrementa o semáforo de 0 para 1, acordando o processo filho `i` que estava bloqueado em `sem_wait(&step_go[i])`. Todos os filhos ativos são disparados praticamente ao mesmo tempo (sequencialmente no loop, mas cada filho fica desbloqueado assim que o seu `sem_post` é executado).

---

```c
ctx->current_thread = THREAD_RESULTS;
pthread_cond_signal(&ctx->cond_results);
```
Passa o controlo à thread Results. O Dispatcher não espera pelos filhos — essa responsabilidade é da thread Results.

---

## 6. Thread Results — `simulation_engine.c`

```c
static void *results_thread(void *arg) {
    SimContext *ctx = (SimContext *)arg;
    while (1) {
        pthread_mutex_lock(&ctx->mutex);
        while (ctx->current_thread != THREAD_RESULTS && !ctx->sim_done)
            pthread_cond_wait(&ctx->cond_results, &ctx->mutex);
        if (ctx->sim_done) {
            pthread_mutex_unlock(&ctx->mutex);
            break;
        }

        bool active[MAX_FLIGHTS];
        memcpy(active, ctx->was_active, sizeof(active));
        pthread_mutex_unlock(&ctx->mutex);

        for (int i = 0; i < ctx->num_flights; i++) {
            if (active[i])
                sem_wait(&ctx->shm->step_done[i]);
        }

        /* Regista posições e passa controlo à thread Violations */
        ...
    }
    return NULL;
}
```

### Linha a linha (parte dos semáforos)

```c
for (int i = 0; i < ctx->num_flights; i++) {
    if (active[i])
        sem_wait(&ctx->shm->step_done[i]);
}
```
Bloqueia até **cada** filho ativo fazer `sem_post(&step_done[i])`. Só avança para o próximo step quando todos os filhos tiverem terminado o step atual. Este é o ponto onde o lockstep é efetivamente garantido: se um filho for mais lento, o pai fica aqui à espera.

O loop é sequencial (espera pelo filho 0, depois pelo 1, etc.), mas isso não introduz atrasos em condições normais porque os filhos correm em paralelo — o pai apenas confirma que todos terminaram.

---

## 7. Cleanup — `simulation_engine.c`

### Em caso de erro antecipado

```c
static void cleanup(SharedBlock *shm, pid_t *pids, int n) {
    shm->terminate = true;

    /* Unblock children that are waiting on step_go */
    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            sem_post(&shm->step_go[i]);

    for (int i = 0; i < n; i++)
        if (pids[i] > 0)
            waitpid(pids[i], NULL, 0);
}
```

```c
shm->terminate = true;
```
Ativa a flag que os filhos verificam após acordar do `sem_wait`. Quando o filho vê `shm->terminate == true`, sai do loop em vez de calcular física.

```c
sem_post(&shm->step_go[i]);
```
Se um filho estiver bloqueado em `sem_wait(&step_go[i])`, nunca acordaria sozinho. Este `sem_post` desbloqueia-o para que possa verificar `shm->terminate` e terminar corretamente. Sem esta linha, o `waitpid` abaixo ficaria bloqueado para sempre.

---

### No final normal da simulação

```c
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_destroy(&shm->step_go[i]);
    sem_destroy(&shm->step_done[i]);
}
munmap(shm, sizeof(SharedBlock));
shm_unlink("/aisafe_sim");
```

### Assinatura de `sem_destroy`

```c
int sem_destroy(sem_t *sem);
```

| Argumento | Valor usado | Significado |
|---|---|---|
| `sem` | `&shm->step_go[i]` | Endereço do semáforo a destruir |

`sem_destroy` liberta os recursos do semáforo. Deve ser chamado antes de `munmap` (que liberta a memória partilhada) — chamar `sem_destroy` após `munmap` seria acesso a memória já libertada.

`shm_unlink` remove o nome `/aisafe_sim` do sistema de ficheiros virtual, para que uma próxima execução não encontre um bloco de memória em estado inconsistente.

---

## Fluxo completo por time step

```
PAI — main loop          FILHO i              PAI — Dispatcher     PAI — Results
       |                    |                       |                    |
       |              sem_post(step_done)            |                    |
       |  ←─────────────────┘  (ready)              |                    |
sem_wait(step_done) (barreira arranque)             |                    |
       |                    |                       |                    |
       | ─── inicia loop ──────────────────→  sem_post(step_go[i])       |
       |                 sem_wait(step_go[i])        |                    |
       |                 compute_physics()           |                    |
       |                 sem_post(step_done[i]) ─────────────────→ sem_wait(step_done[i])
       |                    |                       |               regista posição
       |                    |                       |               verifica violações
       |                    |                 próximo step               |
```

Este padrão repete-se para cada time step. O sistema só avança quando **todos** os filhos ativos tiverem postado `step_done`, garantindo que nenhum voo fica à frente ou atrás dos outros na simulação.
