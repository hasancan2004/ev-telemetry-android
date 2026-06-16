package com.hasancankula.evtelemetry.chat.data

import com.google.ai.client.generativeai.GenerativeModel
import com.hasancankula.evtelemetry.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiService @Inject constructor() {

    // Google'ın en hızlı ve verimli modeli olan gemini-1.5-flash'ı güvenli API anahtarımızla başlatıyoruz
    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun getAiResponse(userMessage: String, fleetContext: String): String = withContext(Dispatchers.IO) {
        try {
            // Gemini'ye bir "Sistem Rolü" ve ön bilgi (Canlı Filo Verilerini) veriyoruz ki bir filo uzmanı gibi davransın
            val fullPrompt = """
                Sen bir Elektrikli Araç Filo Yönetim Asistanısın. Görevin, kullanıcının sorularını filonun canlı verilerine dayanarak yanıtlamaktır.
                Sana aşağıda filonun şu anki canlı durumunu veriyorum. Yanıtlarını bu verilere dayandır, eğer veri dışı bir soru gelirse kibarca sadece filo hakkında yardımcı olabileceğini belirt.
                Cevapların net, profesyonel ve kısa olsun.
                
                [CANLI FİLO VERİLERİ]
                $fleetContext
                
                [KULLANICI SORUSU]
                $userMessage
            """.trimIndent()

            val response = generativeModel.generateContent(fullPrompt)
            response.text ?: "Üzgünüm, şu an bu soruyu yanıtlayamıyorum."
        } catch (e: Exception) {
            "Hata oluştu: ${e.localizedMessage ?: "Bağlantı sağlanamadı."}"
        }
    }
}