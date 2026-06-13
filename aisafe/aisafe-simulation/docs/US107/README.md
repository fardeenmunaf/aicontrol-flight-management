# US107 — Notificar a Thread de Relatório via Variáveis de Condição na Detecção de Violações de Segurança

## Visão Geral

Este documento descreve todas as alterações de código feitas nesta user story, divididas em dois conjuntos distintos:

1. **Compatibilidade com macOS** — alterações necessárias para que a simulação compile e execute correctamente em macOS (além de Linux).
2. **Implementação do US107** — alterações que implementam a user story: notificação em tempo real da thread de relatório quando é detectada uma violação de segurança.

---

## Parte 1 — Compatibilidade com macOS

### Problema

A simulação utiliza memória partilhada entre processos (`shm_open` + `mmap`) e semáforos POSIX para sincronizar o processo pai com os processos filho criados via `fork` (um por voo). No Linux, semáforos anónimos inicializados com `sem_init(..., pshared=1, ...)` funcionam correctamente entre processos quando colocados dentro de um bloco de memória partilhada. No macOS, **isto não é suportado**: `sem_init` com `pshared != 0` está marcado como `__deprecated` e falha silenciosamente em tempo de execução, o que significa que os semáforos nunca funcionam entre pai e filho. Como resultado, os processos filho ficavam bloqueados indefinidamente à espera de um sinal que nunca chegava, e todos os voos permaneciam presos na fase `GATE` durante toda a simulação.

Um problema secundário era o flag de linker `-lrt` (biblioteca POSIX real-time), que não existe como biblioteca separada no macOS — os seus símbolos fazem parte da biblioteca C padrão do sistema.

---

### Correcção 1 — Makefile: flag `-lrt` condicional

**Ficheiro:** `Makefile`

**Alteração:** Detectar o sistema operativo em tempo de compilação e incluir `-lrt` apenas no Linux.

```makefile
# Antes
LDFLAGS = -lm -pthread -lrt

# Depois
UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Darwin)
    LDFLAGS = -lm -pthread
else
    LDFLAGS = -lm -pthread -lrt
endif
```

No macOS, `-lrt` não existe e provoca um erro de linker. No Linux, o flag continua a ser necessário para `clock_gettime`, `shm_open` e símbolos relacionados.

---

### Correcção 2 — `structs.h`: tipos condicionais dos campos de semáforo em `SharedBlock`

**Ficheiro:** `src/main/c/structs.h`

**Alteração:** No macOS, a struct `SharedBlock` passa a armazenar `sem_t *` (ponteiros para semáforos nomeados) em vez de `sem_t` (semáforos anónimos embutidos). No Linux a struct mantém-se inalterada.

```c
/* Antes */
typedef struct {
    SimulationData sim;
    sem_t          step_go[MAX_FLIGHTS];
    sem_t          step_done[MAX_FLIGHTS];
    bool           terminate;
    WindData       env_wind[MAX_FLIGHTS];
    sem_t          env_sem;
} SharedBlock;

/* Depois */
typedef struct {
    SimulationData sim;
#ifdef __APPLE__
    sem_t         *step_go[MAX_FLIGHTS];
    sem_t         *step_done[MAX_FLIGHTS];
#else
    sem_t          step_go[MAX_FLIGHTS];
    sem_t          step_done[MAX_FLIGHTS];
#endif
    bool           terminate;
    WindData       env_wind[MAX_FLIGHTS];
#ifdef __APPLE__
    sem_t         *env_sem;
#else
    sem_t          env_sem;
#endif
} SharedBlock;
```

Os handles de semáforos nomeados (`sem_t *` devolvidos por `sem_open`) são herdados pelo processo filho após `fork()`, pelo que este recebe handles válidos sem necessitar de qualquer configuração adicional.

**Também adicionados** — três macros de acesso que resolvem para `sem_t *` em ambas as plataformas, eliminando a necessidade de `#ifdef` em cada ponto de utilização:

```c
#ifdef __APPLE__
#define SEM_GO(shm, i)   ((shm)->step_go[i])
#define SEM_DONE(shm, i) ((shm)->step_done[i])
#define SEM_ENV(shm)     ((shm)->env_sem)
#else
#define SEM_GO(shm, i)   (&(shm)->step_go[i])
#define SEM_DONE(shm, i) (&(shm)->step_done[i])
#define SEM_ENV(shm)     (&(shm)->env_sem)
#endif
```

