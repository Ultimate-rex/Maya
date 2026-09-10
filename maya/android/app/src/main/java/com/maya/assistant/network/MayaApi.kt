package com.maya.assistant.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class ChatRequest(val text: String, val history: List<Map<String, String>> = emptyList())
data class ChatResponse(val intent: String, val parameters: Map<String, Any?>, val say: String)
data class TranscribeResponse(val text: String)
data class SpeakRequest(val text: String)

interface MayaApi {
    @POST("chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>

    @Multipart
    @POST("voice/transcribe")
    suspend fun transcribe(@Part audio: MultipartBody.Part): Response<TranscribeResponse>

    @POST("voice/speak")
    suspend fun speak(@Body request: SpeakRequest): Response<ResponseBody>
}
