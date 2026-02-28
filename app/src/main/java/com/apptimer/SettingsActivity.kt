package com.apptimer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // 未来可以添加更多设置选项
        // 例如：通知设置、数据清理、导出历史记录等
    }
}
