package com.supercilex.robotscouter.data.model

import androidx.text.isDigitsOnly
import java.util.Collections

enum class TemplateType(val id: Int) {
    MATCH(0),
    PIT(1),
    EMPTY(2);

    companion object {
        val DEFAULT: TemplateType = MATCH
        /**
         * Identical to the native values() method except that this one returns an immutable [List]
         * instead of an [Array] which must be copied defensively.
         *
         * @see enumValues
         */
        val values: List<TemplateType> = Collections.unmodifiableList(values().toList())

        fun valueOf(id: Int): TemplateType = requireNotNull(coerce(id.toString())) {
            "Unknown template type: $id"
        }

        fun coerce(id: String?): TemplateType? = if (id?.isDigitsOnly() == true) {
            values.find { it.id == id.toInt() }
        } else {
            null
        }
    }
}
