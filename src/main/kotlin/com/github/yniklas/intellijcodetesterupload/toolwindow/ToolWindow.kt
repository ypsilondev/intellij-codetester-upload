package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.api.ApiInteraction
import com.github.yniklas.intellijcodetesterupload.api.Network
import com.github.yniklas.intellijcodetesterupload.data.ClassResult
import com.github.yniklas.intellijcodetesterupload.data.TestResult
import com.github.yniklas.intellijcodetesterupload.data.TestResultMessage
import com.google.gson.JsonObject
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.*
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane

import kotlin.collections.ArrayList
import kotlin.collections.HashMap

import java.awt.Color
import java.awt.Container
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.BoxLayout
import javax.swing.ScrollPaneConstants

class ToolWindow(project: Project) {

    private val toolWindowPane: JComponent = JBPanel<JBPanelWithEmptyText>()
    private val contentPane: Container = JPanel()
    var scrollPane = JBScrollPane()

    private val chooseTask = "Choose Task"

    private val taskSelection = ComboBox(ArrayList<String>().toArray())
    private val project: Project
    private var tasks = HashMap<String, Int>()
    private val testBt = JButton("Login")

    init {
        taskSelection.addItem(chooseTask)

        this.project = project

        toolWindowPane.layout = BoxLayout(toolWindowPane, BoxLayout.PAGE_AXIS)

        if (Network.isLoggedIn()) {
            fillTasks()
        }

        // Initialize the Upload Button
        testBt.addActionListener { Thread { testCode() }.start() }

        contentPane.add(taskSelection)
        contentPane.add(testBt)
        toolWindowPane.add(contentPane)

        toolWindowPane.revalidate()
    }

    private fun fillTasks() {
        tasks = ApiInteraction.queryTasks(project)
        for (task in tasks) {
            taskSelection.addItem(task.key)
        }
    }

    private fun testCode() {
        if (!ToolwindoFactory.checkLoggedIn(project)) {
            return
        }

        if (tasks.size == 0) {
            fillTasks()
            testBt.text = "Test code"
            toolWindowPane.revalidate()
            return
        }

        if (taskSelection.selectedItem == chooseTask) {
            ApplicationManager.getApplication().invokeAndWait {
                Messages.showErrorDialog("You have to select a task first", "Select a Task First")
            }
            return
        }

        // Disappear previous results
        toolWindowPane.remove(scrollPane)
        testBt.isEnabled = false

        val results = ApiInteraction.testCode(project, tasks[taskSelection.selectedItem])
        if (results != null) {
            showResults(results)
        }
        testBt.isEnabled = true
    }

    fun getContent(): JComponent {
        return toolWindowPane
    }

    private fun showResults(results: JsonObject) {
        if (!results.has("fileResults")) {
            ApplicationManager.getApplication().invokeAndWait {
                Messages.showErrorDialog("Compilation error by 'CodeTester'", "Error")
            }
            return
        }

        val testResultsRaw = results.get("fileResults").asJsonObject
        val classResults = ArrayList<ClassResult>()

        for (className in testResultsRaw.keySet()) {
            val testResults = ArrayList<TestResult>()

            for (check in testResultsRaw.get(className).asJsonArray) {
                val checkName = check.asJsonObject.get("check").asString
                val checkResult = check.asJsonObject.get("result").asString
                val message = check.asJsonObject.get("message").asString
                val output = check.asJsonObject.get("output").asJsonArray
                val errorOutput = check.asJsonObject.get("errorOutput").asString

                val messages = ArrayList<TestResultMessage>()
                for (msgRaw in output) {
                    val msg = msgRaw.asJsonObject
                    messages.add(TestResultMessage(msg.get("type").asString, msg.get("content").asString))
                }

                testResults.add(TestResult(checkName, checkResult, message,
                    messages.toArray(arrayOf<TestResultMessage>()),
                    errorOutput))
            }

            classResults.add(ClassResult(className, testResults.toArray(arrayOf<TestResult>())))
        }

        // Update UI
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)

        ApplicationManager.getApplication().invokeAndWait {

            var toolWindow: ToolWindow? = null

            ToolWindowManager.getInstance(project).getToolWindow("CodeTester test details")?.remove()

            toolWindowPane.background = Color.GREEN

            for (classResult in classResults) {
                Arrays.sort(classResult.results)
                for ((i, result) in classResult.results.withIndex()) {
                    val pane = TestResultPane(result, i)

                    if (result.result == "FAILED") {
                        toolWindowPane.background = Color.RED

                        if (toolWindow == null) {
                            toolWindow = getToolWindow()
                        }

                        val cw = getConsoleWindow(toolWindow, result.title)
                        var type: ConsoleViewContentType = ConsoleViewContentType.NORMAL_OUTPUT
                        for (trm in result.output) {
                            when (trm.type) {
                                "OTHER" -> type = ConsoleViewContentType.LOG_INFO_OUTPUT
                                "INPUT" -> type = ConsoleViewContentType.USER_INPUT
                                "OUTPUT" -> type = ConsoleViewContentType.NORMAL_OUTPUT
                                "ERROR" -> type = ConsoleViewContentType.ERROR_OUTPUT
                            }
                            cw.print(trm.content + "\n", type)
                        }
                    }

                    panel.add(pane)
                }
            }

            scrollPane = JBScrollPane(panel,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)

            toolWindowPane.add(scrollPane)
            toolWindowPane.revalidate()
        }
    }

    private fun getToolWindow() : ToolWindow {
        return ToolWindowManager.getInstance(project).getToolWindow("CodeTester test details")
            ?: ToolWindowManager.getInstance(project)
                .registerToolWindow(
                    RegisterToolWindowTask.closable(
                        "CodeTester test details",
                        AllIcons.Actions.QuickfixBulb, ToolWindowAnchor.BOTTOM
                    )
                )
    }

    private fun getConsoleWindow(toolWindow: ToolWindow, name: String): ConsoleView {
        val consoleViewBuilder = TextConsoleBuilderFactory.getInstance().createBuilder(project)
        val consoleView = consoleViewBuilder.console
        val content = toolWindow.contentManager.factory.createContent(consoleView.component, name, false)
        toolWindow.contentManager.addContent(content)

        return consoleView
    }
}