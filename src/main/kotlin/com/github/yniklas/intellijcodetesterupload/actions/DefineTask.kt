package com.github.yniklas.intellijcodetesterupload.actions

import com.google.gson.JsonParser
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import okhttp3.*

class DefineTask : AnAction() {
    private val url = "https://codetester.ialistannen.de/login/get-access-token"
    private val urlGetAll = "https://codetester.ialistannen.de/check-category/get-all"

    override fun actionPerformed(e: AnActionEvent) {
        val cAttr = CredentialAttributes("codetester")
        val rToken = PasswordSafe.instance.get(cAttr)?.getPasswordAsString()

        if (rToken == null) {
            Messages.showErrorDialog("You have to sign in first", "Error")
            return
        }

        val body: RequestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("refreshToken", rToken).build()
        val request = Request.Builder().url(url).method("POST", body).build()

        val res: Response = OkHttpClient().newBuilder().build().newCall(request).execute()

        val parser = JsonParser()
        val resAsJson = parser.parse(res.body?.string()).asJsonObject

        if (!resAsJson.has("token")) {
            return
        }

        val request2 = Request.Builder().url(url)
            .header("Authorization", "Bearer " + resAsJson.get("token").asString).get().build()

        val res2: Response = OkHttpClient().newBuilder().build().newCall(request2).execute()

        val parser2 = JsonParser()
        val resAsJson2 = parser2.parse(res2.body?.string()).asJsonObject
    }
}