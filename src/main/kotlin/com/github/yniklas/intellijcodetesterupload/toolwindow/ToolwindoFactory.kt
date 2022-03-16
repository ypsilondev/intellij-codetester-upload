package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.api.Network
import com.github.yniklas.intellijcodetesterupload.settings.CodeTesterSetting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ToolwindoFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val cf = ContentFactory.SERVICE.getInstance()

        val tw = ToolWindow(project)
        val content = cf.createContent(tw.getContent(), "Check Code", false)

        toolWindow.contentManager.addContent(content)

        val tw2 = AllTestToolWindow(project)
        val content2 = cf.createContent(tw2.getContentPane(), "All Tests", false)

        toolWindow.contentManager.addContent(content2)
    }

    override fun isApplicable(project: Project): Boolean {
        return CodeTesterSetting.getInstance(project).uniProject
    }

    companion object {
        fun checkLoggedIn(project: Project): Boolean {
            if (!Network.isLoggedIn()) {
                var username: String? = null
                var password: String? = null
                ApplicationManager.getApplication().invokeAndWait {
                    username = Messages.showInputDialog(
                        project, "Input username", "Credentials",
                        Messages.getInformationIcon()
                    )
                    password = Messages.showPasswordDialog(
                        project, "Input password", "Credentials",
                        Messages.getInformationIcon()
                    )
                }

                if (username == null || password == null || !Network.validate(username!!, password!!)) {
                    ApplicationManager.getApplication().invokeAndWait {
                        Messages.showErrorDialog("Could not log into codetester", "Login Failed")
                    }
                    return false
                }
            }
            return true
        }
    }

}