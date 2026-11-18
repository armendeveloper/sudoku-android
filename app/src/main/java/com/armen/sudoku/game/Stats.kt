package com.armen.sudoku.game

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import com.armen.sudoku.engine.Difficulty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Results for one difficulty. A game counts as played once the player makes a move.
 * Best time only counts games won without hints; a perfect game has no mistakes and no hints.
 */
@Immutable
data class DifficultyStats(
    val played: Int = 0,
    val won: Int = 0,
    val perfect: Int = 0,
    val bestTime: Long? = null,
    val totalWinTime: Long = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
) {
    /** Percentage of played games that were won, or null before the first game. */
    val winRate: Int? get() = if (played == 0) null else won * 100 / played
    val averageTime: Long? get() = if (won == 0) null else totalWinTime / won
}

/**
 * All statistics. Pure and immutable: every update returns a new copy, which keeps the rules
 * easy to test. [dailyDays] holds the epoch days of every completed daily challenge.
 */
@Immutable
data class Stats(
    val byDifficulty: Map<Difficulty, DifficultyStats> = emptyMap(),
    val dailyDays: Set<Long> = emptySet(),
) {
    operator fun get(d: Difficulty): DifficultyStats = byDifficulty[d] ?: DifficultyStats()

    private fun with(d: Difficulty, change: (DifficultyStats) -> DifficultyStats) =
        copy(byDifficulty = byDifficulty + (d to change(this[d])))

    fun started(d: Difficulty) = with(d) { it.copy(played = it.played + 1) }

    /** A loss, a game finished with Solve, or an abandoned game: ends the win streak. */
    fun lost(d: Difficulty) = with(d) { it.copy(currentStreak = 0) }

    fun won(d: Difficulty, seconds: Long, mistakes: Int, hints: Int, dailyDay: Long?, today: Long): Pair<Stats, WinSummary> {
        val old = this[d]
        val countsForBest = hints == 0
        val newBest = countsForBest && (old.bestTime == null || seconds < old.bestTime)
        val streak = old.currentStreak + 1
        val next = with(d) {
            it.copy(
                won = it.won + 1,
                perfect = it.perfect + if (mistakes == 0 && hints == 0) 1 else 0,
                bestTime = if (newBest) seconds else it.bestTime,
                totalWinTime = it.totalWinTime + seconds,
                currentStreak = streak,
                bestStreak = maxOf(it.bestStreak, streak),
            )
        }.let { s -> if (dailyDay != null) s.copy(dailyDays = s.dailyDays + dailyDay) else s }
        val dailyStreak = if (dailyDay != null) next.dailyStreak(today) else null
        return next to WinSummary(newBestTime = newBest, streak = streak, dailyStreak = dailyStreak)
    }

    /** Consecutive completed days ending today, or ending yesterday if today isn't done yet. */
    fun dailyStreak(today: Long): Int {
        var day = if (today in dailyDays) today else today - 1
        var count = 0
        while (day in dailyDays) {
            count++
            day--
        }
        return count
    }

    val bestDailyStreak: Int
        get() {
            var best = 0
            var run = 0
            var previous = Long.MIN_VALUE
            for (day in dailyDays.sorted()) {
                run = if (day == previous + 1) run + 1 else 1
                best = maxOf(best, run)
                previous = day
            }
            return best
        }
}

/** Keeps [Stats] in SharedPreferences. */
class StatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("sudoku_stats", Context.MODE_PRIVATE)
    private val _stats = MutableStateFlow(load())
    val stats: StateFlow<Stats> = _stats.asStateFlow()

    fun update(transform: (Stats) -> Stats) {
        val next = transform(_stats.value)
        _stats.value = next
        save(next)
    }

    fun reset() = update { Stats() }

    private fun save(stats: Stats) = prefs.edit {
        clear()
        for (d in Difficulty.entries) {
            val s = stats[d]
            putInt("${d.name}_played", s.played)
            putInt("${d.name}_won", s.won)
            putInt("${d.name}_perfect", s.perfect)
            s.bestTime?.let { putLong("${d.name}_best", it) }
            putLong("${d.name}_total", s.totalWinTime)
            putInt("${d.name}_streak", s.currentStreak)
            putInt("${d.name}_best_streak", s.bestStreak)
        }
        putString(KEY_DAILY, stats.dailyDays.sorted().joinToString(","))
    }

    private fun load(): Stats = runCatching {
        Stats(
            byDifficulty = Difficulty.entries.associateWith { d ->
                DifficultyStats(
                    played = prefs.getInt("${d.name}_played", 0),
                    won = prefs.getInt("${d.name}_won", 0),
                    perfect = prefs.getInt("${d.name}_perfect", 0),
                    bestTime = if (prefs.contains("${d.name}_best")) prefs.getLong("${d.name}_best", 0) else null,
                    totalWinTime = prefs.getLong("${d.name}_total", 0),
                    currentStreak = prefs.getInt("${d.name}_streak", 0),
                    bestStreak = prefs.getInt("${d.name}_best_streak", 0),
                )
            },
            dailyDays = prefs.getString(KEY_DAILY, "").orEmpty()
                .split(",").filter { it.isNotBlank() }.map(String::toLong).toSet(),
        )
    }.getOrDefault(Stats())

    private companion object {
        const val KEY_DAILY = "daily_days"
    }
}
