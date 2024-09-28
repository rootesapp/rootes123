package com.root.system

import com.root.system.PartitionAdapter

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.BufferedReader
import java.io.InputStreamReader

class FragmentHome : Fragment() {

    private lateinit var partitionList: RecyclerView
    private lateinit var searchBox: EditText
    private lateinit var partitions: MutableList<String>
    private var rootCommand = "su" // 默认的 root 命令

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // 初始化RecyclerView和搜索框
        partitionList = view.findViewById(R.id.partition_list)
        searchBox = view.findViewById(R.id.search_box)
        val flashAllBtn: FloatingActionButton = view.findViewById(R.id.flash_all_button)

        // 获取分区列表，使用root权限从 /dev/block/by-name 目录获取分区信息
        partitions = getPartitionsFromDev()

        // 设置RecyclerView
        partitionList.layoutManager = LinearLayoutManager(requireContext())
        partitionList.adapter = PartitionAdapter(partitions) { partition ->
            showPartitionDialog(partition)
        }

        // 搜索框监听
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterPartitions(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // 批量刷入按钮
        flashAllBtn.setOnClickListener {
            flashAllPartitions()
        }

        return view
    }

    // 使用 root 权限从 /dev/block/by-name 获取分区
    private fun getPartitionsFromDev(): MutableList<String> {
        val partitions = mutableListOf<String>()
        val command = "$rootCommand -c 'ls /dev/block/by-name'"

        try {
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                partitions.add(line!!)
            }

            process.waitFor()

            if (process.exitValue() != 0) {
                showRootCommandDialog()
            }
        } catch (e: Exception) {
            showRootCommandDialog()
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
        AlertDialog.Builder(requireContext())
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
        val command = "$rootCommand -c 'dd if=/dev/block/by-name/$partition of=$outputPath'"

        executeRootCommand(command) {
            Toast.makeText(requireContext(), "提取完成: $outputPath", Toast.LENGTH_SHORT).show()
        }
    }

    // 刷入分区的功能 (使用dd命令)
    private fun flashPartition(partition: String) {
        val inputPath = "/sdcard/${partition}_image.img"
        val command = "$rootCommand -c 'dd if=$inputPath of=/dev/block/by-name/$partition'"

        executeRootCommand(command) {
            Toast.makeText(requireContext(), "$partition 刷入完成", Toast.LENGTH_SHORT).show()
        }
    }

    // 批量刷入所有分区
    private fun flashAllPartitions() {
        AlertDialog.Builder(requireContext())
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
            val process = Runtime.getRuntime().exec(command)
            process.waitFor()

            if (process.exitValue() == 0) {
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
        val input = EditText(requireContext()).apply {
            hint = "请输入Root授权命令"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Root授权命令无效")
            .setMessage("当前无法使用默认的'su'命令，请输入正确的Root授权命令：")
            .setView(input)
            .setPositiveButton("确定") { _, _ ->
                rootCommand = input.text.toString()
                Toast.makeText(requireContext(), "Root授权命令已更新", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("退出软件") { _, _ ->
                activity?.finish() // 退出应用程序
            }
            .show()
    }
}
