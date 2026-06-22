package uz.vazifa.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class UzbekPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = UzPhoneFormatter.format(digits)
        val mapping = PhoneOffsetMapping(digits.length, formatted)
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

private class PhoneOffsetMapping(
    private val digitCount: Int,
    private val formatted: String,
) : OffsetMapping {

    /** Milliy raqamlar "+998 " dan keyin boshlanadi. */
    private val nationalStart = UzPhoneFormatter.PREFIX.length

    override fun originalToTransformed(offset: Int): Int {
        val o = offset.coerceIn(0, digitCount)
        if (o == 0) return nationalStart.coerceAtMost(formatted.length)
        var seen = 0
        var i = nationalStart
        while (i < formatted.length && seen < o) {
            if (formatted[i].isDigit()) seen++
            i++
        }
        return i.coerceIn(0, formatted.length)
    }

    override fun transformedToOriginal(offset: Int): Int {
        val t = offset.coerceIn(0, formatted.length)
        if (t <= nationalStart) return 0
        var seen = 0
        var i = nationalStart
        while (i < t && i < formatted.length) {
            if (formatted[i].isDigit()) seen++
            i++
        }
        return seen.coerceIn(0, digitCount)
    }
}
