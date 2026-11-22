package com.ashstride.app.domain

import org.json.JSONArray
import org.json.JSONObject

data class BackupDay(
    val date: String,
    val steps: Int,
    val km: Double,
    val source: String,
    val goal: Int,
    val mended: Boolean,
    val mendedAt: Long?,
)

data class BackupXp(val epochMs: Long, val type: String, val amount: Int, val ref: String?)

data class BackupWeek(
    val weekStart: String,
    val offersJson: String,
    val picked: String?,
    val status: String,
    val resolvedAt: Long?,
)

data class BackupMend(val date: String, val usedAt: Long)

data class BackupBadge(val badgeId: String, val variant: String, val unlockedAt: Long, val awardedFor: String?)

/** Everything the app stores, in a form that does not depend on Room. */
data class BackupData(
    val exportedAt: Long,
    val goal: Int,
    val shards: Int,
    val menders: Int,
    val days: List<BackupDay>,
    val xp: List<BackupXp>,
    val weeks: List<BackupWeek>,
    val mends: List<BackupMend>,
    val badges: List<BackupBadge>,
)

/** JSON backup and CSV step export. Pure and tested; the Android side only moves bytes. */
object BackupCodec {
    const val FORMAT = "ashstride-backup"
    const val VERSION = 1
    const val CSV_HEADER = "date,steps,distance_km,goal,source,mended"

    private fun strOrNull(o: JSONObject, key: String): String? = if (o.isNull(key) || !o.has(key)) null else o.getString(key)
    private fun longOrNull(o: JSONObject, key: String): Long? = if (o.isNull(key) || !o.has(key)) null else o.getLong(key)

