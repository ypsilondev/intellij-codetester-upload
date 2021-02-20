package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.TestResult
import java.awt.GridBagLayout
import java.awt.GridBagConstraints
import java.awt.Color
import javax.swing.JLabel
import javax.swing.JPanel

class TestResultPane(testResult: TestResult, count: Int) : JPanel() {

    init {
        layout = GridBagLayout()

        val c = GridBagConstraints()
        c.gridwidth = 3
        c.fill = GridBagConstraints.HORIZONTAL
        c.gridy = count
        c.gridx = 0
        c.weightx = 0.8

        add(JLabel(testResult.title, null, JLabel.LEFT), c)

        c.gridx = 3
        c.fill = GridBagConstraints.HORIZONTAL
        val resultLabel = JLabel(testResult.result, null, JLabel.RIGHT)
        if (testResult.result == "FAILED") {
            resultLabel.foreground = Color.RED
        } else {
            resultLabel.foreground = Color.GREEN
        }
        add(resultLabel, c)

        validate()
        repaint()
    }

}