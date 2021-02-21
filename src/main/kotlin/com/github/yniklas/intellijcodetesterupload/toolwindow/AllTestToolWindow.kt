package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.TestCase
import com.google.gson.JsonParser
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.containers.stream
import okhttp3.*
import java.util.*
import java.util.stream.Collectors
import javax.swing.*
import kotlin.collections.ArrayList

class AllTestToolWindow(project: Project) {
    private val project = project
    private val contentPane: JComponent = JPanel()
    private var scrollPane = JBScrollPane()
    private var header = JLabel()

    private val taskSelection = ComboBox(ArrayList<String>().toArray())
    private val tasks = HashMap<String, Int>()
    private val queryAllTests = JButton("List tests")

    private val allTestsUrl = "https://codetester.ialistannen.de/checks/get-all"

    init {
        contentPane.layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        val subPanel = JPanel()
        subPanel.add(JLabel("List all Tests after choosing the Task"))

        taskSelection.addItem("Choose Task")
        queryTasks()
        subPanel.add(taskSelection)
        contentPane.add(subPanel)

        queryAllTests.addActionListener { showTests() }
        subPanel.add(queryAllTests)
        contentPane.revalidate()
    }

    private fun showTests() {
        if (taskSelection.selectedItem == "Choose Task") {
            return
        }

        contentPane.remove(scrollPane)
        contentPane.remove(header)
        queryAllTests.isEnabled = false
        var allTests = getAllTests()
        queryAllTests.isEnabled = true

        allTests = allTests.stream()
            .filter { t -> t.catId == tasks[taskSelection.selectedItem] }
            .sorted(Comparator.comparingLong { t -> -t.creationTime } ).collect(Collectors.toList()).toTypedArray()

        header = JLabel("All tests (${allTests.size})")
        contentPane.add(header)

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)
        for (test in allTests) {
            panel.add(TestCasePane(test))
        }

        scrollPane = JBScrollPane(panel)
        contentPane.add(scrollPane)
        contentPane.revalidate()
    }

    private fun queryTasks() {
        val responseTasks = Network.queryTasks(project)

        for (jsonElement in responseTasks) {
            val taskName = jsonElement.asJsonObject.get("name").asString
            val taskId = jsonElement.asJsonObject.get("id").asInt

            // Show only tasks from this and last year
            if (Network.parseDates(taskName)) {
                taskSelection.addItem(taskName)
                tasks[taskName] = taskId
            }
        }

    }

    private fun getAllTests(): Array<TestCase> {
        val bearer = Network.getRToken(project)
        val testCases = ArrayList<TestCase>()

        if (bearer != null) {
            val request = Request.Builder().url(allTestsUrl)
                .header("Authorization", "Bearer $bearer").get().build()

            val response: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
            val responseTests = JsonParser.parseString(response.body?.string()).asJsonArray

            if (responseTests != null) {
                for (rtRaw in responseTests) {
                    val rt = rtRaw.asJsonObject
                    testCases.add(TestCase(
                        rt.get("id").asInt,
                        rt.get("name").asString,
                        rt.get("creator").asString,
                        rt.get("checkType").asString,
                        rt.get("approved").asBoolean,
                        rt.get("creationTime").asLong,
                        rt.get("category").asJsonObject.get("id").asInt,
                        rt.get("category").asJsonObject.get("name").asString)
                    )
                }
            }
        }
        return testCases.toArray(arrayOf<TestCase>())
    }

    fun getContentPane(): JComponent {
        return contentPane
    }
}