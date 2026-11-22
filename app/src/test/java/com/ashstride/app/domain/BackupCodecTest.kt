package com.ashstride.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupCodecTest {
    private val sample = BackupData(
        exportedAt = 1_700_000_000_000L, goal = 12_500, shards = 2, menders = 1,
        days = listOf(
            BackupDay("2026-11-20", 12_000, 9.2, "IMPORTED", 10_000, false, null),
            BackupDay("2026-11-19", 8_000, 6.1, "HEALTH_CONNECT", 10_000, true, 5L),
        ),
        xp = listOf(BackupXp(10L, "CHALLENGE", 300, "2026-11-14:EASY"), BackupXp(11L, "X", 5, null)),
        weeks = listOf(BackupWeek("2026-11-14", "[]", "2026-11-14:EASY", "WON", 99L), BackupWeek("2026-11-21", "[]", null, "OPEN", null)),
        mends = listOf(BackupMend("2026-07-25", 7L)),
        badges = listOf(BackupBadge("streak_100", "PURE", 3L, "2026-01-24"), BackupBadge("steps_100000", "STANDARD", 4L, null)),
    )

    @Test
    fun jsonRoundTripKeepsEverything() {
        assertEquals(sample, BackupCodec.decode(BackupCodec.encode(sample)))
    }

    @Test
    fun rejectsOtherFiles() {
        for (bad in listOf("hello", "{}", "{\"format\":\"other\"}")) {
            try {
                BackupCodec.decode(bad)
                fail("accepted $bad")
            } catch (e: IllegalArgumentException) {
                // expected
            }
        }
    }

    @Test
    fun rejectsNewerVersions() {
        val text = BackupCodec.encode(sample).replace("\"version\":1", "\"version\":99")
        try {
            BackupCodec.decode(text)
            fail("accepted a newer version")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("newer"))
        }
    }

    @Test
    fun csvRoundTrip() {
        val csv = BackupCodec.toCsv(sample.days)
        assertTrue(csv.startsWith(BackupCodec.CSV_HEADER))
        val back = BackupCodec.parseCsv(csv, 10_000)
        assertEquals(0, back.skipped)
        assertEquals(listOf("2026-11-19", "2026-11-20"), back.days.map { it.date })
        assertEquals(true, back.days[0].mended)
        assertEquals("HEALTH_CONNECT", back.days[0].source)
        assertEquals(9.2, back.days[1].km, 0.0001)
    }

    @Test
    fun csvNeedsOnlyDateAndStepsAndSkipsBadRows() {
        val r = BackupCodec.parseCsv("Date,Steps\n2026-01-01,5000\nnonsense\n2026-01-02,abc\n2026-01-03,7000\n", 8_000)
        assertEquals(2, r.days.size)
        assertEquals(2, r.skipped)
        assertEquals(8_000, r.days[0].goal)
        assertEquals("IMPORTED", r.days[0].source)
        assertNull(r.days[0].mendedAt)
    }

    @Test
    fun csvWithoutRequiredColumnsFails() {
        try {
            BackupCodec.parseCsv("a,b\n1,2\n", 10_000)
            fail("accepted")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }
}
