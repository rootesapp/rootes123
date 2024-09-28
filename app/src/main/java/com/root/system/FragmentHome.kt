package com.root.system

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.topjohnwu.libsu.Su
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedReader
import java.io.InputStreamReader

class FragmentHome : AppCompatActivity() {

    private lateinit var partitionList: RecyclerView
    private lateinit var searchBox: EditText
    private lateinit var partitions: MutableList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_home) // 保持使用 fragment_home.xml

        // 初始化RecyclerView和搜索框
        partitionList = findViewById(R.id.partition_list)
        searchBox = findViewById(R.id.search_box)
        val flashAllBtn: FloatingActionButton = findViewById(R.id.flash_all_button)

        // 获取分区列表
        partitions = getPartitionsFromDev()

        // 如果获取失败，显示对话框自定义root命令
        if (partitions.isEmpty()) {
            showRootCommandDialog()
        } else {
            setupUI()
        }

        // 批量刷入按钮
        flashAllBtn.setOnClickListener {
            flashAllPartitions()
        }
    }

    // 初始化 UI，包括设置 RecyclerView 和搜索框监听
    private fun setupUI() {
        partitionList.layoutManager = LinearLayoutManager(this)
        partitionList.adapter = PartitionAdapter(partitions) { partition ->
            showPartitionDialog(partition)
        }

        // 添加搜索框回车键监听
        searchBox.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                val query = searchBox.text.toString()
                filterPartitions(query)
                return@OnEditorActionListener true
            }
            false
        })

        // 搜索框文字变更监听
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // 使用 libsu 获取分区列表
    private fun getPartitionsFromDev(): MutableList<String> {
        val partitions = mutableListOf<String>()
        val command = "ls /dev/block/by-name"

        try {
            // 使用 libsu 执行命令
            val output = Su.run(command)
            partitions.addAll(output.lines().filter { it.isNotEmpty() })

            // 如果没有获取到分区，清空列表
            if (partitions.isEmpty()) {
                Toast.makeText(this, "无法获取分区", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            partitions.clear() // 捕获异常，清空 partitions
        }

        return partitions
    }

    // 过滤分区列表
    private fun filterPartitions(query: String) {
        val filteredList = partitions.filter { it.contains(query, ignoreCase = true) }
        partitionList.adapter = PartitionAdapter(filteredList) { partition ->
            showPartitionDialog(partition)
        }
    }

    // 显示分区对话框
    private fun showPartitionDialog(partition: String) {
        val options = arrayOf("提取", "刷入")
        AlertDialog.Builder(this)
            .setTitle("选择操作 - $partition")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> extractPartition(partition)
                    1 -> flashPartition(partition)
                }
            }
            .show()
    }

    // 提取分区的功能 (使用dd命令)
    private fun extractPartition(partition: String) {
        val outputPath = "/sdcard/${partition}_backup.img"
        val command = "dd if=/dev/block/by-name/$partition of=$outputPath"

        executeRootCommand(command) {
            Toast.makeText(this, "提取完成: $outputPath", Toast.LENGTH_SHORT).show()
        }
    }

    // 刷入分区的功能 (使用dd命令)
    private fun flashPartition(partition: String) {
        val inputPath = "/sdcard/${partition}_image.img"
        val command = "dd if=$inputPath of=/dev/block/by-name/$partition"

        executeRootCommand(command) {
            Toast.makeText(this, "$partition 刷入完成", Toast.LENGTH_SHORT).show()
        }
    }

    // 批量刷入所有分区
    private fun flashAllPartitions() {
        AlertDialog.Builder(this)
            .setTitle("确认批量刷入")
            .setMessage("你确定要刷入所有分区吗？这可能会导致数据丢失。")
            .setPositiveButton("确定") { _, _ ->
                partitions.forEach { flashPartition(it) }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 执行root命令的方法
    private fun executeRootCommand(command: String, onSuccess: () -> Unit) {
        try {
            // 使用 libsu 执行命令
            val process = Su.run(command)
            if (process.isNotEmpty()) {
                onSuccess()
            } else {
                showRootCommandDialog()
            }
        } catch (e: Exception) {
            showRootCommandDialog()
        }
    }

    // 弹出对话框让用户输入root授权命令，并增加退出选项
    private fun showRootCommandDialog() {
        val input = EditText(this).apply {
            hint = "请输入Root授权命令"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle("Root授权命令无效")
            .setMessage("当前无法使用默认的命令，请输入正确的Root授权命令：")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                // 不再需要自定义命令，直接获取分区列表
                partitions = getPartitionsFromDev()
                if (partitions.isNotEmpty()) {
                    setupUI()
                } else {
                    Toast.makeText(this, "获取分区失败，请检查Root授权命令", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("退出软件") { _, _ ->
                finish() // 退出应用程序
            }
            .show()
    }
}
