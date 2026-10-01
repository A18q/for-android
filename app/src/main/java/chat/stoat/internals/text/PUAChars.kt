package chat.stoat.internals.text

internal const val PUA_MIN = 0xE0E0
internal const val PUA_MAX = 0xE0E6

internal fun String.stripPUAChars(): String {
    var hasPua = false
    for (i in 0 until length) {
        val code = this[i].code
        if (code in PUA_MIN..PUA_MAX) {
            hasPua = true
            break
        }
    }
    if (!hasPua) return this

    val sb = java.lang.StringBuilder(length)
    for (i in 0 until length) {
        val ch = this[i]
        if (ch.code !in PUA_MIN..PUA_MAX) {
            sb.append(ch)
        }
    }
    return sb.toString()
}

