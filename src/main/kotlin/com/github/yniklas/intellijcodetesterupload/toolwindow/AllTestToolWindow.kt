package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.api.ApiInteraction
import com.github.yniklas.intellijcodetesterupload.api.Network
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.containers.stream
import java.util.*
import java.util.stream.Collectors
import javax.swing.*

class AllTestToolWindow(private val project: Project) {
    private val contentPane: JComponent = JPanel()
    private var scrollPane = JBScrollPane()
    private var header = JLabel()

    private val taskSelection = ComboBox(ArrayList<String>().toArray())
    private var tasks = HashMap<String, Int>()
    private val queryAllTests = JButton("Login")

    init {
        contentPane.layout = BoxLayout(contentPane, BoxLayout.PAGE_AXIS)
        val subPanel = JPanel()
        subPanel.add(JLabel("List all Tests after choosing the Task"))

        taskSelection.addItem("Choose Task")

        if (Network.isLoggedIn()) {
            fillTasks()
        }

        subPanel.add(taskSelection)
        contentPane.add(subPanel)

        queryAllTests.addActionListener { showTests() }
        subPanel.add(queryAllTests)
        contentPane.revalidate()
    }

    private fun fillTasks() {
        tasks = ApiInteraction.queryTasks(project)
        for (task in tasks) {
            taskSelection.addItem(task.key)
        }
    }

    private fun showTests() {
        if (!ToolwindoFactory.checkLoggedIn(project)) {
            return
        }

        if (tasks.size == 0) {
            fillTasks()
            queryAllTests.text = "Choose Task"
            contentPane.revalidate()
            return
        }

        if (taskSelection.selectedItem == "Choose Task") {
            ApplicationManager.getApplication().invokeAndWait {
                Messages.showErrorDialog("You have to select a task first", "Select a Task First")
            }
        }

        contentPane.remove(scrollPane)
        contentPane.remove(header)
        queryAllTests.isEnabled = false
        var allTests = Network.getAllTests(project)
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

    fun getContentPane(): JComponent {
        return contentPane
    }
}