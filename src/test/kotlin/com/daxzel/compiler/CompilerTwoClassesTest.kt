package com.daxzel.compiler;

import org.junit.jupiter.api.Test

class CompilerTwoClassesTest {

    // No dependency between class

    @Test
    fun testCase1() {
        // A new method has been added.
        incrementalCompilationTest(TWO_CLASSES_GROUP, 1, 2, 1)
    }

    // There is a dependency from one class to another.

    @Test
    fun testCase2() {
        // A new static method has been added.
        // TODO: there is a possibility to avoid rebuilding dependencies if it is only the body change
        incrementalCompilationTest(TWO_CLASSES_GROUP, 2, 2, 2)
    }

    @Test
    fun testCase3() {
        // There is a dependency from one class to another. A method which used by another class has been removed
        incrementalCompilationTest(TWO_CLASSES_GROUP, 3, 2, 2)
    }

    @Test
    fun testCase4() {
        // Both files have been removed
        incrementalCompilationTest(TWO_CLASSES_GROUP, 4, 2, 0)
    }

    @Test
    fun testCase5() {
        // Both files have been added
        incrementalCompilationTest(TWO_CLASSES_GROUP, 5, 0, 2)
    }

    @Test
    fun testCase6() {
        // The main class (on which other depends) has been removed
        incrementalCompilationTest(TWO_CLASSES_GROUP, 6, 2, 1)
    }

    @Test
    fun testCase7() {
        // The main class (on which other depends) has been added
        incrementalCompilationTest(TWO_CLASSES_GROUP, 7, 1, 2)
    }

}
