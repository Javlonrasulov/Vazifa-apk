package uz.vazifa.app.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/** restDays: [2,3] yoki ba'zan string/number aralash JSON dan xavfsiz o'qish. */
class RestDaysDeserializer : JsonDeserializer<List<Int>?> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?,
    ): List<Int>? {
        if (json == null || json.isJsonNull) return null
        if (!json.isJsonArray) return null
        val days = json.asJsonArray.mapNotNull { element ->
            when {
                element.isJsonPrimitive && element.asJsonPrimitive.isNumber ->
                    element.asInt
                element.isJsonPrimitive && element.asJsonPrimitive.isString ->
                    element.asString.toIntOrNull()
                else -> null
            }
        }.filter { it in 0..6 }
        return days.distinct().sorted().takeIf { it.isNotEmpty() }
    }
}
