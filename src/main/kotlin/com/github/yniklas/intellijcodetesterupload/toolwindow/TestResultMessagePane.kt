package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.TestResultMessage
import java.awt.Color
import javax.swing.JLabel

class TestResultMessagePane(trm : TestResultMessage) : JLabel(trm.content) {

    init {
        when (trm.type) {
            "OTHER" -> foreground = Color.GRAY
            "INPUT" -> foreground = Color.LIGHT_GRAY
            "OUTPUT" -> foreground = Color.DARK_GRAY
            "ERROR" -> foreground = Color.RED
        }
    }

}