package top.xlxs.diyhorn

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private val ids = listOf(R.id.button1, R.id.button2, R.id.button3, R.id.button4)
    private val defaultColors = listOf("#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4")
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var buttonConfigs: MutableList<ButtonConfig>
    private lateinit var db: AppDatabase
    private val TAG = "MainActivity"
    private lateinit var indexInput: EditText // 新增输入框引用

    var dlDir: String = ""
    var audioDir: String = ""

    private fun checkAccessibilityServiceEnabled() {
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (enabledServicesSetting == null ||!enabledServicesSetting.contains(packageName)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        indexInput = findViewById(R.id.indexInput) // 初始化输入框
        findViewById<View>(R.id.powerBy).setOnClickListener {
            checkAccessibilityServiceEnabled()
        }
        initData()
    }

    private fun initData() {
        // 检查并复制 assets 中的文件
        initAssetsFiles()

        // 初始化数据库
        db = Room.databaseBuilder(
            applicationContext, AppDatabase::class.java, "button-config-db"
        ).build()

        // 加载数据
        lifecycleScope.launch(Dispatchers.Default) {
            loadButtonConfigs()
            // 打印 buttonConfigs
            Log.e(TAG, "onCreate: buttonConfigs : $buttonConfigs")
            // 设置按钮
            withContext(Dispatchers.Main) {
                ids.forEachIndexed { index, id ->
                    val textView = findViewById<TextView>(id)
                    textView.text = buttonConfigs[index].text
                    textView.setOnClickListener { playSound(buttonConfigs[index].soundPath) }
                    textView.setOnLongClickListener {
                        showEditDialog(index, textView)
                        true
                    }
                    applyAnimation(textView)

                    val drawable = textView.background as? GradientDrawable
                    drawable?.setColor(buttonConfigs[index].color)
                }
            }
        }
    }

    // 初始化 assets 中的文件
    private fun initAssetsFiles() {
        lifecycleScope.launch(Dispatchers.IO) {
            val assets = assets
            val soundFiles = assets.list("sounds")
            Log.i(TAG, "initAssetsFiles: ${soundFiles.contentToString()}")
            dlDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath
            audioDir = dlDir + File.separator + "sounds"
            // 创建 sounds 文件夹
            if (!File(audioDir).exists()) {
                File(audioDir).mkdirs()
            }
            // 复制 assets 中的文件到 sounds 文件夹
            soundFiles?.forEach { file ->
                val inputStream: InputStream = assets.open("sounds/$file")
                val outputFile = File(audioDir, file)
                if (!outputFile.exists()) {
                    val outputStream = FileOutputStream(outputFile)
                    val buffer = ByteArray(1024)
                    var length: Int
                    while (inputStream.read(buffer).also { length = it } > 0) {
                        outputStream.write(buffer, 0, length)
                    }
                    outputStream.flush()
                    outputStream.close()
                    inputStream.close()
                }
            }
        }
    }

    private fun loadButtonConfigs() {
        val configs = db.buttonConfigDao().getAll()
        if (configs.isEmpty()) {
            // 初始化默认配置
            buttonConfigs = mutableListOf()
            buttonConfigs.add(
                ButtonConfig(
                    id = 0,
                    text = "你干嘛~哎呦",
                    soundPath = audioDir + File.separator + "你干嘛~哎呦.mp3",
                    color = Color.parseColor(defaultColors[0])
                )
            )
            buttonConfigs.add(
                ButtonConfig(
                    id = 1,
                    text = "大小姐驾到",
                    soundPath = audioDir + File.separator + "大小姐驾到.mp3",
                    color = Color.parseColor(defaultColors[1])
                )
            )
            buttonConfigs.add(
                ButtonConfig(
                    id = 2,
                    text = "我嘞个豆",
                    soundPath = audioDir + File.separator + "我嘞个豆.mp3",
                    color = Color.parseColor(defaultColors[2])
                )
            )
            buttonConfigs.add(
                ButtonConfig(
                    id = 3,
                    text = "狗叫",
                    soundPath = audioDir + File.separator + "狗叫.mp3",
                    color = Color.parseColor(defaultColors[3])
                )
            )
            db.buttonConfigDao().insertAll(*buttonConfigs.toTypedArray())
        } else {
            buttonConfigs = configs.toMutableList()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyAnimation(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val scaleAnim = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f)
                    val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f)
                    val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f)
                    android.animation.AnimatorSet().apply {
                        playTogether(scaleAnim, scaleYAnim, alphaAnim)
                        duration = 200
                        start()
                    }
                }

                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    val scaleAnim = ObjectAnimator.ofFloat(view, "scaleX", 0.9f, 1f)
                    val scaleYAnim = ObjectAnimator.ofFloat(view, "scaleY", 0.9f, 1f)
                    val alphaAnim = ObjectAnimator.ofFloat(view, "alpha", 0.7f, 1f)
                    android.animation.AnimatorSet().apply {
                        playTogether(scaleAnim, scaleYAnim, alphaAnim)
                        duration = 200
                        start()
                    }
                }
            }
            false
        }
    }

    private fun playSound(soundPath: String) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                Log.i(TAG, "playSound: $soundPath")
                setDataSource(soundPath)
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showEditDialog(index: Int, button: TextView) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_button, null)
        val dialogEt = dialogView.findViewById<EditText>(R.id.editTextText)
        dialogEt!!.setText(buttonConfigs[index].text)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("编辑按钮")
            .setPositiveButton("保存") { _, _ ->
                val newText = dialogEt.text.toString().take(8)
                val newConfig = buttonConfigs[index].copy(text = newText)
                buttonConfigs[index] = newConfig
                button.text = newText
                lifecycleScope.launch(Dispatchers.Default) {
                    db.buttonConfigDao().insertAll(newConfig)
                }
            }
            .setNeutralButton("选择音频") { _, _ ->
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, index)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val uri = data.data
            uri?.let {
                lifecycleScope.launch(Dispatchers.Default) {
                    val filePath = saveAudioFile(it)
                    buttonConfigs[requestCode] = buttonConfigs[requestCode]
                        .copy(text = File(filePath).name, soundPath = filePath)
                }
            }
        }
    }

    private suspend fun saveAudioFile(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            val inputStream: InputStream = contentResolver.openInputStream(uri)!!
            val appDir = File(filesDir, "sounds")
            if (!appDir.exists()) appDir.mkdirs()
            // 获取 contentUri 最后的文件名称
            val fileName = uri.lastPathSegment?.substringBeforeLast(".") ?: ""
            val outputFile = File(appDir, fileName)
            val outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            outputFile.absolutePath
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 这里以按下任意键为例，你可以根据需要修改 keyCode 来监听特定按键
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            // 从输入框获取索引
            val input = indexInput.text.toString()
            var index = 0
            try {
                index = input.toInt()
                if (index < 0 || index > 3) {
                    index = 0
                }
            } catch (e: NumberFormatException) {
                index = 0
            }

            // 检查 buttonConfigs 是否为空
            if (buttonConfigs.isNotEmpty() && index < buttonConfigs.size) {
                playSound(buttonConfigs[index].soundPath)
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

// Room 数据库实体类
@Entity
data class ButtonConfig(
    @PrimaryKey val id: Int,
    val text: String,
    val soundPath: String,
    @ColorInt val color: Int
)