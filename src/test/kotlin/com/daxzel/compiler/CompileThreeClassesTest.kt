package com.daxzel.compiler;

import org.junit.jupiter.api.Disabled
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
        // If the SecondClass's build succeeds we don't rebuild the ThirdClass
        // TODO: there is a possibility to avoid rebuilding dependencies if it is only the body change
        incrementalCompilationTest(THREE_CLASSES_GROUP, 2, 3, 2)
    }

    @Test
    @Disabled("""
       Javac doesn't work very well in that case - it creates SecondClass.class which depends on the ThirdClass.class 
       which doesn't build. Our compiler is a bit smarter in that case and removes SecondClass.class.
    """)
    fun testCase3() {
        // ThirdClass depend on Main. SecondClass depend on Main. We remove the method in Main.
        // As the SecondClass's build fails we try to rebuild the ThirdClass and fail as well
        // It is possible to avoid rebuilding the third class in dependency in that case, but we would miss the
        // build error in that case.
        incrementalCompilationTest(THREE_CLASSES_GROUP, 3, 3, 3)
    }
}
