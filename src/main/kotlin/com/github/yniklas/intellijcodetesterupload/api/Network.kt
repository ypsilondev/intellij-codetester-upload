package com.github.yniklas.intellijcodetesterupload.api

import com.github.yniklas.intellijcodetesterupload.data.TestCase
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class Network {
    companion object {
        private const val TIMEOUT = 30L
        private const val host = "https://codetester.ialistannen.de"
        private const val acUrl = "$host/login/get-access-token"
        private const val urlGetAll = "$host/check-category/get-all"
        private const val allTestsUrl = "$host/checks/get-all"
        private const val uploadUrl = "$host/test/zip/"

        private val cAttr = CredentialAttributes("codetester")
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

        fun getAllTests(project: Project): Array<TestCase> {
            val bearer = getRToken(project)
            val testCases = ArrayList<TestCase>()

            if (bearer != null) {
                val request = Request.Builder().url(allTestsUrl)
                    .header("Authorization", "Bearer $bearer").get().build()

                val response: Response = OkHttpClient().newBuilder().build().newCall(request).execute()
                val responseTests = JsonParser.parseString(response.body?.string()).asJsonArray

                if (responseTests != null) {
                    for (rtRaw in responseTests) {
                        val rt = rtRaw.asJsonObject
                        testCases.add(
                            TestCase(
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

        fun uploadCode(project: Project, filePath: File, task: Int?): ResponseBody? {
            val bearer = getRToken(project)

            if (bearer != null) {
                val client = OkHttpClient().newBuilder().connectTimeout(TIMEOUT, TimeUnit.SECONDS).build()

                val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", "",
                        filePath
                            .asRequestBody("application/octet-stream".toMediaTypeOrNull())
                    )
                    .build()
                val request: Request = Request.Builder()
                    .url(uploadUrl + task)
                    .method("POST", body)
                    .addHeader(
                        "Authorization",
                        "Bearer $bearer"
                    )
                    .build()

                val response = client.newCall(request).execute()
                return response.body
            } else {
                throw LoginException("User can not login in")
            }
        }
    }
}