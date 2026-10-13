package de.ptrlx.oneshot.feature_diary.domain.model

import de.ptrlx.oneshot.feature_diary.domain.util.HappinessType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import java.time.LocalDate

class InvalidDiaryEntryException(message: String) : Exception(message)

/**
 * The JSON configuration of the export format, used for both export and import
 * (docs/modernization/DATA_CONTRACT.md, section 5).
 *
 * `ignoreUnknownKeys` lets this version read an export written by a later one that added fields,
 * keeping the fields it knows instead of failing the whole file. It does not change what is written:
 * the encoded bytes stay identical to the default configuration, which the golden files pin.
 */
val diaryExportJson = Json { ignoreUnknownKeys = true }

/**
 * For serialization to JSON. Needed for Import / Export of database.
 */
object DiaryEntrySerializer : KSerializer<DiaryEntry> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DiaryEntry") {
        element<Long>("date")
        element<Long>("created")
        element<Int>("dayOfYear")
        element<String>("relativePath")
        element<String>("happiness")
        element<String>("motivation")
        element<String>("textContent")
    }

    override fun serialize(encoder: Encoder, entry: DiaryEntry) =
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, entry.date.toEpochDay())
            encodeLongElement(descriptor, 1, entry.created)
            encodeIntElement(descriptor, 2, entry.dayOfYear)
            encodeStringElement(descriptor, 3, entry.relativePath)
            encodeStringElement(descriptor, 4, entry.happiness.toString())
            encodeStringElement(descriptor, 5, entry.motivation)
            encodeStringElement(descriptor, 6, entry.textContent)
        }

    override fun deserialize(decoder: Decoder): DiaryEntry =
        decoder.decodeStructure(descriptor) {
            var date = LocalDate.MIN
            var created = 0L
            var dayOfYear = 0
            var relativePath = ""
            var happiness = HappinessType.NOT_SPECIFIED
            var motivation = ""
            var textContent = ""
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> date = LocalDate.ofEpochDay(decodeLongElement(descriptor, 0))
                    1 -> created = decodeLongElement(descriptor, 1)
                    2 -> dayOfYear = decodeIntElement(descriptor, 2)
                    3 -> relativePath = decodeStringElement(descriptor, 3)
                    4 -> try {
                        happiness = HappinessType.valueOf(decodeStringElement(descriptor, 4))
                    } catch (e: IllegalArgumentException) {
                    }
                    5 -> motivation = decodeStringElement(descriptor, 5)
                    6 -> textContent = decodeStringElement(descriptor, 6)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            DiaryEntry(date, created, dayOfYear, relativePath, happiness, motivation, textContent)
        }
}