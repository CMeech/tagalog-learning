package ca.cashmclean.tagalog.domain

internal fun requireNotBlank(value: String, field: String) {
    require(value.isNotBlank()) { "$field must not be blank" }
}

internal fun requireNotBlankIfPresent(value: String?, field: String) {
    require(value == null || value.isNotBlank()) { "$field must not be blank when provided" }
}
