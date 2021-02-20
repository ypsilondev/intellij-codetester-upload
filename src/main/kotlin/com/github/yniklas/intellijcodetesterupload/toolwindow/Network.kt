package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import okhttp3.*
import java.util.*

class Network {
    companion object {
        private val cAttr = CredentialAttributes("codetester")
        private const val acUrl = "https://codetester.ialistannen.de/login/get-access-token"
        private const val urlGetAll = "https://codetester.ialistannen.de/check-category/get-all"
        private var tasks: JsonArray? = null

        fun getRToken(project: Project): String? {
            var rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()

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
            val request = Request.Builder().url(acUrl).method("POST", body).build()

            val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
            val authenticationObject = JsonParser.parseString(res.body?.string()).asJsonObject

            if (!authenticationObject.has("token")) {
                PasswordSafe.instance.set(cAttr, null)
                getRToken(project)
            }

            return authenticationObject.get("token")?.asString
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


        fun queryTasks(project: Project): JsonArray {
            if (tasks != null) {
                return tasks as JsonArray
            }

            val bearer = getRToken(project)

            if (bearer != null) {
                val request = Request.Builder().url(urlGetAll)
                    .header("Authorization", "Bearer $bearer").get().build()

                val response: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
                return JsonParser.parseString(response.body?.string()).asJsonArray
            } else {
                return JsonArray()
            }
        }

        fun parseDates(dateString: String): Boolean {
            return dateString.contains(Calendar.getInstance().get(Calendar.YEAR).toString())
                    || dateString.contains((Calendar.getInstance().get(Calendar.YEAR) - 1).toString())
        }
    }
}