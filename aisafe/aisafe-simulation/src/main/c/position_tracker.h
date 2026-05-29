#ifndef POSITION_TRACKER_H
#define POSITION_TRACKER_H

#include "structs.h"

/**
 * @brief Validates that a position contains legal geographic coordinates.
 *
 * Checks latitude in [-90, 90], longitude in [-180, 180], and altitude >= 0.
 *
 * @param p Pointer to the position to validate.
 * @return 1 if valid, 0 if invalid or NULL.
 */
int is_valid_position(const Position *p);

/**
 * @brief Appends a position to a ring buffer history.
 *
 * When the buffer is full (MAX_HISTORY entries), the oldest entry is overwritten.
 *
 * @param h Pointer to the PositionHistory ring buffer.
 * @param p Pointer to the Position to append.
 */
void append_position(PositionHistory *h, const Position *p);

#endif /* POSITION_TRACKER_H */
