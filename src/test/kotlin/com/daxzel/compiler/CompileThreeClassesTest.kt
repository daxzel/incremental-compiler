package com.daxzel.compiler;

import org.junit.jupiter.api.Test

class CompileThreeClassesTest {

    @Test
    fun testCase1() {
        // ThirdClass depend on Main. SecondClass depend on Main. Second class is removed.
        // No compilation need, only clean up.
        incrementalCompilationTest(THREE_CLASSES_GROUP, 1, 3, 0)
    }

    @Test
    fun testCase2() {
        // ThirdClass depend on Main. SecondClass depend on Main. We change the method body in Main.
        // We expect that we rebuild all classes.
        // TODO: there is a possibility to avoid rebuilding dependencies if it is only the body change
        incrementalCompilationTest(THREE_CLASSES_GROUP, 2, 3, 3)
    }
}
