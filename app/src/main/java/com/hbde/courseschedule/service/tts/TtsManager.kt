package com.hbde.courseschedule.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.hbde.courseschedule.data.local.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {

    companion object {
        private const val TAG = "TtsManager"
        private const val UTTERANCE_ID = "course_reminder_utterance"
    }

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var initSuccess = false
    private val pendingMessages = mutableListOf<String>()

    /**
     * 初始化 TTS
     */
    fun init() {
        if (textToSpeech != null) return

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val tts = textToSpeech ?: return@TextToSpeech
                val result = tts.setLanguage(Locale.CHINESE)

                initSuccess = when (result) {
                    TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                        Log.w(TAG, "中文语音数据缺失或不支持，尝试使用默认语言")
                        val defaultResult = tts.setLanguage(Locale.getDefault())
                        defaultResult == TextToSpeech.LANG_COUNTRY_AVAILABLE
                                || defaultResult == TextToSpeech.LANG_AVAILABLE
                    }
                    else -> true
                }

                if (initSuccess) {
                    tts.setSpeechRate(1.0f)
                    tts.setPitch(1.0f)
                    isInitialized = true

                    // 处理待播报消息
                    pendingMessages.forEach { message ->
                        speakInternal(message)
                    }
                    pendingMessages.clear()
                } else {
                    Log.e(TAG, "TTS 语言设置失败")
                }
            } else {
                Log.e(TAG, "TTS 初始化失败，状态码: $status")
                initSuccess = false
            }
        }
    }

    /**
     * 播报消息
     */
    fun speak(message: String, params: HashMap<String, String>? = null) {
        if (!isInitialized || !initSuccess) {
            pendingMessages.add(message)
            // 如果未初始化，尝试初始化
            if (textToSpeech == null) {
                init()
            }
            return
        }
        speakInternal(message, params)
    }

    /**
     * 播报课程提醒
     * @param courseName 课程名称
     * @param classroom 教室
     * @param minutes 提前分钟数
     */
    suspend fun speakCourseReminder(courseName: String, classroom: String, minutes: Int) {
        val speed = settingsDataStore.ttsSpeed.first()
        val volume = settingsDataStore.ttsVolume.first()

        textToSpeech?.setSpeechRate(speed)

        val message = buildString {
            append("距离${courseName}还有${minutes}分钟")
            if (classroom.isNotBlank()) {
                append("，教室在${classroom}")
            }
            append("，请不要忘记带课本")
        }

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = UTTERANCE_ID
        // 音量参数（0.0 - 1.0）
        params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = volume.toString()

        speak(message, params)
    }

    private fun speakInternal(message: String, params: HashMap<String, String>? = null) {
        val tts = textToSpeech ?: return
        if (!initSuccess) return

        val utteranceParams = params ?: HashMap()
        if (!utteranceParams.containsKey(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID)) {
            utteranceParams[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = UTTERANCE_ID
        }

        tts.speak(message, TextToSpeech.QUEUE_FLUSH, utteranceParams)
    }

    /**
     * 停止播报
     */
    fun stop() {
        textToSpeech?.stop()
    }

    /**
     * 释放资源
     */
    fun shutdown() {
        textToSpeech?.let {
            it.stop()
            it.shutdown()
        }
        textToSpeech = null
        isInitialized = false
        initSuccess = false
        pendingMessages.clear()
    }

    /**
     * 检查 TTS 是否可用
     */
    fun isAvailable(): Boolean = isInitialized && initSuccess
}
