#include <stdio.h>
#include "position_tracker.h"

int is_valid_position(const Position *p) {
    if (!p)                                             return 0;
    if (p->latitude  < -90.0  || p->latitude  > 90.0)  return 0;
    if (p->longitude < -180.0 || p->longitude > 180.0) return 0;
    if (p->altitude  < 0.0)                             return 0;
    return 1;
}

void append_position(PositionHistory *h, const Position *p) {
    if (!h || !p) return;
    h->entries[h->next] = *p;
    h->next = (h->next + 1) % MAX_HISTORY;
    if (h->count < MAX_HISTORY)
        h->count++;
}
