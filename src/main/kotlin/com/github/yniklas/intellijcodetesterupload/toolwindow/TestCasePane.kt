package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.TestCase
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.JLabel
import javax.swing.JPanel

class TestCasePane(testCase: TestCase) : JPanel() {
    init {
        add(JLabel("\"" + testCase.name + "\" by"))
        add(JLabel(testCase.creator + " ("))

        val sdf = SimpleDateFormat("dd.MM.yyyy, HH:mm")
        add(JLabel(sdf.format(Date((testCase.creationTime))) + " )"))
    }
}