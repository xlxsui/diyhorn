package top.xlxs.diyhorn

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.KeyEvent
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class AudioAccessibilityService : AccessibilityService() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var buttonConfigs: MutableList<ButtonConfig>
    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val TAG = "AudioAccessibilityService"
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AudioAccessibilityService", "无障碍服务已连接");

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
            // 👇关键：允许监听按键
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 此方法用于处理无障碍事件，按键事件处理在 onKeyEvent 中
    }

    override fun onInterrupt() {
        // 服务中断时调用
        Log.i("AudioAccessibilityService", "无障碍服务已中断");
    }

    override fun onCreate() {
        super.onCreate()
        // 初始化 AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setOnAudioFocusChangeListener {}
                .build()
        }

        // 初始化数据库
        db = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "button-config-db"
        ).build()

        // 加载数据
        serviceScope.launch(Dispatchers.Default) {
            loadButtonConfigs()
        }
    }

    private suspend fun loadButtonConfigs() {
        val configs = db.buttonConfigDao().getAll()
        if (configs.isEmpty()) {
            // 初始化默认配置
            buttonConfigs = mutableListOf()
            // 这里需要根据实际情况初始化默认配置
        } else {
            buttonConfigs = configs.toMutableList()
        }
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        Log.i(TAG, "onKeyEvent: $event")
        event?.let {
            if (it.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                handleKeyDown()
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun handleKeyDown() {
        if (::buttonConfigs.isInitialized && buttonConfigs.isNotEmpty()) {
            playSound(buttonConfigs[0].soundPath)
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun playSound(soundPath: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                // 请求音频焦点
                val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    audioManager.requestAudioFocus(audioFocusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(
                        {},
                        AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                    )
                }

                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    setDataSource(soundPath)
                    prepare()
                    start()

                    // 设置播放完成监听器，播放结束后释放音频焦点
                    setOnCompletionListener {
                        releaseAudioFocus()
                        it.release()
                        mediaPlayer = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                releaseAudioFocus()
            }
        }
    }

    private fun stopPlaying() {
        releaseAudioFocus()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaying()
        serviceScope.cancel()
    }
}