#### Nota sobre o tamanho da struct em cada plataforma

A struct `SharedBlock` tem tamanhos diferentes consoante o sistema operativo: no Linux, `sem_t` é embutido na struct (tipicamente ~32 bytes por semáforo); no macOS, `sem_t *` é apenas um ponteiro (8 bytes). **Isto não é problema** porque a memória partilhada só é partilhada entre processos **no mesmo sistema** — pai e filho compilam do mesmo binário, portanto o layout da struct é sempre consistente dentro de uma execução. Um binário compilado no Linux nunca partilha memória com um binário compilado no macOS.

---

### Correcção 3 — `simulation_engine.c`: inicialização dos semáforos

**Ficheiro:** `src/main/c/simulation_engine.c`

**Alteração:** Substituir `sem_init` por `sem_open` no macOS. Cada semáforo recebe um nome único. `sem_unlink` é chamado antes de `sem_open` para limpar semáforos residuais de uma execução anterior que tenha terminado de forma anormal.

```c
/* Antes */
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_init(&shm->step_go[i],   1, 0);
    sem_init(&shm->step_done[i], 1, 0);
}
sem_init(&shm->env_sem, 1, 1);

/* Depois */
#ifdef __APPLE__
for (int i = 0; i < MAX_FLIGHTS; i++) {
    char name[40];
    snprintf(name, sizeof(name), "/aisafe_go_%d", i);
    sem_unlink(name);
    shm->step_go[i] = sem_open(name, O_CREAT | O_EXCL, 0600, 0);
    snprintf(name, sizeof(name), "/aisafe_done_%d", i);
    sem_unlink(name);
    shm->step_done[i] = sem_open(name, O_CREAT | O_EXCL, 0600, 0);
}
sem_unlink("/aisafe_env");
shm->env_sem = sem_open("/aisafe_env", O_CREAT | O_EXCL, 0600, 1);
#else
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_init(&shm->step_go[i],   1, 0);
    sem_init(&shm->step_done[i], 1, 0);
}
sem_init(&shm->env_sem, 1, 1);
#endif
```

---

### Correcção 4 — `simulation_engine.c`: limpeza dos semáforos

**Ficheiro:** `src/main/c/simulation_engine.c`

**Alteração:** Substituir `sem_destroy` por `sem_close` + `sem_unlink` no macOS.

```c
/* Antes */
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_destroy(&shm->step_go[i]);
    sem_destroy(&shm->step_done[i]);
}
sem_destroy(&shm->env_sem);

/* Depois */
#ifdef __APPLE__
for (int i = 0; i < MAX_FLIGHTS; i++) {
    char name[40];
    sem_close(shm->step_go[i]);
    snprintf(name, sizeof(name), "/aisafe_go_%d", i);
    sem_unlink(name);
    sem_close(shm->step_done[i]);
    snprintf(name, sizeof(name), "/aisafe_done_%d", i);
    sem_unlink(name);
}
sem_close(shm->env_sem);
sem_unlink("/aisafe_env");
#else
for (int i = 0; i < MAX_FLIGHTS; i++) {
    sem_destroy(&shm->step_go[i]);
    sem_destroy(&shm->step_done[i]);
}
sem_destroy(&shm->env_sem);
#endif
```

---

### Correcção 5 — `simulation_engine.c`: substituição de todos os pontos de utilização de semáforos pelas macros

**Ficheiro:** `src/main/c/simulation_engine.c`

Os quatro pontos de utilização que anteriormente usavam `&shm->step_go[i]` / `&shm->step_done[i]` foram substituídos pelas macros neutras para a plataforma:

| Localização | Antes | Depois |
|---|---|---|
| `dispatcher_thread` | `sem_post(&ctx->shm->step_go[i])` | `sem_post(SEM_GO(ctx->shm, i))` |
| `results_thread` | `sem_wait(&ctx->shm->step_done[i])` | `sem_wait(SEM_DONE(ctx->shm, i))` |
| `cleanup` | `sem_post(&shm->step_go[i])` | `sem_post(SEM_GO(shm, i))` |
| barreira de arranque | `sem_wait(&shm->step_done[i])` | `sem_wait(SEM_DONE(shm, i))` |

---

### Correcção 6 — `environment.c`: substituição dos pontos de utilização de `env_sem` pelo macro

**Ficheiro:** `src/main/c/environment.c`

