package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.ClassResult
import com.github.yniklas.intellijcodetesterupload.data.TestResult
import com.github.yniklas.intellijcodetesterupload.data.TestResultMessage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.AbstractConsoleRunnerWithHistory
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.components.JBScrollPane
import com.intellij.usages.UsageViewManager
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import okhttp3.RequestBody

import okhttp3.MultipartBody

import okhttp3.OkHttpClient
import org.intellij.lang.annotations.JdkConstants
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*


class ToolWindow(project: Project, toolWindow: ToolWindow) {
    private val myToolWindowContent: JPanel = JPanel()

    private val url = "https://codetester.ialistannen.de/login/get-access-token"
    private val urlGetAll = "https://codetester.ialistannen.de/check-category/get-all"
    private val uploadUrl = "https://codetester.ialistannen.de/test/zip/"
    private val chooseTask = "Choose task"

    private val taskSelection = JComboBox(ArrayList<String>().toArray())
    private val cAttr = CredentialAttributes("codetester")
    private val rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()
    private val project: Project
    private val tasks = HashMap<String, Int>()
    var scrollPane = JScrollPane()

    init {
        taskSelection.addItem(chooseTask)

        this.project = project

        queryTasks()

        // Initialize the Upload Button
        val testCode = JButton("Test code")
        testCode.addActionListener { testCode() }

        myToolWindowContent.add(taskSelection)
        myToolWindowContent.add(testCode)
        myToolWindowContent.revalidate()
    }

    private fun removeFile(file: File) {
        file.delete()
    }

    private fun getZipStream(): File? {
        val currentFile = FileEditorManager.getInstance(project).selectedFiles[0]

        val currentModule = ModuleUtil.findModuleForFile(currentFile, project)

        for (sourceRoot in currentModule?.rootManager?.sourceRoots!!) {
            sourceRoot.refresh(false, true)

            val name = currentModule.name

            val file = File(project.basePath, "$name.zip")
            if (!file.exists()) {
                file.createNewFile()
            }
            val zos = ZipOutputStream(FileOutputStream(file))
            zos.setLevel(Deflater.DEFAULT_COMPRESSION)

            val data = getSourceContent("", zos, sourceRoot)

            zos.flush()
            zos.close()

            return file
        }
        return null
    }

    private fun getSourceContent(path: String, zos: ZipOutputStream, vf: VirtualFile): List<VirtualFile> {
        val files = LinkedList<VirtualFile>()

        for (child in vf.children) {
            if (child.isDirectory) {
                files.addAll(getSourceContent(child.name + "/", zos, child))
            } else {
                files.add(child)
                zos.putNextEntry(ZipEntry(path + child.name))
                val fis = child.inputStream

                val buffer = ByteArray(4092)
                var byteCount = 0
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

        val bearer = getRToken()

        if (bearer != null) {
            val path = getZipStream()

            if (path != null) {
                val client = OkHttpClient().newBuilder()
                    .build()

                val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", "",
                        RequestBody.create(
                            "application/octet-stream".toMediaTypeOrNull(),
                            path
                        )
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

                val testResults = JsonParser().parse(response.body?.string()).asJsonObject

                // Delete the created zip file
                removeFile(path)

                // Show the results in the JPanel
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
            val responseTasks = JsonParser().parse(response.body?.string()).asJsonArray

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
            Messages.showErrorDialog("You have to sign in before using the 'CodeTester' bridge", "Error")
            return null
        }

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("refreshToken", rToken).build()
        val request = Request.Builder().url(url).method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
        val authenticationObject = JsonParser().parse(res.body?.string()).asJsonObject

        if (!authenticationObject.has("token")) {
            return null
        }

        return authenticationObject.get("token").asString
    }

    fun getContent(): JPanel {
        return myToolWindowContent
    }

    private fun parseDates(dateString: String): Boolean {
        return dateString.contains((1900 + Date().year).toString())
                || dateString.contains((1899 + Date().year).toString())
    }

    private fun showResults(results: JsonObject) {
        // Disappear previous results
        myToolWindowContent.remove(scrollPane)

        val panel = JPanel()
        panel.layout = GridLayout(0, 1)

        scrollPane = JScrollPane(panel)


        val asJsonObject = results.get("fileResults").asJsonObject
        for (className in asJsonObject.keySet()) {

            val data = ArrayList<Array<String>>()

            for (check in asJsonObject.get(className).asJsonArray) {
                var checkName = check.asJsonObject.get("check").asString
                var checkResult = check.asJsonObject.get("result").asString

                val d = ArrayList<String>()
                d.add(checkName)
                d.add(checkResult)
                data.add(d.toArray() as Array<String>)

                val checkPanel = JPanel()
                checkPanel.layout = GridLayout(0, 2)

                checkPanel.add(JLabel(checkName))
                checkPanel.add(JLabel(checkResult))

                panel.add(checkPanel)
            }
            val toArray = data.toArray()
            val arr = ArrayList<String>()
            arr.add("Name")
            arr.add("Result")

            val tab = JTable(toArray as Array<Array<String>>, arr.toArray())
            tab.setBounds(30, 40, 200, 300)
            scrollPane.add(tab)
        }

        // Update UI
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.PAGE_AXIS)

        for (classResult in classResults) {
            var i = 0
            Arrays.sort(classResult.results)
            for (result in classResult.results) {
                val pane = TestResultPane(result, i++)
                panel.add(pane)
            }
        }

        scrollPane = JBScrollPane(panel)

        myToolWindowContent.add(scrollPane)
        myToolWindowContent.revalidate()
    }
}