package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.Handler
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ToolwindoFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val tw = com.github.yniklas.intellijcodetesterupload.toolwindow.ToolWindow(project, toolWindow)
        val cf = ContentFactory.SERVICE.getInstance()
        val content = cf.createContent(tw.getContent(), "All Tasks", false)



        toolWindow.contentManager.addContent(content)
    }
}