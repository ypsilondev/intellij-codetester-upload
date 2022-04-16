package com.github.yniklas.intellijcodetesterupload.settings

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBSlider
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class CodeTesterSettingsComponent(project: Project) {

    private val panel: JComponent
    private val useCheckBox = JBCheckBox()
    private val saveBeforeTestCheckBox = JBCheckBox()
    private val timeoutInput = JBTextField

    init {
        useCheckBox.isSelected = CodeTesterSetting.getInstance(project).uniProject
        saveBeforeTestCheckBox.isSelected = CodeTesterSetting.getInstance(project).saveBeforeTesting
        timeoutInput.value = CodeTesterSetting.getInstance(project).timeoutInSeconds.toInt()

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Use this project for Codetester"), useCheckBox)
            .addLabeledComponent(JBLabel("Save all files before testing"), saveBeforeTestCheckBox)
            .addLabeledComponent(JBLabel("Timeout in seconds"), timeoutInput)
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

    fun getTimeoutInSeconds(): Long {
        return timeoutInput.value.toLong()
    }

    fun setUseSelected(newValue: Boolean) {
        useCheckBox.isSelected = newValue
    }

    fun setSaveBeforeTesting(newValue: Boolean) {
        saveBeforeTestCheckBox.isSelected = newValue
    }

    fun setTimeout(seconds: Long) {
        timeoutInput.value = seconds.toInt()
    }

}