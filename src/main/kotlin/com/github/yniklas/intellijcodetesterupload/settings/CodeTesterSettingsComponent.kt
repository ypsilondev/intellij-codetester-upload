package com.github.yniklas.intellijcodetesterupload.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class CodeTesterSettingsComponent(project: Project) {

    private val panel: JComponent
    private val cb = JBCheckBox()

    init {
        cb.isSelected = CodeTesterSetting.getInstance(project).uniProject

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Use this project for Codetester"), cb)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel() : JComponent {
        return panel
    }

    fun getPreferredFocusedComponent(): JComponent{
        return cb
    }

    fun isSelected(): Boolean {
        return cb.isSelected
    }

}