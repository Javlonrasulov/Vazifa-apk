package uz.vazifa.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uz.vazifa.app.data.remote.ChatMessageDto
import uz.vazifa.app.data.remote.RoomMessageDto
import uz.vazifa.app.domain.model.ChatMessage
import uz.vazifa.app.domain.model.RoomMessage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatMessageCache @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val gson = Gson()
    private val dmDir = File(context.filesDir, "chat_cache/dm").also { it.mkdirs() }
    private val roomDir = File(context.filesDir, "chat_cache/room").also { it.mkdirs() }

    suspend fun loadDm(peerId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        readDtos(File(dmDir, "$peerId.json")).mapNotNull { safeToDomain(it) }
    }

    suspend fun clearDm(peerId: String) = withContext(Dispatchers.IO) {
        File(dmDir, "$peerId.json").delete()
    }

    suspend fun saveDm(peerId: String, dtos: List<ChatMessageDto>) = withContext(Dispatchers.IO) {
        if (dtos.isEmpty()) return@withContext
        File(dmDir, "$peerId.json").writeText(gson.toJson(dtos))
    }

    suspend fun loadRoom(roomId: String): List<RoomMessage> = withContext(Dispatchers.IO) {
        readRoomDtos(File(roomDir, "$roomId.json")).mapNotNull { safeRoomToDomain(it) }
    }

    suspend fun saveRoom(roomId: String, dtos: List<RoomMessageDto>) = withContext(Dispatchers.IO) {
        if (dtos.isEmpty()) return@withContext
        File(roomDir, "$roomId.json").writeText(gson.toJson(dtos))
    }

    private fun readDtos(file: File): List<ChatMessageDto> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<ChatMessageDto>>() {}.type
            gson.fromJson<List<ChatMessageDto>>(file.readText(), type)
        }.getOrDefault(emptyList())
    }

    private fun readRoomDtos(file: File): List<RoomMessageDto> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<RoomMessageDto>>() {}.type
            gson.fromJson<List<RoomMessageDto>>(file.readText(), type)
        }.getOrDefault(emptyList())
    }
}

fun safeToDomain(dto: ChatMessageDto): ChatMessage? {
    return runCatching { dto.toDomain() }.getOrElse {
        if (dto.id.isBlank() || dto.senderId.isBlank() || dto.receiverId.isBlank()) return null
        ChatMessage(
            id = dto.id,
            senderId = dto.senderId,
            receiverId = dto.receiverId,
            type = uz.vazifa.app.domain.model.ChatMessageType.from(dto.type),
            body = dto.body,
            filePath = dto.filePath,
            fileName = dto.fileName,
            mimeType = dto.mimeType,
            meta = dto.meta.toDomainMessageMeta(dto.filePath),
            createdAt = dto.createdAt?.takeIf { it.isNotBlank() }
                ?: java.time.Instant.now().toString(),
        )
    }
}

fun safeRoomToDomain(dto: RoomMessageDto): RoomMessage? {
    return runCatching { dto.toDomain() }.getOrElse {
        if (dto.id.isBlank() || dto.roomId.isBlank() || dto.senderId.isBlank()) return null
        RoomMessage(
            id = dto.id,
            roomId = dto.roomId,
            senderId = dto.senderId,
            senderName = dto.sender?.fullName,
            type = uz.vazifa.app.domain.model.ChatMessageType.from(dto.type),
            body = dto.body,
            filePath = dto.filePath,
            fileName = dto.fileName,
            mimeType = dto.mimeType,
            meta = dto.meta.toDomainMessageMeta(dto.filePath),
            createdAt = dto.createdAt?.takeIf { it.isNotBlank() }
                ?: java.time.Instant.now().toString(),
        )
    }
}
