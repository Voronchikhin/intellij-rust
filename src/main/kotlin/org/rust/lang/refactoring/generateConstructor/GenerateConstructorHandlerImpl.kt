/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.refactoring.generateConstructor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import org.rust.lang.core.psi.*
import org.rust.openapiext.checkWriteAccessAllowed

fun generateConstructorBody(structItem: RsStructItem, editor: Editor) {
    check(!ApplicationManager.getApplication().isWriteAccessAllowed)
    val chosenFields = showConstructorArgumentsChooser(structItem, structItem.project)
    runWriteAction {
        insertNewConstructor(structItem, chosenFields, editor)
    }
}

fun insertNewConstructor(structItem: RsStructItem, selectedFields: List<ConstructorArguments>?, editor: Editor) {
    checkWriteAccessAllowed()
    if (selectedFields == null) return
    val rsPsiFactory = RsPsiFactory(editor.project ?: return)
    var expr = rsPsiFactory.createImpl(structItem.name
        ?: return, listOf(getFunction(structItem, selectedFields, rsPsiFactory)))
    expr = structItem.parent.addAfter(expr, structItem) as RsImplItem
    editor.caretModel.moveToOffset(expr.textOffset + expr.textLength - 1)
}

fun getFunction(structItem: RsStructItem, selectedFields: List<ConstructorArguments>, rsPsiFactory: RsPsiFactory): RsFunction {
    val arguments = buildString {
        append(selectedFields.joinToString(prefix = "(", postfix = ")", separator = ",")
        { "${it.argumentIdentifier}:${(it.typeReference)}" })
    }

    val body = generateBody(structItem, selectedFields)
    return rsPsiFactory.createTraitMethodMember("pub fn new$arguments->Self{\n$body}\n")
}


fun generateBody(structItem: RsStructItem, selectedFields: List<ConstructorArguments>): String {
    val prefix = if (isTupleStruct(structItem)) "(" else "{"
    val postfix = if (isTupleStruct(structItem)) ")" else "}"
    return structItem.nameIdentifier?.text + ConstructorArguments.fromStruct(structItem).joinToString(prefix = prefix, postfix = postfix, separator = ",") {
        if (!selectedFields.contains(it)) {
            it.fieldIdentifier
        } else {
            it.argumentIdentifier
        }
    }
}

fun isTupleStruct(structItem: RsStructItem): Boolean {
    return structItem.tupleFields != null
}


data class ConstructorArguments(val argumentIdentifier: String, val fieldIdentifier: String, val typeReference: String) {
    companion object {
        private fun fromTupleList(tupleFieldList: List<RsTupleFieldDecl>): List<ConstructorArguments> {
            return tupleFieldList.mapIndexed { index: Int, tupleField: RsTupleFieldDecl ->
                ConstructorArguments("field$index", "()", tupleField.typeReference.text ?: "[unknown]")
            }
        }

        fun fromStruct(structItem: RsStructItem): List<ConstructorArguments> {
            return if (isTupleStruct(structItem)) {
                fromTupleList(structItem.tupleFields?.tupleFieldDeclList.orEmpty())
            } else {
                fromFieldList(structItem.blockFields?.fieldDeclList.orEmpty())
            }
        }

        private fun fromFieldList(rsFieldDeclList: List<RsFieldDecl>): List<ConstructorArguments> {
            return rsFieldDeclList.map {
                ConstructorArguments(
                    it.identifier.text ?: "()",
                    it.identifier.text + ":()",
                    it.typeReference?.text
                        ?: "[unknown]"
                )
            }
        }
    }
}
