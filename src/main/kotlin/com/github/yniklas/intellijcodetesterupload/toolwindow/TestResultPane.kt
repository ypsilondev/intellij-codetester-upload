package com.github.yniklas.intellijcodetesterupload.toolwindow

import com.github.yniklas.intellijcodetesterupload.data.TestResult
import java.awt.*
import java.util.concurrent.Flow
import javax.swing.BoxLayout
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

        add(JLabel("${testResult.title}", null, JLabel.LEFT), c)

        c.gridx = 3
        c.fill = GridBagConstraints.HORIZONTAL
        var jLabel = JLabel(testResult.result, null, JLabel.RIGHT)
        if (testResult.result.equals("FAILED")) {
            jLabel.foreground = Color.RED
        } else {
            jLabel.foreground = Color.GREEN
        }
        add(jLabel, c)
        //for (trm in testResult.output) {
        //    add(TestResultMessagePane(trm))
        //}
        validate()
    }

}