```c
/* Antes */
sem_wait(&ctx->shm->env_sem);
// ...
sem_post(&ctx->shm->env_sem);

/* Depois */
sem_wait(SEM_ENV(ctx->shm));
// ...
sem_post(SEM_ENV(ctx->shm));
```

---

### Correcção 7 — `flight_simulation.c`: substituição dos pontos de utilização de `step_go` / `step_done` pelos macros

**Ficheiro:** `src/main/c/flight_simulation.c`

```c
/* Antes */
sem_post(&shm->step_done[flight_index]);
// ...
do { r = sem_wait(&shm->step_go[flight_index]); } while (...);
// ...
sem_post(&shm->step_done[flight_index]);

/* Depois */
sem_post(SEM_DONE(shm, flight_index));
// ...
do { r = sem_wait(SEM_GO(shm, flight_index)); } while (...);
// ...
sem_post(SEM_DONE(shm, flight_index));
```

---

## Parte 2 — Implementação do US107

### User Story

> Como PO, quero que a thread de detecção de violações de segurança da simulação notifique a thread de geração de relatórios através de variáveis de condição quando ocorre uma violação de segurança, para que o relatório seja actualizado em tempo real com informação precisa.

### Critérios de Aceitação

| Critério | Estado |
|---|---|
| A thread de detecção de violações monitoriza a memória partilhada em busca de conflitos entre voos | Feito |
| Ao detectar uma violação, sinaliza a thread de relatório usando variáveis de condição | Feito |
| A thread de relatório, em espera na variável de condição, processa e regista imediatamente o evento (colisões e exaustão de combustível) | Feito |
| É utilizado mutex locking correcto para garantir notificação thread-safe | Feito |

---

### Arquitectura

A simulação utiliza um pipeline de threads no processo pai, cada uma à espera na sua variável de condição para ser activada:

```
Environment → Dispatcher → Results → Violations → Report → (próximo step)
```

Cada thread sinaliza a seguinte via `pthread_cond_signal`. A `violations_thread` e a `report_thread` são as duas threads centrais no US107.

---

### Alteração 1 — `structs.h`: adição de `logged_violation_idx` ao `SimContext`

**Ficheiro:** `src/main/c/structs.h`

Um novo campo regista quantas entradas `ViolationEvent` em `sim->report.violations[]` já foram registadas pela thread de relatório. Isto impede que eventos de steps anteriores sejam registados novamente.

```c
/* Antes */
bool  sim_done;
int   pending_violations;
bool  report_finalize;

/* Depois */
bool  sim_done;
int   pending_violations;
int   logged_violation_idx;   /* próximo índice em violations[] a registar na report_thread */
bool  report_finalize;
```

---

### Alteração 2 — `simulation_engine.c`: inicialização de `logged_violation_idx`

**Ficheiro:** `src/main/c/simulation_engine.c` — `main()`

```c
ctx.pending_violations   = 0;
ctx.logged_violation_idx = 0;   /* ← adicionado */
ctx.report_finalize      = false;
```

---

### Alteração 3 — `simulation_engine.c`: `violations_thread` sinaliza a `report_thread` **apenas** quando ocorre uma violação

**Ficheiro:** `src/main/c/simulation_engine.c` — `violations_thread()`

A thread conta as novas violações após cada step e só acorda a `report_thread` se houver pelo menos uma violação nova. Se não houver, sinaliza diretamente `cond_step_done` (passando a `THREAD_IDLE`) para não bloquear o pipeline desnecessariamente:

```c
int previous_violation_count = sim->total_violations_detected;
check_violations(sim, active, ctx->pids, ctx->num_flights);
int new_violations = sim->total_violations_detected - previous_violation_count;

// ... (imprimir tabela do step, mensagens de aterragem, etc.) ...

pthread_mutex_lock(&ctx->mutex);
if (new_violations > 0) {
    ctx->pending_violations += new_violations;
    ctx->current_thread = THREAD_REPORT;
    pthread_cond_signal(&ctx->cond_report);   // ← acorda a report_thread apenas se há violações
} else {
    ctx->current_thread = THREAD_IDLE;
    pthread_cond_signal(&ctx->cond_step_done); // ← liberta o main loop directamente
}
pthread_mutex_unlock(&ctx->mutex);
```

O mutex garante que a escrita em `pending_violations` e o sinal são atómicos do ponto de vista da thread de relatório.

---

### Alteração 4 — `simulation_engine.c`: `report_thread` processa e regista cada evento de violação

