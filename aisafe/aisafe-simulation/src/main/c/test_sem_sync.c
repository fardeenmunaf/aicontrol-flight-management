/* Tests for the named-semaphore lockstep protocol used by the simulation
 * engine (US108): parent posts step_go[i], a "child" thread blocks on it,
 * does its work, then posts step_done[i] for the parent to wait on. */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <pthread.h>
#include <semaphore.h>
#include <fcntl.h>
#include <unistd.h>

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, int ok) {
    printf("[%s] %s\n", ok ? PASS : FAIL, name);
    return ok ? 0 : 1;
}

#define SEM_GO_NAME   "/aisafe_test_go"
#define SEM_DONE_NAME "/aisafe_test_done"

/* Re-create a named semaphore, as the engine does at startup: unlink any
 * leftover from a crashed previous run, then create fresh with O_EXCL. */
static sem_t *fresh_semaphore(const char *name, unsigned int value) {
    sem_unlink(name);
    sem_t *s = sem_open(name, O_CREAT | O_EXCL, 0600, value);
    return s == SEM_FAILED ? NULL : s;
}

static int test_recreate_after_unlink(void) {
    sem_t *s1 = fresh_semaphore(SEM_GO_NAME, 0);
    int created_first = (s1 != NULL);

    /* Simulate a crash: leave the semaphore behind without sem_unlink. */
    if (s1) sem_close(s1);

    sem_t *s2 = fresh_semaphore(SEM_GO_NAME, 0);
    int created_second = (s2 != NULL);

    if (s2) { sem_close(s2); sem_unlink(SEM_GO_NAME); }

    return check("Named semaphore is recreated after sem_unlink, even if a "
                  "previous run left it behind", created_first && created_second);
}

static int test_initial_value_is_zero(void) {
    sem_t *go = fresh_semaphore(SEM_GO_NAME, 0);
    int ok = go != NULL && sem_trywait(go) == -1; /* not signaled yet */

    if (go) { sem_close(go); sem_unlink(SEM_GO_NAME); }
    return check("step_go is created with initial value 0 (not yet signaled)", ok);
}

/* Mimics one child flight process: blocks on step_go, "computes" a step,
 * then signals step_done. */
typedef struct {
    sem_t *go;
    sem_t *done;
    int    steps;
    int   *order; /* records the sequence of (1=child-ran) markers */
    int   *next;
} ChildArgs;

static void *child_loop(void *arg) {
    ChildArgs *c = (ChildArgs *)arg;
    for (int i = 0; i < c->steps; i++) {
        sem_wait(c->go);
        c->order[(*c->next)++] = 1; /* child did its work */
        sem_post(c->done);
    }
    return NULL;
}

static int test_child_blocks_until_dispatched(void) {
    sem_t *go   = fresh_semaphore(SEM_GO_NAME, 0);
    sem_t *done = fresh_semaphore(SEM_DONE_NAME, 0);

    int order[2] = {0, 0};
    int next = 0;
    ChildArgs args = {go, done, 1, order, &next};

    pthread_t th;
    pthread_create(&th, NULL, child_loop, &args);

    /* Give the child a chance to run before being released — it must not
     * record anything yet. */
    usleep(20000);
    int blocked_before_go = (next == 0);

    order[next++] = 2; /* parent's "dispatch" marker */
    sem_post(go);

    sem_wait(done);
    int child_ran_after_go = (next == 2 && order[0] == 2 && order[1] == 1);

    pthread_join(th, NULL);
    sem_close(go); sem_close(done);
    sem_unlink(SEM_GO_NAME); sem_unlink(SEM_DONE_NAME);

    return check("Child step blocks on step_go until the parent dispatches, "
                  "and parent waits on step_done for the result",
                  blocked_before_go && child_ran_after_go);
}

static int test_multi_step_lockstep(void) {
    sem_t *go   = fresh_semaphore(SEM_GO_NAME, 0);
    sem_t *done = fresh_semaphore(SEM_DONE_NAME, 0);

    enum { steps = 5 };
    int order[steps * 2];
    int next = 0;
    ChildArgs args = {go, done, steps, order, &next};

    pthread_t th;
    pthread_create(&th, NULL, child_loop, &args);

    int ok = 1;
    for (int step = 0; step < steps; step++) {
        int before = next;
        sem_post(go);
        sem_wait(done);
        /* Exactly one unit of child work must have happened per step. */
        if (next != before + 1) ok = 0;
    }

    pthread_join(th, NULL);
    sem_close(go); sem_close(done);
    sem_unlink(SEM_GO_NAME); sem_unlink(SEM_DONE_NAME);

    return check("Each sem_post(step_go)/sem_wait(step_done) pair advances "
                  "exactly one simulation step", ok);
}

int main(void) {
    int failures = 0;

    failures += test_recreate_after_unlink();
    failures += test_initial_value_is_zero();
    failures += test_child_blocks_until_dispatched();
    failures += test_multi_step_lockstep();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
