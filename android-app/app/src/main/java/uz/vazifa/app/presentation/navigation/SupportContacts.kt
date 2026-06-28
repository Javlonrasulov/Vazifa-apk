package uz.vazifa.app.presentation.navigation

data class SupportPhone(
    val label: String,
    val dialUri: String,
    val smsUri: String,
)

object SupportContacts {
    const val TELEGRAM_USERNAME = "javlon_abdurasulov_dev"
    const val TELEGRAM_URL = "https://t.me/$TELEGRAM_USERNAME"

    val phones = listOf(
        SupportPhone(
            label = "+998 95 173 96 06",
            dialUri = "tel:+998951739606",
            smsUri = "smsto:+998951739606",
        ),
        SupportPhone(
            label = "+998 93 559 96 99",
            dialUri = "tel:+998935599699",
            smsUri = "smsto:+998935599699",
        ),
    )
}