**Ficheiro:** `src/main/c/simulation_engine.c` — `report_thread()`

Esta é a alteração central do US107. A thread de relatório já esperava em `cond_report` e lia `pending_violations`. O novo código itera sobre cada novo `ViolationEvent` em `sim->report.violations[]` e regista os detalhes completos de cada um.

```c
/* Antes */
if (to_process > 0) {
    printf("[REPORT] Processed %d new violation event(s). Total recorded: %d\n",
           to_process, sim->report.total_violations_recorded);
    fflush(stdout);
}

/* Depois */
if (to_process > 0) {
    int total_recorded = sim->report.total_violations_recorded;
    for (int k = ctx->logged_violation_idx; k < total_recorded; k++) {
        ViolationEvent *evt = &sim->report.violations[k];
        double abs_time = sim->config.sim_start_seconds + evt->timestamp;
        int h = (int)(abs_time / 3600) % 24;
        int m = (int)(fmod(abs_time, 3600.0) / 60.0);
        printf("[REPORT] Violation #%d  t=%.0fs [%02d:%02d]  "
               "%s (%.4f°,%.4f°,%.0fm) <-> %s (%.4f°,%.4f°,%.0fm)  "
               "dist=%.1fm\n",
               k + 1, evt->timestamp, h, m,
               sim->flights[evt->flight_a_id].callsign,
               evt->pos_a.latitude, evt->pos_a.longitude, evt->pos_a.altitude,
               sim->flights[evt->flight_b_id].callsign,
               evt->pos_b.latitude, evt->pos_b.longitude, evt->pos_b.altitude,
               evt->distance);
    }
    ctx->logged_violation_idx = total_recorded;
    printf("[REPORT] %d new violation(s) processed. Total recorded: %d\n",
           to_process, total_recorded);
    fflush(stdout);
}
```

Cada `ViolationEvent` contém:
- `timestamp` — tempo de simulação em segundos no momento em que a violação ocorreu
- `flight_a_id` / `flight_b_id` — índices em `sim->flights[]` para obter os callsigns
- `pos_a` / `pos_b` — posições 3D (latitude, longitude, altitude) de ambas as aeronaves
- `distance` — separação mínima em metros no momento da detecção

Violações por exaustão de combustível também criam uma entrada `ViolationEvent` (com `flight_b_id == flight_a_id` como marcador), sendo igualmente registadas em tempo real pela thread de relatório.

---

### Modelo de thread-safety

```
violations_thread                    report_thread
─────────────────                    ─────────────
lock(mutex)
  pending_violations += N
  current_thread = THREAD_REPORT
  cond_signal(cond_report)      ───► acorda do cond_wait(cond_report, mutex)
unlock(mutex)                        mutex já adquirido ao acordar
                                       to_process = pending_violations
                                       pending_violations = 0
                                     unlock(mutex)
                                     [regista detalhes da violação — sem lock,
                                      violations[] só é escrito pela violations_thread
                                      que está agora inactiva]
```

O array `violations[]` e `total_violations_recorded` são escritos exclusivamente por `check_violations`, que corre dentro da `violations_thread` enquanto a thread de relatório está bloqueada. Quando a thread de relatório os lê, a `violations_thread` já libertou o mutex e está à espera do próximo step. Não é necessário locking adicional para a leitura do array.

---

---

## Parte 3 — Bugs detectados após testes

### Bug 1 — Violações por exaustão de combustível não eram registadas pela `report_thread`

**Ficheiro:** `src/main/c/simulation_engine.c` — `check_violations()`

**Problema:** `check_violations` incrementava `total_violations_detected` quando detectava exaustão de combustível, mas não adicionava nenhuma entrada ao array `violations[]`. Como `report_thread` itera de `logged_violation_idx` até `total_violations_recorded` (que só conta entradas no array), estas violações nunca eram registadas em detalhe — falhando o AC3.

**Correcção:** Ao detectar exaustão de combustível, é agora criada uma `ViolationEvent` em `violations[]`, usando `flight_b_id == flight_a_id` para distinguir este caso de uma colisão entre dois voos.

