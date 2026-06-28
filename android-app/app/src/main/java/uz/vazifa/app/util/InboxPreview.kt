package uz.vazifa.app.util

import uz.vazifa.app.localization.AppLanguage
import uz.vazifa.app.localization.AppStrings

object InboxPreview {
    fun chatBody(type: String, body: String?, fileName: String?, lang: AppLanguage): String {
        val t = { key: String -> AppStrings.t(lang, key) }
        return when (type) {
            "text" -> body.orEmpty()
            "image" -> "📷 ${t("chat_photo")}"
            "video" -> "🎬 ${t("chat_video")}"
            "voice" -> "🎤 ${t("chat_voice_message")}"
            "audio" -> "🎵 ${fileName ?: "Audio"}"
            "file" -> "📎 ${fileName ?: t("chat_attachment")}"
            "location" -> "📍 ${t("chat_attach_location")}"
            "contact" -> "👤 ${t("chat_attach_contact")}"
            "sticker" -> "Sticker"
            "gif" -> "GIF"
            else -> body.orEmpty()
        }
    }
}
