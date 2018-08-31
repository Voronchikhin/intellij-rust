/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.refactoring

import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.rust.RsTestBase
import org.rust.lang.refactoring.generateConstructor.RsStructMemberChooserMember
import org.rust.lang.refactoring.generateConstructor.mockStructMemberChooser

class GenerateConstructorTest : RsTestBase() {
    fun testEmptyTypeDecl() =
        doTest("""
        struct S{
            n :  i32,
            m :
        }
    """,
            listOf(
                ConstructorArgumentsSelection("n  :   i32", true),
                ConstructorArgumentsSelection("m  :   [unknown]", true)),
            """
struct S{
    n :  i32,
    m :
}

impl S {
    pub fn new(n: i32, m: [unknown]) -> Self {
        S { n, m }
    }
}
            """)

    fun testEmptyStruct() = doTest("""
            struct S{}
        """, emptyList(),
        """
struct S{}

impl S {
    pub fn new() -> Self {
        S {}
    }
}"""
    )

    fun testTupleStruct() = doTest("""
        struct Color(i32, i32, i32)/*caret*/;
    """,
        listOf(
            ConstructorArgumentsSelection("field0  :   i32", true),
            ConstructorArgumentsSelection("field1  :   i32", true),
            ConstructorArgumentsSelection("field2  :   i32", true)
        ),
        """
struct Color(i32, i32, i32);

impl Color {
    pub fn new(field0: i32, field1: i32, field2: i32) -> Self {
        Color(field0, field1, field2)
    }
}""")

    fun testSelectNoneFields() = doTest("""
            struct S{
                    n :  i32,/*caret*/
                    m :  i64,
            }
        """,
        listOf(
            ConstructorArgumentsSelection("n  :   i32", false),
            ConstructorArgumentsSelection("m  :   i64", false)
        ),
        """
struct S{
        n :  i32,
        m :  i64,
}

impl S {
    pub fn new() -> Self {
        S { n: (), m: () }
    }
}"""
    )

    fun testSelectAllFields() = doTest("""
            struct S{
                    n :  i32,/*caret*/
                    m :  i64,
            }
        """,
        listOf(
            ConstructorArgumentsSelection("n  :   i32", true),
            ConstructorArgumentsSelection("m  :   i64", true)
        ),
        """
struct S{
        n :  i32,
        m :  i64,
}

impl S {
    pub fn new(n: i32, m: i64) -> Self {
        S { n, m }
    }
}"""
    )


    fun testSelectSomeFields() = doTest("""
            struct S{
                    n :  i32,/*caret*/
                    m :  i64,
            }
        """,
        listOf(
            ConstructorArgumentsSelection("n  :   i32", true),
            ConstructorArgumentsSelection("m  :   i64", false)
        ),
        """
struct S{
        n :  i32,
        m :  i64,
}

impl S {
    pub fn new(n: i32) -> Self {
        S { n, m: () }
    }
}"""
    )


    private data class ConstructorArgumentsSelection(val member: String, val isSelected: Boolean)

    private fun doTest(@Language("Rust") code: String,
                       chooser: List<ConstructorArgumentsSelection>,
                       @Language("Rust") expected: String) {

        checkByText(code.trimIndent(), expected.trimIndent()) {
            mockStructMemberChooser({ _, all ->
                TestCase.assertEquals(all.map { it.formattedText() }, chooser.map { it.member })
                extractSelected(all, chooser)
            }) {
                myFixture.performEditorAction("Rust.GenerateConstructor")
            }
        }
    }

    private fun extractSelected(all: List<RsStructMemberChooserMember>, chooser: List<ConstructorArgumentsSelection>): List<RsStructMemberChooserMember> {
        val selected = chooser.filter { it.isSelected }.map { it.member }
        return all.filter { selected.contains(it.formattedText()) }
    }
}
