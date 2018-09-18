package hamburg.remme.tinygit.event

import java.time.Instant
import java.util.UUID

abstract class Event(val uuid: UUID = UUID.randomUUID(), val created: Instant = Instant.now()) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        if (uuid != other.uuid) return false

        return true
    }

    override fun hashCode(): Int = uuid.hashCode()

}
