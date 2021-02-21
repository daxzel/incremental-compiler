package com.daxzel.compiler;

import org.junit.jupiter.api.Test

class CompilerTwoClassesTest {

    @Test
    fun testCase1() {
        // No dependency between classes.
        // A new method is added
        incrementalCompilationTest(TWO_CLASSES_GROUP, 1, 2, 1)
    }

    @Test
    fun testCase2() {
        // There is a dependency from one class to another. A new static method has been added.
        // TODO: there is a possibility to avoid rebuilding dependencies if it is only the body change
        incrementalCompilationTest(TWO_CLASSES_GROUP, 2, 2, 2)
    }

    @Test
    fun testCase3() {
        // There is a dependency from one class to another. A method which used by another class has been removed
        incrementalCompilationTest(TWO_CLASSES_GROUP, 2, 2, 2)
    }
    
}
