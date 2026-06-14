/* Tests for the POSIX shared-memory setup used to bootstrap the simulation
 * (US105): shm_open/ftruncate/mmap, zero-initialization, leftover cleanup
 * from a crashed run, and visibility of writes across fork(). */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <fcntl.h>
#include <unistd.h>

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, int ok) {
    printf("[%s] %s\n", ok ? PASS : FAIL, name);
    return ok ? 0 : 1;
}

#define SHM_NAME "/aisafe_test_shm"

typedef struct {
    int  counter;
    char marker[16];
} TestBlock;

static int test_recreate_after_leftover(void) {
    shm_unlink(SHM_NAME);
    int fd1 = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, 0600);
    int created_first = fd1 >= 0;
    if (fd1 >= 0) close(fd1); /* simulate a crash: no shm_unlink */

    shm_unlink(SHM_NAME);
    int fd2 = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, 0600);
    int created_second = fd2 >= 0;

    if (fd2 >= 0) { close(fd2); shm_unlink(SHM_NAME); }

    return check("Shared-memory segment is recreated after shm_unlink, even "
                  "if a previous run left it behind", created_first && created_second);
}

static int test_mmap_is_zero_initialized(void) {
    shm_unlink(SHM_NAME);
    int fd = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, 0600);
    int ok = fd >= 0;

    if (ok) ok = ftruncate(fd, sizeof(TestBlock)) == 0;

    TestBlock *b = MAP_FAILED;
    if (ok) {
        b = mmap(NULL, sizeof(TestBlock), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        ok = b != MAP_FAILED;
    }

    if (ok) ok = b->counter == 0 && b->marker[0] == '\0';

    if (b != MAP_FAILED) munmap(b, sizeof(TestBlock));
    if (fd >= 0) close(fd);
    shm_unlink(SHM_NAME);

    return check("shm_open + ftruncate + mmap yields a zero-initialized block", ok);
}

static int test_writes_visible_across_fork(void) {
    shm_unlink(SHM_NAME);
    int fd = shm_open(SHM_NAME, O_CREAT | O_EXCL | O_RDWR, 0600);
    int ok = fd >= 0 && ftruncate(fd, sizeof(TestBlock)) == 0;

    TestBlock *b = MAP_FAILED;
    if (ok) {
        b = mmap(NULL, sizeof(TestBlock), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
        ok = b != MAP_FAILED;
    }

    if (ok) {
        b->counter = 0;

        pid_t pid = fork();
        if (pid == 0) {
            b->counter = 42;
            strncpy(b->marker, "child", sizeof(b->marker) - 1);
            munmap(b, sizeof(TestBlock));
            close(fd);
            _exit(0);
        }
        waitpid(pid, NULL, 0);

        ok = b->counter == 42 && strcmp(b->marker, "child") == 0;
        munmap(b, sizeof(TestBlock));
    }

    if (fd >= 0) close(fd);
    shm_unlink(SHM_NAME);

    return check("A child process inherits the mmap'd shared-memory block "
                  "and its writes are visible to the parent", ok);
}

int main(void) {
    int failures = 0;

    failures += test_recreate_after_leftover();
    failures += test_mmap_is_zero_initialized();
    failures += test_writes_visible_across_fork();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
