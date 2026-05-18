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

/**
 * TTS 语音播报管理器
 * 封装 TextToSpeech，支持播报课程信息
 * 处理 TTS 初始化失败的情况（静音模式 fallback）
 */
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
     * 播报模板："下一节是 {courseName}，在 {classroom} 上课，还有 {minutes} 分钟开始"
     * @param courseName 课程名称
     * @param classroom 教室
     * @param minutes 提前分钟数
     */
    suspend fun speakCourseReminder(courseName: String, classroom: String, minutes: Int) {
        val speed = settingsDataStore.ttsSpeed.first()
        val volume = settingsDataStore.ttsVolume.first()

        textToSpeech?.setSpeechRate(speed)

        // 使用指定模板播报
        val message = buildString {
            append("下一节是${courseName}")
            if (classroom.isNotBlank()) {
                append("，在${classroom}上课")
            }
            when {
                minutes <= 0 -> append("，即将开始")
                minutes < 60 -> append("，还有${minutes}分钟开始")
                else -> {
                    val hours = minutes / 60
                    val mins = minutes % 60
                    append("，还有${hours}小时")
                    if (mins > 0) append("${mins}分钟")
                    append("开始")
                }
            }
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
