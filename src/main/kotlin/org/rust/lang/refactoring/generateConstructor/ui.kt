/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.refactoring.generateConstructor

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.ide.util.MemberChooser
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import org.jetbrains.annotations.TestOnly
import org.rust.lang.core.psi.RsStructItem
import org.rust.openapiext.isUnitTestMode
import javax.swing.JTree

class RsStructMemberChooserMember(val base: MemberChooserObjectBase, val member: ConstructorArguments) : ClassMember {
    private val text: String = "${member.argumentIdentifier}  :   ${member.typeReference}"
    override fun renderTreeNode(component: SimpleColoredComponent?, tree: JTree?) {
        component?.append(text)
    }

    override fun getParentNodeDelegate(): MemberChooserObject? = base

    override fun getText() = "${member.argumentIdentifier} : ${member.typeReference}"

    override fun equals(other: Any?): Boolean {
        return text == (other as? RsStructMemberChooserMember)?.text
    }

    fun formattedText() = text
    override fun hashCode() = text.hashCode()
}

fun showConstructorArgumentsChooser(
    structItem: RsStructItem,
    project: Project
): List<ConstructorArguments>? {

    val base = MemberChooserObjectBase(structItem.name, structItem.getIcon(0))

    val chooser = if (isUnitTestMode) {
        MOCK!!
    } else memberChooserDialog
    return chooser(project,
        ConstructorArguments.argumentsFromStruct(structItem).map { RsStructMemberChooserMember(base, it) })?.map { it.member }
}
typealias StructMemberChooser = (
    project: Project,
    all: List<RsStructMemberChooserMember>
) -> List<RsStructMemberChooserMember>?

private val memberChooserDialog: StructMemberChooser = { project, all ->
    val chooser = MemberChooser(all.toTypedArray(), true, true, project).apply {
        title = "Generate Constructor"
        selectElements(all.toTypedArray())
        setCopyJavadocVisible(false)
    }
    if (!all.isEmpty()) {
        chooser.show()
        chooser.selectedElements
    } else {
        all
    }
}

private var MOCK: StructMemberChooser? = null
@TestOnly
fun mockStructMemberChooser(
    mock: StructMemberChooser,
    f: () -> Unit
) {
    MOCK = { project, all ->
        val result = mock(project, all)
        MOCK = null
        result
    }
    try {
        f()
        check(MOCK == null) { "Selector was not called" }
    } finally {
        MOCK = null
    }
}