```c
/* Adicionado dentro do bloco de exaustão de combustível */
if (sim->report.total_violations_recorded < MAX_RECORDED_VIOLATIONS) {
    ViolationEvent *evt = &sim->report.violations[sim->report.total_violations_recorded];
    evt->timestamp   = sim->current_time;
    evt->flight_a_id = i;
    evt->flight_b_id = i; /* mesmo índice indica exaustão (evento de voo único) */
    evt->pos_a       = f->current_pos;
    evt->pos_b       = f->current_pos;
    evt->vel_a       = f->velocity;
    evt->vel_b       = f->velocity;
    evt->distance    = 0.0;
    sim->report.total_violations_recorded++;
}
```

A `report_thread` imprime uma mensagem diferente quando `flight_b_id == flight_a_id`:

```c
if (evt->flight_b_id == evt->flight_a_id) {
    printf("[REPORT] Violation #%d  t=%.0fs [%02d:%02d]  "
           "%s fuel exhaustion at (%.4f°,%.4f°,%.0fm)\n", ...);
} else {
    printf("[REPORT] Violation #%d  t=%.0fs [%02d:%02d]  "
           "%s <-> %s  dist=%.1fm\n", ...);
}
```

---

### Bug 2 — `finalize_and_write_report` sobrescrevia `total_violations_recorded` com o valor errado

**Ficheiro:** `src/main/c/simulation_engine.c` — `finalize_and_write_report()`

**Problema:** A linha

```c
sim->report.total_violations_recorded = sim->total_violations_detected;
```

substituía a contagem correcta de entradas no array `violations[]` pelo total geral de violações detectadas (que incluía exaustões de combustível mesmo quando estas não tinham entradas no array). O `report_generator` usaria então `total_violations_recorded` como contagem de entradas válidas, podendo ler dados não inicializados.

**Correcção:** A linha foi removida. Com o Bug 1 corrigido, `total_violations_recorded` é incrementado para todos os tipos de violação directamente em `check_violations`, pelo que o valor já está correcto quando `finalize_and_write_report` é chamada.

---

### Exemplo de output

```
  *** COLLISION: MAD02A <-> TAP02A  dist=0.0 m — both flights destroyed ***
[REPORT] Violation #1  t=19800s [05:30]  MAD02A (39.3003°,-7.9144°,10000m) <-> TAP02A (39.3003°,-7.9144°,10000m)  dist=0.0m
[REPORT] 1 new violation(s) processed. Total recorded: 1
```

---

### Planos de voo de teste

Estão disponíveis três planos de voo em `flightplansSim/`, todos com partida às **05:30 de 2026-06-22**:

| Ficheiro | Callsign | Rota | Combustível | Violação esperada |
|---|---|---|---|---|
| `TAP02A_1.json` | TAP02A | LIS → MAD | 42 000 kg | Colisão por distância (mesma rota que MAD02A) |
| `MAD02A_1.json` | MAD02A | LIS → MAD | 42 000 kg | Colisão por distância (mesma rota que TAP02A) |
| `OPO01A_1.json` | OPO01A | OPO → MAD | 5 000 kg | **Exaustão de combustível** |

#### TAP02A / MAD02A — teste de colisão por distância

Ambos os aviões seguem exactamente a mesma trajectória física a partir de LIS, com separação zero em cada step. A colisão é detectada e a `report_thread` é notificada via variável de condição.

#### OPO01A — teste de exaustão de combustível

O A380 parte de Porto com apenas 5 000 kg de combustível para uma viagem de ~700 km. O consumo real ultrapassa os 10 000 kg no primeiro step de 15 minutos, pelo que o combustível esgota imediatamente. A rota vai a ~41°N, bem a norte da rota LIS→MAD (~39–40°N), garantindo que não há sobreposição de distância com TAP02A/MAD02A.

Output esperado ao seleccionar os três voos:

```
  *** VIOLATION: OPO01A fuel exhausted — flight destroyed ***
[REPORT] Violation #1  t=19800s [05:30]  OPO01A fuel exhaustion at (41.2481°,-8.6814°,69m)
[REPORT] 1 new violation(s) processed. Total recorded: 1

  *** COLLISION: TAP02A <-> MAD02A  dist=0.0 m — both flights destroyed ***
[REPORT] Violation #2  t=19800s [05:30]  TAP02A (...) <-> MAD02A (...)  dist=0.0m
[REPORT] 1 new violation(s) processed. Total recorded: 2
```

Os dois tipos de violação distintos são ambos capturados pela `violations_thread` e notificados à `report_thread` via `cond_report`, demonstrando o mecanismo do US107.

Para executar: `make run`, depois seleccionar os voos pretendidos no menu interactivo.
