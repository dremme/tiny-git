package hamburg.remme.tinygit

import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Container for all properties that are application settings and also methods for handling formatting related
 * to settings.
 * Settings are persistent and are saved and retrieved from the local file system.
 */
class Settings {

    /**
     * The selected locale. Is [Locale.getDefault] by default.
     */
    val locale: Locale = Locale.getDefault()
    /**
     * The selected time zone. Is [ZoneId.systemDefault] by default.
     */
    val timeZone: ZoneId = ZoneId.systemDefault()
    /**
     * The selected format for dates. Is `yyyy-MM-dd` by default, using [locale] and [timeZone].
     */
    var dateFormat: DateTimeFormatter by lazyVar {
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withLocale(locale).withZone(timeZone)
    }
    /**
     * The selected format for dates with time. Is `yyyy-MM-dd HH:mm` by default, using [locale] and [timeZone].
     */
    var dateTimeFormat: DateTimeFormatter by lazyVar {
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(locale).withZone(timeZone)
    }
    /**
     * The currently selected and active Git repository.
     */
    var repository: File? = null

    /**
     * @param instant the instant in time to format.
     * @return formatted instant in time according to the current [dateFormat].
     */
    fun formatDate(instant: Instant): String {
        return dateFormat.format(instant)
    }

    /**
     * @param instant the instant in time to format.
     * @return formatted instant in time according to the current [dateTimeFormat].
     */
    fun formatDateTime(instant: Instant): String {
        return dateTimeFormat.format(instant)
    }

}
