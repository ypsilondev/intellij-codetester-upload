package com.github.yniklas.intellijcodetesterupload.actions

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.icons.AllIcons
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Pass
import com.intellij.remoteServer.util.CloudConfigurationUtil
import okhttp3.*
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class Credentials : AnAction() {
    private val httpClient: HttpClient = HttpClient.newHttpClient()
    private val url = "https://codetester.ialistannen.de/login"

    override fun actionPerformed(e: AnActionEvent) {
        val username = Messages.showInputDialog(e.project, "Input username", "Credentials",
         Messages.getInformationIcon())
        val password = Messages.showPasswordDialog(e.project, "Input password", "Credentials",
         Messages.getInformationIcon())

        if (username == null || password == null) {
            return
        }

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("username", username)
            .addFormDataPart("password", password).build()
        val request = Request.Builder().url(url).method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()

        val parser = JsonParser()
        val resAsJson = parser.parse(res.body?.string()).asJsonObject

        if (resAsJson.has("error")) {
            Messages.showErrorDialog(resAsJson.get("error").asString, "Error")
        } else if (resAsJson.has("token")) {
            val cAttr = CredentialAttributes("codetester")
            val creds = Credentials("token",resAsJson.get("token").asString)

            PasswordSafe.instance.set(cAttr, creds)
        }
    }

    fun HttpRequest.Builder.postMultipartFormData(boundary: String, data: Map<String, Any>): HttpRequest.Builder {
        val byteArrays = ArrayList<ByteArray>()
        val separator = "--$boundary\r\nContent-Disposition: form-data; name=".toByteArray(StandardCharsets.UTF_8)

        for (entry in data.entries) {
            byteArrays.add(separator)
            when(entry.value) {
                is File -> {
                    val file = entry.value as File
                    val path = Path.of(file.toURI())
                    val mimeType = Files.probeContentType(path)
                    byteArrays.add("\"${entry.key}\"; filename=\"${path.fileName}\"\r\nContent-Type: $mimeType\r\n\r\n".toByteArray(StandardCharsets.UTF_8))
                    byteArrays.add(Files.readAllBytes(path))
                    byteArrays.add("\r\n".toByteArray(StandardCharsets.UTF_8))
                }
                else -> byteArrays.add("\"${entry.key}\"\r\n\r\n${entry.value}\r\n".toByteArray(StandardCharsets.UTF_8))
            }
        }
        byteArrays.add("--$boundary--".toByteArray(StandardCharsets.UTF_8))

        this.header("Content-Type", "multipart/form-data;boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArrays(byteArrays))
        return this
    }
}