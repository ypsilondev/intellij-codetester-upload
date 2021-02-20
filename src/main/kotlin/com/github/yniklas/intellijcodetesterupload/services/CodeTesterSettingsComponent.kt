package com.github.yniklas.intellijcodetesterupload.services

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent

class CodeTesterSettingsComponent {

    private val panel: JComponent
    private val cb = JBCheckBox()

    init {
        cb.isSelected = CodeTesterSetting.getInstance().uniProject

        panel = FormBuilder.createFormBuilder().addLabeledComponent(JBLabel("Use this project for Codetester"), cb)
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