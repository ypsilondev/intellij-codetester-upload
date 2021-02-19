package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.Handler
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.rootManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.sun.jna.StringArray
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.print.attribute.standard.Media

import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import okhttp3.RequestBody

import okhttp3.MultipartBody

import okhttp3.OkHttpClient
import java.awt.GridLayout
import javax.swing.*


class ToolWindow(project: Project, toolWindow: ToolWindow) {
    private val myToolWindowContent: JPanel = JPanel()

    private val url = "https://codetester.ialistannen.de/login/get-access-token"
    private val urlGetAll = "https://codetester.ialistannen.de/check-category/get-all"
    private val uploadUrl = "https://codetester.ialistannen.de/test/zip/"

    private val taskSelection = JComboBox(ArrayList<String>().toArray())
    private val cAttr = CredentialAttributes("codetester")
    private val rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()
    private val project: Project
    private val tasks = HashMap<String, Int>()
    var scrollPane = JScrollPane()

    init {
        taskSelection.addItem("")

        this.project = project
        getStuff()
        myToolWindowContent.add(taskSelection)
        myToolWindowContent.revalidate()

        val testCode = JButton("Test code")
        testCode.addActionListener { testCode() }

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
        if (taskSelection.selectedItem == "") {
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
                    .url("https://codetester.ialistannen.de/test/zip/${tasks[taskSelection.selectedItem]}")
                    .method("POST", body)
                    .addHeader(
                        "Authorization",
                        "Bearer $bearer"
                    )
                    .build()
                val response = client.newCall(request).execute()

                val parser = JsonParser()
                val resAsJson = parser.parse(response.body?.string()).asJsonObject

                removeFile(path)

                showResults(resAsJson)
            }
        }
    }

    private fun getStuff() {
        val bearer = getRToken()

        if (bearer != null) {
            val request2 = Request.Builder().url(urlGetAll)
                .header("Authorization", "Bearer $bearer").get().build()

            val res2: Response = OkHttpClient().newBuilder().build().newCall(request2).execute()

            val parser2 = JsonParser()
            val resAsJson2 = parser2.parse(res2.body?.string()).asJsonArray

            for (jsonElement in resAsJson2) {
                val taskBt = JButton(jsonElement.asJsonObject.get("name").asString)
                taskBt.addActionListener { Handler.chosenTask = jsonElement.asJsonObject.get("id").asInt }

                if (parseDates(jsonElement.asJsonObject.get("name").asString)) {
                    taskSelection.addItem(jsonElement.asJsonObject.get("name").asString)
                    tasks[jsonElement.asJsonObject.get("name").asString] = jsonElement.asJsonObject.get("id").asInt
                }
            }
        }
    }

    private fun getRToken(): String? {
        if (rToken == null) {
            Messages.showErrorDialog("You have to sign in first", "Error")
            return null
        }

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("refreshToken", rToken).build()
        val request = Request.Builder().url(url).method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()

        val parser = JsonParser()
        val resAsJson = parser.parse(res.body?.string()).asJsonObject

        if (!resAsJson.has("token")) {
            return null
        }

        return resAsJson.get("token").asString
    }

    fun getContent(): JPanel {
        return myToolWindowContent
    }

    private fun parseDates(dateString: String): Boolean {
        return dateString.contains((1900 + Date().year).toString())
                || dateString.contains((1899 + Date().year).toString())
    }

    private fun showResults(results: JsonObject) {
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

        myToolWindowContent.add(scrollPane)
        myToolWindowContent.revalidate()
    }
}