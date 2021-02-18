package com.github.yniklas.intellijcodetesterupload.services

import com.github.yniklas.intellijcodetesterupload.MyBundle
import com.intellij.openapi.project.Project

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
