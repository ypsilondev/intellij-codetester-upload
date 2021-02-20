package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ToolwindoFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val cf = ContentFactory.SERVICE.getInstance()

        val tw = ToolWindow(project, toolWindow)
        val content = cf.createContent(tw.getContent(), "Check Code", false)

        toolWindow.contentManager.addContent(content)

        val tw2 = AllTestToolWindow(project)
        val content2 = cf.createContent(tw2.getContentPane(), "All Tests", false)

        toolWindow.contentManager.addContent(content2)
    }
}