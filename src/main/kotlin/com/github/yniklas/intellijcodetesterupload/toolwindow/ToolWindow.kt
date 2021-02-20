package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.ClassResult
import com.github.yniklas.intellijcodetesterupload.data.TestResult
import com.github.yniklas.intellijcodetesterupload.data.TestResultMessage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.*
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import okhttp3.RequestBody

import okhttp3.MultipartBody

import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import java.awt.Color
import java.awt.Container
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.BoxLayout
import javax.swing.ScrollPaneConstants

class ToolWindow(project: Project, toolWindow: ToolWindow) {

    companion object {
        private const val TIMEOUT = 30L
        private const val BYTE_ARRAY_SIZE = 4092
    }

    private val toolWindowPane: JComponent = JPanel()
    private val contentPane: Container = JPanel()
    var scrollPane = JBScrollPane()

    private val url = "https://codetester.ialistannen.de/login/get-access-token"
    private val urlGetAll = "https://codetester.ialistannen.de/check-category/get-all"
    private val uploadUrl = "https://codetester.ialistannen.de/test/zip/"
    private val chooseTask = "Choose task"

    private val taskSelection = ComboBox(ArrayList<String>().toArray())
    private val cAttr = CredentialAttributes("codetester")
    private var rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()
    private val project: Project
    private val tasks = HashMap<String, Int>()
    private val testBt = JButton("Test code")

    init {
        taskSelection.addItem(chooseTask)

        this.project = project

        toolWindowPane.layout = BoxLayout(toolWindowPane, BoxLayout.PAGE_AXIS)

        queryTasks()

        // Initialize the Upload Button
        testBt.addActionListener { Thread { testCode() }.start() }

        toolWindowPane.background = Color.GREEN
        println(toolWindowPane.height)
        contentPane.add(taskSelection)
        contentPane.add(testBt)
        toolWindowPane.add(contentPane)

        toolWindowPane.revalidate()
    }

    private fun getZipStream(): File {
        val currentFile = FileEditorManager.getInstance(project).selectedFiles[0]

        val currentModule = ModuleUtil.findModuleForFile(currentFile, project)

        val name = currentModule?.name

        val file = File(project.basePath, "$name.zip")
        if (!file.exists()) {
            file.createNewFile()
        } else {
            file.delete()
        }
        val zos = ZipOutputStream(FileOutputStream(file))
        zos.setLevel(Deflater.BEST_COMPRESSION)

        val msr = ProjectRootManager.getInstance(project).fileIndex.getSourceRootForFile(currentFile)

        msr?.refresh(false, true)
        getSourceContent("", zos, msr!!)

        zos.flush()
        zos.close()

        return file
    }

    private fun getSourceContent(path: String, zos: ZipOutputStream, vf: VirtualFile): List<VirtualFile> {
        val files = LinkedList<VirtualFile>()

        for (child in vf.children) {
            if (child.isDirectory) {
                files.addAll(getSourceContent(path + child.name + "/", zos, child))
            } else {
                if (child.name == "Terminal.java") {
                    continue
                }

                files.add(child)
                zos.putNextEntry(ZipEntry(path + child.name))
                val fis = child.inputStream

                val buffer = ByteArray(BYTE_ARRAY_SIZE)
                var byteCount: Int
                while (fis.read(buffer).also { byteCount = it } != -1) {
                    zos.write(buffer, 0, byteCount)
                }

                fis.close()
                zos.closeEntry()
            }
        }

        return files
    }

    private fun testCode() {
        if (taskSelection.selectedItem == chooseTask) {
            return
        }

        testBt.isEnabled = false

        val bearer = getRToken()

        if (bearer != null) {
            val path = getZipStream()

            val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).build()

            val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "",
                    path
                        .asRequestBody("application/octet-stream".toMediaTypeOrNull())
                )
                .build()
            val request: Request = Request.Builder()
                .url(uploadUrl + tasks[taskSelection.selectedItem])
                .method("POST", body)
                .addHeader(
                    "Authorization",
                    "Bearer $bearer"
                )
                .build()
            val response = client.newCall(request).execute()
            val responseBody = response.body

            if (responseBody == null) {
                testBt.isEnabled = true
            } else {
                val testResults = JsonParser.parseString(responseBody.string()).asJsonObject
                showResults(testResults)
            }
        }
    }

    private fun queryTasks() {
        val bearer = getRToken()

        if (bearer != null) {
            val request = Request.Builder().url(urlGetAll)
                .header("Authorization", "Bearer $bearer").get().build()

            val response: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
            val responseTasks = JsonParser.parseString(response.body?.string()).asJsonArray

            for (jsonElement in responseTasks) {
                val taskName = jsonElement.asJsonObject.get("name").asString
                val taskId = jsonElement.asJsonObject.get("id").asInt;

                // Show only tasks from this and last year
                if (parseDates(taskName)) {
                    taskSelection.addItem(taskName)
                    tasks[taskName] = taskId
                }
            }
        }
    }

    private fun getRToken(): String? {
        if (rToken == null) {
            val username = Messages.showInputDialog(project, "Input username", "Credentials",
                Messages.getInformationIcon())
            val password = Messages.showPasswordDialog(project, "Input password", "Credentials",
                Messages.getInformationIcon())

            if (username == null || password == null || !validate(username, password)) {
                return null
            }
        }

        rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("refreshToken", rToken!!).build()
        val request = Request.Builder().url(url).method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
        val authenticationObject = JsonParser.parseString(res.body?.string()).asJsonObject

        if (!authenticationObject.has("token")) {
            return null
        }

        return authenticationObject.get("token").asString
    }

    private fun validate(username: String, password: String): Boolean {
        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("username", username)
            .addFormDataPart("password", password).build()
        val request = Request.Builder().url("https://codetester.ialistannen.de/login")
            .method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()

        val resAsJson = JsonParser.parseString(res.body?.string()).asJsonObject

        if (resAsJson.has("error")) {
            Messages.showErrorDialog(resAsJson.get("error").asString, "Error")
        } else if (resAsJson.has("token")) {
            val cAttr = CredentialAttributes("codetester")
            val creds = Credentials("token",resAsJson.get("token").asString)

            PasswordSafe.instance.set(cAttr, creds)
            return true
        }
        return false
    }

    fun getContent(): JComponent {
        return toolWindowPane
        //return myToolWindowContent
    }

    private fun parseDates(dateString: String): Boolean {
        return dateString.contains(Calendar.getInstance().get(Calendar.YEAR).toString())
                || dateString.contains((Calendar.getInstance().get(Calendar.YEAR) - 1).toString())
    }

    private fun showResults(results: JsonObject) {
        // Disappear previous results
        toolWindowPane.remove(scrollPane)

        if (!results.has("fileResults")) {
            testBt.isEnabled = true
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

            for (classResult in classResults) {
                Arrays.sort(classResult.results)
                for ((i, result) in classResult.results.withIndex()) {
                    val pane = TestResultPane(result, i)

                    if (result.result == "FAILED") {

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
        val consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(project).console
        val content = toolWindow.contentManager.factory.createContent(consoleView.component, name, false)
        toolWindow.contentManager.addContent(content)

        return consoleView
    }
}