    fun encode(b: BackupData): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", VERSION)
        root.put("exportedAt", b.exportedAt)
        root.put("goal", b.goal)
        root.put("shards", b.shards)
        root.put("menders", b.menders)
        val days = JSONArray()
        for (d in b.days) {
            val o = JSONObject()
            o.put("date", d.date); o.put("steps", d.steps); o.put("km", d.km)
            o.put("source", d.source); o.put("goal", d.goal); o.put("mended", d.mended)
            if (d.mendedAt != null) o.put("mendedAt", d.mendedAt)
            days.put(o)
        }
        root.put("days", days)
        val xp = JSONArray()
        for (x in b.xp) {
            val o = JSONObject()
            o.put("at", x.epochMs); o.put("type", x.type); o.put("amount", x.amount)
            if (x.ref != null) o.put("ref", x.ref)
            xp.put(o)
        }
        root.put("xp", xp)
        val weeks = JSONArray()
        for (w in b.weeks) {
            val o = JSONObject()
            o.put("weekStart", w.weekStart); o.put("offers", w.offersJson); o.put("status", w.status)
            if (w.picked != null) o.put("picked", w.picked)
            if (w.resolvedAt != null) o.put("resolvedAt", w.resolvedAt)
            weeks.put(o)
        }
        root.put("weeks", weeks)
        val mends = JSONArray()
        for (m in b.mends) {
            val o = JSONObject()
            o.put("date", m.date); o.put("usedAt", m.usedAt)
            mends.put(o)
        }
        root.put("mends", mends)
        val badges = JSONArray()
        for (g in b.badges) {
            val o = JSONObject()
            o.put("id", g.badgeId); o.put("variant", g.variant); o.put("at", g.unlockedAt)
            if (g.awardedFor != null) o.put("for", g.awardedFor)
            badges.put(o)
        }
        root.put("badges", badges)
        return root.toString()
    }

    /** @throws IllegalArgumentException when the text is not an Ashstride backup. */
    fun decode(text: String): BackupData {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a backup file")
        }
        if (root.optString("format") != FORMAT) throw IllegalArgumentException("Not an Ashstride backup")
        if (root.optInt("version", 0) > VERSION) throw IllegalArgumentException("Backup is from a newer version of the app")
        val days = ArrayList<BackupDay>()
        val da = root.getJSONArray("days")
        for (i in 0 until da.length()) {
            val o = da.getJSONObject(i)
            days.add(BackupDay(o.getString("date"), o.getInt("steps"), o.getDouble("km"), o.getString("source"), o.getInt("goal"), o.optBoolean("mended", false), longOrNull(o, "mendedAt")))
        }
        val xp = ArrayList<BackupXp>()
        val xa = root.optJSONArray("xp") ?: JSONArray()
        for (i in 0 until xa.length()) {
            val o = xa.getJSONObject(i)
            xp.add(BackupXp(o.getLong("at"), o.getString("type"), o.getInt("amount"), strOrNull(o, "ref")))
        }
        val weeks = ArrayList<BackupWeek>()
        val wa = root.optJSONArray("weeks") ?: JSONArray()
        for (i in 0 until wa.length()) {
            val o = wa.getJSONObject(i)
            weeks.add(BackupWeek(o.getString("weekStart"), o.getString("offers"), strOrNull(o, "picked"), o.getString("status"), longOrNull(o, "resolvedAt")))
        }
        val mends = ArrayList<BackupMend>()
        val ma = root.optJSONArray("mends") ?: JSONArray()
        for (i in 0 until ma.length()) {
            val o = ma.getJSONObject(i)
            mends.add(BackupMend(o.getString("date"), o.getLong("usedAt")))
        }
        val badges = ArrayList<BackupBadge>()
        val ba = root.optJSONArray("badges") ?: JSONArray()
        for (i in 0 until ba.length()) {
            val o = ba.getJSONObject(i)
            badges.add(BackupBadge(o.getString("id"), o.getString("variant"), o.getLong("at"), strOrNull(o, "for")))
        }
        return BackupData(
            exportedAt = root.optLong("exportedAt", 0L),
            goal = root.optInt("goal", 10_000),
            shards = root.optInt("shards", 0),
            menders = root.optInt("menders", 0),
            days = days, xp = xp, weeks = weeks, mends = mends, badges = badges,
        )
    }

    fun toCsv(days: List<BackupDay>): String {
        val sb = StringBuilder(CSV_HEADER).append('\n')
        for (d in days.sortedBy { it.date }) {
            sb.append(d.date).append(',').append(d.steps).append(',')
                .append(String.format(java.util.Locale.US, "%.3f", d.km)).append(',')
                .append(d.goal).append(',').append(d.source).append(',').append(if (d.mended) 1 else 0).append('\n')
        }
        return sb.toString()
    }

    /**
     * Reads a step CSV. Needs a header with at least date and steps; distance_km, goal, source and mended are
     * optional (defaults: 0 km, [defaultGoal], IMPORTED, not mended). Bad rows are skipped and counted.
     */
    fun parseCsv(text: String, defaultGoal: Int): CsvResult {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
        if (lines.isEmpty()) return CsvResult(emptyList(), 0)
        val header = lines[0].lowercase().split(',').map { it.trim() }
        val iDate = header.indexOf("date")
        val iSteps = header.indexOf("steps")
        if (iDate < 0 || iSteps < 0) throw IllegalArgumentException("CSV needs date and steps columns")
        val iKm = header.indexOf("distance_km")
        val iGoal = header.indexOf("goal")
        val iSource = header.indexOf("source")
        val iMended = header.indexOf("mended")
        val rx = Regex("\\d{4}-\\d{2}-\\d{2}")
        val out = ArrayList<BackupDay>()
        var bad = 0
        for (line in lines.drop(1)) {
            val c = line.split(',').map { it.trim() }
            val date = c.getOrNull(iDate)
            val steps = c.getOrNull(iSteps)?.toIntOrNull()
            if (date == null || !rx.matches(date) || steps == null || steps < 0) { bad++; continue }
            out.add(
                BackupDay(
                    date = date,
                    steps = steps,
                    km = if (iKm >= 0) c.getOrNull(iKm)?.toDoubleOrNull() ?: 0.0 else 0.0,
                    source = if (iSource >= 0) c.getOrNull(iSource)?.ifEmpty { null } ?: "IMPORTED" else "IMPORTED",
                    goal = if (iGoal >= 0) c.getOrNull(iGoal)?.toIntOrNull() ?: defaultGoal else defaultGoal,
                    mended = if (iMended >= 0) c.getOrNull(iMended) == "1" else false,
                    mendedAt = null,
                ),
            )
        }
        return CsvResult(out, bad)
    }
}

data class CsvResult(val days: List<BackupDay>, val skipped: Int)
