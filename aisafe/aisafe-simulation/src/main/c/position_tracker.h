#ifndef POSITION_TRACKER_H
#define POSITION_TRACKER_H

#include "structs.h"

/*
 * Returns 1 if the position contains valid geographic coordinates:
 *   latitude  in [-90,  90]
 *   longitude in [-180, 180]
 *   altitude  >= 0
 * Returns 0 otherwise (TC05 — invalid updates are rejected).
 */
int is_valid_position(const Position *p);

/*
 * Appends *p to the history buffer h if there is room (h->count < MAX_HISTORY).
 * Positions are stored in arrival order, supporting TC03 and TC06.
 */
void append_position(PositionHistory *h, const Position *p);

#endif /* POSITION_TRACKER_H */
