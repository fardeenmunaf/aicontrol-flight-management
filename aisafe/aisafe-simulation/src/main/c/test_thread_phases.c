/* Tests for the parent-process thread phase hand-off (US106):
 * dispatcher_thread posts step_go only for active flights, and
 * results_thread waits on step_done only for active flights, records their
 * positions, and advances the phase to THREAD_VIOLATIONS.
 *
 * dispatcher_thread/results_thread are `static` inside simulation_engine.c,
 * so this file includes that translation unit directly (renaming its main
 * so the two don't collide) — the same technique used in test_violations.c
 * and test_report.c. */
#define main __sim_engine_main
#include "simulation_engine.c"
#undef main

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, int ok) {
    printf("[%s] %s\n", ok ? PASS : FAIL, name);
    return ok ? 0 : 1;
}

#define SEM_GO_NAME   "/aisafe_test_phase_go"
#define SEM_DONE_NAME "/aisafe_test_phase_done"

static sem_t *fresh_semaphore(const char *name, unsigned int value) {
    sem_unlink(name);
    sem_t *s = sem_open(name, O_CREAT | O_EXCL, 0600, value);
    return s == SEM_FAILED ? NULL : s;
}

/* Mimics one child flight process: blocks on step_go, then posts step_done. */
typedef struct {
    sem_t *go;
    sem_t *done;
    volatile int ran;
} FakeChild;

static void *fake_child(void *arg) {
    FakeChild *c = (FakeChild *)arg;
    sem_wait(c->go);
    c->ran = 1;
    sem_post(c->done);
    return NULL;
}

/* Polls ctx->current_thread until it reaches `target` or the timeout
 * (in milliseconds) elapses; returns the phase observed at the end. */
static SimThreadPhase wait_for_phase(SimContext *ctx, SimThreadPhase target, int timeout_ms) {
    for (int i = 0; i < timeout_ms; i++) {
        pthread_mutex_lock(&ctx->mutex);
        SimThreadPhase cur = ctx->current_thread;
        pthread_mutex_unlock(&ctx->mutex);
        if (cur == target) return cur;
        usleep(1000);
    }
    pthread_mutex_lock(&ctx->mutex);
    SimThreadPhase cur = ctx->current_thread;
    pthread_mutex_unlock(&ctx->mutex);
    return cur;
}

static void trigger_dispatch(SimContext *ctx) {
    pthread_mutex_lock(&ctx->mutex);
    ctx->current_thread = THREAD_DISPATCHER;
    pthread_cond_signal(&ctx->cond_dispatch);
    pthread_mutex_unlock(&ctx->mutex);
}

static SharedBlock     g_shm;
static PositionHistory g_histories[1];

static int test_dispatcher_skips_inactive_flight(void) {
    memset(&g_shm, 0, sizeof(g_shm));
    memset(g_histories, 0, sizeof(g_histories));
    g_shm.step_go[0]   = fresh_semaphore(SEM_GO_NAME, 0);
    g_shm.step_done[0] = fresh_semaphore(SEM_DONE_NAME, 0);

    SimContext ctx;
    memset(&ctx, 0, sizeof(ctx));
    ctx.shm          = &g_shm;
    ctx.num_flights  = 1;
    ctx.histories    = g_histories;
    ctx.was_active[0] = false; /* flight not active this step */
    ctx.current_thread = THREAD_IDLE;
    pthread_mutex_init(&ctx.mutex, NULL);
    pthread_cond_init(&ctx.cond_dispatch, NULL);
    pthread_cond_init(&ctx.cond_results, NULL);
    pthread_cond_init(&ctx.cond_violations, NULL);

    pthread_t dispatcher, results;
    pthread_create(&dispatcher, NULL, dispatcher_thread, &ctx);
    pthread_create(&results, NULL, results_thread, &ctx);

    trigger_dispatch(&ctx);
    SimThreadPhase reached = wait_for_phase(&ctx, THREAD_VIOLATIONS, 500);

    int step_go_untouched   = sem_trywait(g_shm.step_go[0]) == -1;
    int step_done_untouched = sem_trywait(g_shm.step_done[0]) == -1;

    /* Shut both threads down. */
    pthread_mutex_lock(&ctx.mutex);
    ctx.sim_done = true;
    pthread_cond_signal(&ctx.cond_dispatch);
    pthread_cond_signal(&ctx.cond_results);
    pthread_mutex_unlock(&ctx.mutex);
    pthread_join(dispatcher, NULL);
    pthread_join(results, NULL);

    sem_close(g_shm.step_go[0]);   sem_unlink(SEM_GO_NAME);
    sem_close(g_shm.step_done[0]); sem_unlink(SEM_DONE_NAME);

    int ok = reached == THREAD_VIOLATIONS && step_go_untouched &&
             step_done_untouched && g_histories[0].count == 0;
    return check("Dispatcher/Results skip step_go/step_done for a flight "
                  "with was_active==false and still hand off to Violations", ok);
}

static int test_results_records_active_flight_position(void) {
    memset(&g_shm, 0, sizeof(g_shm));
    memset(g_histories, 0, sizeof(g_histories));
    g_shm.step_go[0]   = fresh_semaphore(SEM_GO_NAME, 0);
    g_shm.step_done[0] = fresh_semaphore(SEM_DONE_NAME, 0);
    g_shm.sim.flights[0].current_pos = (Position){41.0, -8.0, 1000.0, 0.0};

    SimContext ctx;
    memset(&ctx, 0, sizeof(ctx));
    ctx.shm           = &g_shm;
    ctx.num_flights   = 1;
    ctx.histories     = g_histories;
    ctx.was_active[0] = true;
    ctx.current_thread = THREAD_IDLE;
    pthread_mutex_init(&ctx.mutex, NULL);
    pthread_cond_init(&ctx.cond_dispatch, NULL);
    pthread_cond_init(&ctx.cond_results, NULL);
    pthread_cond_init(&ctx.cond_violations, NULL);

    FakeChild child = {g_shm.step_go[0], g_shm.step_done[0], 0};
    pthread_t child_th, dispatcher, results;
    pthread_create(&child_th, NULL, fake_child, &child);
    pthread_create(&dispatcher, NULL, dispatcher_thread, &ctx);
    pthread_create(&results, NULL, results_thread, &ctx);

    trigger_dispatch(&ctx);
    SimThreadPhase reached = wait_for_phase(&ctx, THREAD_VIOLATIONS, 500);

    pthread_join(child_th, NULL);

    /* Shut both parent threads down. */
    pthread_mutex_lock(&ctx.mutex);
    ctx.sim_done = true;
    pthread_cond_signal(&ctx.cond_dispatch);
    pthread_cond_signal(&ctx.cond_results);
    pthread_mutex_unlock(&ctx.mutex);
    pthread_join(dispatcher, NULL);
    pthread_join(results, NULL);

    sem_close(g_shm.step_go[0]);   sem_unlink(SEM_GO_NAME);
    sem_close(g_shm.step_done[0]); sem_unlink(SEM_DONE_NAME);

    int ok = reached == THREAD_VIOLATIONS && child.ran &&
             g_histories[0].count == 1 &&
             g_histories[0].entries[0].latitude == 41.0;
    return check("Dispatcher wakes the child via step_go, Results waits on "
                  "step_done and records its position, then hands off to "
                  "Violations", ok);
}

int main(void) {
    int failures = 0;

    failures += test_dispatcher_skips_inactive_flight();
    failures += test_results_records_active_flight_position();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
