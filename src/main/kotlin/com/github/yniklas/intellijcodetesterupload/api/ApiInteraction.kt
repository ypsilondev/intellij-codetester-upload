package com.github.yniklas.intellijcodetesterupload.api

import com.github.yniklas.intellijcodetesterupload.settings.CodeTesterSetting
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileOutputStream
import java.net.SocketTimeoutException
import java.util.*
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ApiInteraction {
    companion object {
        private const val BYTE_ARRAY_SIZE = 4092

        fun queryTasks(project: Project): HashMap<String, Int> {
            val tasks = HashMap<String, Int>()
            val responseTasks = Network.queryTasks(project)

            for (jsonElement in responseTasks) {
                val taskName = jsonElement.asJsonObject.get("name").asString
                val taskId = jsonElement.asJsonObject.get("id").asInt

                // Show only tasks from this and last year
                if (parseDates(taskName)) {
                    tasks[taskName] = taskId
                }
            }

            return tasks
        }

        private fun parseDates(dateString: String): Boolean {
            return dateString.contains(Calendar.getInstance().get(Calendar.YEAR).toString())
                    || dateString.contains((Calendar.getInstance().get(Calendar.YEAR) - 1).toString())
        }

        fun testCode(project: Project, task: Int?): JsonObject? {
            try {
                val filePath = getZipStream(project)
                return if (filePath != null) {
                    val responseBody = Network.uploadCode(project, filePath, task)
                    if (responseBody != null) {
                        JsonParser.parseString(responseBody.string()).asJsonObject
                    } else {
                        ApplicationManager.getApplication().invokeAndWait {
                            Messages.showErrorDialog("Empty response by 'CodeTester'", "Error")
                        }
                        null
                    }
                } else {
                    null
                }
            } catch (e: SocketTimeoutException) {
                ApplicationManager.getApplication().invokeAndWait {
                    Messages.showErrorDialog("Timeout by 'CodeTester'", "Error")
                }
                return null
            }
        }

        private fun getZipStream(project: Project): File? {
            if (CodeTesterSetting.getInstance(project).saveBeforeTesting) {
                ApplicationManager.getApplication().invokeAndWait {
                    FileDocumentManager.getInstance().saveAllDocuments()
                }
            }

            val selectedFiles = FileEditorManager.getInstance(project).selectedFiles

            if (selectedFiles.isEmpty()) {
                ApplicationManager.getApplication().invokeAndWait {
                    Messages.showErrorDialog("Choose a project by opening a source file", "Error No Module")
                }
                return null
            }

            val currentFile = selectedFiles[0]

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
    }
}