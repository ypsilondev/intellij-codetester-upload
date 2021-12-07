package com.github.yniklas.intellijcodetesterupload.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class CodeTesterSettingsComponent(project: Project) {

    private val panel: JComponent
    private val useCheckBox = JBCheckBox()
    private val saveBeforeTestCheckBox = JBCheckBox()

    init {
        useCheckBox.isSelected = CodeTesterSetting.getInstance(project).uniProject
        saveBeforeTestCheckBox.isSelected = CodeTesterSetting.getInstance(project).saveBeforeTesting

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Use this project for Codetester"), useCheckBox)
            .addLabeledComponent(JBLabel("Save all files before testing"), saveBeforeTestCheckBox)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPanel() : JComponent {
        return panel
    }

    fun getPreferredFocusedComponent(): JComponent{
        return useCheckBox
    }

    fun isUseSelected(): Boolean {
        return useCheckBox.isSelected
    }

    fun isSaveBeforeTestSelected(): Boolean {
        return saveBeforeTestCheckBox.isSelected
    }

    fun setUseSelected(newValue: Boolean) {
        useCheckBox.isSelected = newValue
    }

    fun setSaveBeforeTesting(newValue: Boolean) {
        saveBeforeTestCheckBox.isSelected = newValue
    }

}