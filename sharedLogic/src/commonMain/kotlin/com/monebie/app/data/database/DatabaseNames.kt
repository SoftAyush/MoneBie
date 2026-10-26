package com.aurex.app.data.database

/**
 * The on-disk name of the database, shared by every platform's driver.
 *
 * Named here rather than repeated in each `actual` because it is not only the
 * driver that needs it: wiping the app deletes this file by name, and a wipe
 * that misses because one copy of the string drifted would look like it worked
 * and leave everything in place.
 */
const val AUREX_DATABASE_NAME = "aurex.db"
