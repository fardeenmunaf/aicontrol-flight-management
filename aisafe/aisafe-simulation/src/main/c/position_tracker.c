#include <stdio.h>
#include "position_tracker.h"

/**
 * @brief Validates that a position contains legal geographic coordinates.
 *
 * Checks latitude in [-90, 90], longitude in [-180, 180], and altitude >= 0.
 *
 * @param p Pointer to the position to validate.
 * @return 1 if valid, 0 if invalid or NULL.
 */
int is_valid_position(const Position *p) {
    if (!p)                                             return 0;
    if (p->latitude  < -90.0  || p->latitude  > 90.0)  return 0;
    if (p->longitude < -180.0 || p->longitude > 180.0) return 0;
    if (p->altitude  < 0.0)                             return 0;
    return 1;
}

/**
 * @brief Appends a position to a ring buffer history.
 *
 * When the buffer is full (MAX_HISTORY entries), the oldest entry is
 * overwritten. Both h and p must be non-NULL.
 *
 * @param h Pointer to the PositionHistory ring buffer.
 * @param p Pointer to the Position to append.
 */
void append_position(PositionHistory *h, const Position *p) {
    if (!h || !p) return;
    h->entries[h->next] = *p;
    h->next = (h->next + 1) % MAX_HISTORY;
    if (h->count < MAX_HISTORY)
        h->count++;
}
