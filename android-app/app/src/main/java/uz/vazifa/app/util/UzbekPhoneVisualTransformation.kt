package uz.vazifa.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class UzbekPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = UzPhoneFormatter.format(text.text)
        val mapping = PhoneOffsetMapping(text.text.length, formatted)
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

private class PhoneOffsetMapping(
    private val digitCount: Int,
    private val formatted: String,
) : OffsetMapping {
    override fun originalToTransformed(offset: Int): Int =
        formattedOffsetForDigitIndex(offset.coerceIn(0, digitCount))

    override fun transformedToOriginal(offset: Int): Int =
        digitIndexForFormattedOffset(offset.coerceIn(0, formatted.length))
}

/** +998 XX XXX XX XX — raqam indeksidan formatdagi pozitsiya. */
internal fun formattedOffsetForDigitIndex(index: Int): Int = when {
    index <= 0 -> 5 // "+998 "
    index <= 2 -> 5 + index
    index <= 5 -> 8 + (index - 2)
    index <= 7 -> 13 + (index - 5)
    else -> 16 + (index - 7)
}

internal fun digitIndexForFormattedOffset(offset: Int): Int = when {
    offset <= 5 -> 0
    offset <= 7 -> offset - 5
    offset <= 11 -> 2 + (offset - 8)
    offset <= 14 -> 5 + (offset - 13)
    else -> 7 + (offset - 16)
}
