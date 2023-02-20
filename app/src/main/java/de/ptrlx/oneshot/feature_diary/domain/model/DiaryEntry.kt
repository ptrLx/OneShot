package de.ptrlx.oneshot.feature_diary.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.serialization.Serializable
import java.time.LocalDate

/**
 * A entry in the diary.
 *
 * @property date date of entry.
 * @property created timestamp of creation.
 * @property dayOfYear day of year of date that it was created [1-366].
 * @property relativePath path to image relative to base location.
 * @property happiness happiness of this day.
 * @property motivation motivation slogan for this day.
 * @property textContent entry in diary.
 */
@Entity
@Serializable(with = DiaryEntrySerializer::class)
data class DiaryEntry(
    @PrimaryKey
    val date: LocalDate,
    val created: Long,
    val dayOfYear: Int,
    val relativePath: String,
    val happiness: HappinessType = HappinessType.NOT_SPECIFIED,
    val motivation: String = "",
    val textContent: String = ""
)
