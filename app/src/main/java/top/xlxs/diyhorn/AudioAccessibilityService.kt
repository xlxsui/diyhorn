package top.xlxs.diyhorn

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
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

class AudioAccessibilityService : AccessibilityService() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var buttonConfigs: MutableList<ButtonConfig>
    private lateinit var db: AppDatabase
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val TAG = "AudioAccessibilityService"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("AudioAccessibilityService", "无障碍服务已连接");

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
            // 👇关键：允许监听按键
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 此方法用于处理无障碍事件，按键事件处理在 onKeyEvent 中
    }

    override fun onInterrupt() {
        // 服务中断时调用
    }

    override fun onCreate() {
        super.onCreate()
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
            if (it.action == KeyEvent.ACTION_DOWN) {
                handleKeyDown(it.keyCode)
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    private fun handleKeyDown(keyCode: Int) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (::buttonConfigs.isInitialized && buttonConfigs.isNotEmpty()) {
                playSound(buttonConfigs[0].soundPath)
            }
        }
    }

    private fun playSound(soundPath: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(soundPath)
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        serviceScope.cancel()
    }
}
