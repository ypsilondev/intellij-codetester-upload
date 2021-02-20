package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.services.CodeTesterSetting
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ToolwindoFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tw = ToolWindow(project, toolWindow)
        val cf = ContentFactory.SERVICE.getInstance()
        val content = cf.createContent(tw.getContent(), "Basic Tests", false)

        toolWindow.contentManager.addContent(content)
    }

    override fun isApplicable(project: Project): Boolean {
        return CodeTesterSetting.getInstance().uniProject
    }

}