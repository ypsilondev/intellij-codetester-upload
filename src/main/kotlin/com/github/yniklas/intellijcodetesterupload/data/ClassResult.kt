package com.github.yniklas.intellijcodetesterupload.data

import java.util.Objects

data class ClassResult(val className: String, val results: Array<TestResult>) {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return Objects.hash(className, results.hashCode())
    }
}
