package com.supercilex.robotscouter.data.model

import com.supercilex.robotscouter.util.isNumber

enum class TemplateType(val id: Int) {
    MATCH(0),
    PIT(1),
    EMPTY(2);

    companion object {
        val DEFAULT: TemplateType = MATCH

        fun valueOf(id: Int): TemplateType = requireNotNull(coerce(id.toString())) {
            "Unknown template type: $id"
        }

        fun coerce(id: String?): TemplateType? = if (id?.isNumber() == true) {
            TemplateType.values().find { it.id == id.toInt() }
        } else {
            null
        }
    }
}
