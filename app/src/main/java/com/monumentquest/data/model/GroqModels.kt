package com.monumentquest.data.model

import com.google.gson.annotations.SerializedName

data class GroqMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class GroqRequest(
    @SerializedName("model") val model: String,
    @SerializedName("messages") val messages: List<GroqMessage>,
    @SerializedName("temperature") val temperature: Double = 0.7
)

data class GroqResponse(
    @SerializedName("id") val id: String,
    @SerializedName("choices") val choices: List<GroqChoice>
)

data class GroqChoice(
    @SerializedName("message") val message: GroqMessage,
    @SerializedName("finish_reason") val finishReason: String
)
