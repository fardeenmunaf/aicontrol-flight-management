#include <stdio.h>
#include <string.h>
#include <unistd.h>     /* pipe(), read(), write(), fork() */
#include <sys/wait.h>   /* waitpid() */
#include "position_tracker.h"

#define PASS "\033[32mPASS\033[0m"
#define FAIL "\033[31mFAIL\033[0m"

static int check(const char *name, int ok) {
    printf("[%s] %s\n", ok ? PASS : FAIL, name);
    return ok ? 0 : 1;
}


static int test_valid_position(void) {
    Position valid = {41.0, -8.0, 5000.0, 0.0};
    return check("Valid position accepted", is_valid_position(&valid));
}

static int test_invalid_latitude(void) {
    Position p = {95.0, -8.0, 5000.0, 0.0};
    return check("Latitude > 90 rejected", !is_valid_position(&p));
}

static int test_invalid_longitude(void) {
    Position p = {41.0, 200.0, 5000.0, 0.0};
    return check("Longitude > 180 rejected", !is_valid_position(&p));
}

static int test_invalid_negative_latitude(void) {
    Position p = {-91.0, -8.0, 5000.0, 0.0};
    return check("Latitude < -90 rejected", !is_valid_position(&p));
}

static int test_invalid_altitude(void) {
    Position p = {41.0, -8.0, -1.0, 0.0};
    return check("Negative altitude rejected", !is_valid_position(&p));
}

static int test_null_position(void) {
    return check("NULL position rejected", !is_valid_position(NULL));
}


static int test_single_append(void) {
    PositionHistory h;
    memset(&h, 0, sizeof(h));
    Position p = {41.0, -8.0, 5000.0, 0.0};
    append_position(&h, &p);
    int ok = (h.count == 1 &&
              h.entries[0].latitude  == 41.0 &&
              h.entries[0].longitude == -8.0 &&
              h.entries[0].altitude  == 5000.0);
    return check("Main process stores latest position", ok);
}

static int test_multiple_appends(void) {
    PositionHistory h;
    memset(&h, 0, sizeof(h));
    Position p1 = {41.0, -8.0,  5000.0, 0.0};
    Position p2 = {41.5, -7.5,  9000.0, 1800.0};
    Position p3 = {42.0, -6.0, 11000.0, 3600.0};
    append_position(&h, &p1);
    append_position(&h, &p2);
    append_position(&h, &p3);
    int ok = (h.count == 3 &&
              h.entries[0].latitude == 41.0 &&
              h.entries[1].latitude == 41.5 &&
              h.entries[2].latitude == 42.0);
    return check("Past positions stored in order", ok);
}

static int test_history_full(void) {
    static PositionHistory h;
    memset(&h, 0, sizeof(h));
    Position old_pos = {10.0, 10.0, 1000.0, 0.0};
    Position new_pos = {20.0, 20.0, 2000.0, 0.0};
    for (int i = 0; i < MAX_HISTORY; i++)
        append_position(&h, &old_pos);
    append_position(&h, &new_pos);
    int ok = (h.count == MAX_HISTORY &&
              h.entries[0].latitude == 20.0 &&
              h.next == 1);
    return check("Full buffer overwrites oldest entry (ring buffer)", ok);
}

static int test_independent_histories(void) {
    PositionHistory h1, h2;
    memset(&h1, 0, sizeof(h1));
    memset(&h2, 0, sizeof(h2));

    Position pA = {41.0, -8.0, 5000.0, 0.0};
    Position pB = {48.0,  2.3, 9000.0, 0.0};

    append_position(&h1, &pA);
    append_position(&h2, &pB);

    int ok = (h1.count == 1 && h2.count == 1 &&
              h1.entries[0].latitude == 41.0 &&
              h2.entries[0].latitude == 48.0);
    return check("Multiple flights tracked independently", ok);
}


static int test_history_accessible(void) {
    PositionHistory h;
    memset(&h, 0, sizeof(h));
    Position p1 = {41.0, -8.0, 5000.0,    0.0};
    Position p2 = {41.3, -7.5, 8000.0, 1800.0};
    append_position(&h, &p1);
    append_position(&h, &p2);

    int altitude_ever_above_7000 = 0;
    for (int i = 0; i < h.count; i++)
        if (h.entries[i].altitude > 7000.0)
            altitude_ever_above_7000 = 1;

    return check("History supports safety analysis (altitude check)",
                 altitude_ever_above_7000);
}


static int test_pipe_position_update(void) {
    int fd[2];
    if (pipe(fd) < 0) {
        printf("[%s] Pipe() failed\n", FAIL);
        return 1;
    }

    pid_t pid = fork();
    if (pid == 0) {
        close(fd[0]);
        PipeMessage msg;
        msg.type      = MSG_POSITION_UPDATE;
        msg.flight_id = 1;
        msg.new_pos.latitude  = 41.26;
        msg.new_pos.longitude = -8.68;
        msg.new_pos.altitude  = 9000.0;
        msg.new_pos.timestamp = 1800.0;
        write(fd[1], &msg, sizeof(msg));
        close(fd[1]);
        _exit(0);
    }

    close(fd[1]);
    PipeMessage received;
    ssize_t n = read(fd[0], &received, sizeof(received));
    close(fd[0]);
    waitpid(pid, NULL, 0);

    int ok = (n == (ssize_t)sizeof(PipeMessage) &&
              received.type      == MSG_POSITION_UPDATE &&
              received.flight_id == 1 &&
              received.new_pos.latitude  == 41.26 &&
              received.new_pos.longitude == -8.68 &&
              received.new_pos.altitude  == 9000.0);
    return check("Flight sends position update through pipe", ok);
}

int main(void) {
    int failures = 0;


    failures += test_pipe_position_update();
    failures += test_single_append();
    failures += test_multiple_appends();
    failures += test_history_full();
    failures += test_independent_histories();
    failures += test_valid_position();
    failures += test_invalid_latitude();
    failures += test_invalid_longitude();
    failures += test_invalid_negative_latitude();
    failures += test_invalid_altitude();
    failures += test_null_position();
    failures += test_history_accessible();

    printf("\n%d failure(s)\n", failures);
    return failures > 0 ? 1 : 0;
}
