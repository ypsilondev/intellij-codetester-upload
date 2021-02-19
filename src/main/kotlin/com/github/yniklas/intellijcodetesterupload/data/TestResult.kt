package com.github.yniklas.intellijcodetesterupload.data

data class TestResult(val title: String,
                      val result: String,
                      val message: String,
                      val output: Array<TestResultMessage>,
                      val errorOutput: String) : Comparable<TestResult> {

    override fun compareTo(other: TestResult): Int {
        return result.compareTo(other.result)
    }
}