package com.ashstride.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncBucketTest {
    private val slice = 15 * 60_000L

    @Test
    fun bucketStartRoundsDownToTheSlice() {
        assertEquals(1_000L, SyncPlanner.bucketStart(1_000L, 1_000L, slice))
        assertEquals(1_000L, SyncPlanner.bucketStart(1_000L, 1_000L + slice - 1, slice))
        assertEquals(1_000L + slice, SyncPlanner.bucketStart(1_000L, 1_000L + slice, slice))
        assertEquals(1_000L + 3 * slice, SyncPlanner.bucketStart(1_000L, 1_000L + 3 * slice + 5, slice))
    }

    @Test
    fun recordsBeforeTheRangeFallIntoTheFirstBucket() {
        assertEquals(1_000L, SyncPlanner.bucketStart(1_000L, 500L, slice))
    }

    @Test
    fun zeroSliceIsSafe() {
        assertEquals(1_000L, SyncPlanner.bucketStart(1_000L, 5_000L, 0L))
    }
}
