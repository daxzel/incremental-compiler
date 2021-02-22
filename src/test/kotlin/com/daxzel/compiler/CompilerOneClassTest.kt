package com.daxzel.compiler;

import org.junit.jupiter.api.Test;

class CompilerOneClassTest {

    @Test
    fun testCase1() {
        // No changes in the file
        incrementalCompilationTest(ONE_CLASS_GROUP, 1, 1, 0)
    }

    @Test
    fun testCase2() {
        // One method has been changed
        incrementalCompilationTest(ONE_CLASS_GROUP, 2, 1, 1)
    }

    @Test
    fun testCase3() {
        // A new method has been added
        incrementalCompilationTest(ONE_CLASS_GROUP, 3, 1, 1)
    }

    @Test
    fun testCase4() {
        // The java file has been removed
        incrementalCompilationTest(ONE_CLASS_GROUP, 4, 1, 0)
    }

    @Test
    fun testCase5() {
        // The java file has been added
        incrementalCompilationTest(ONE_CLASS_GROUP, 5, 0, 1)
    }

}
