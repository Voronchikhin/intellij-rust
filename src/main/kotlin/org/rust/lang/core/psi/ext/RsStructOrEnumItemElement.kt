/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext

import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Query
import org.rust.lang.core.psi.RsImplItem
import org.rust.lang.core.psi.RsMetaItem
import org.rust.lang.core.psi.RsTraitItem
import org.rust.lang.core.psi.RsTypeReference
import org.rust.openapiext.filterIsInstanceQuery
import org.rust.openapiext.mapQuery

interface RsStructOrEnumItemElement : RsQualifiedNamedElement, RsItemElement, RsGenericDeclaration, RsTypeDeclarationElement, RsNameIdentifierOwner

val RsStructOrEnumItemElement.derivedTraits: Collection<RsTraitItem>
    get() = deriveMetaItems
        .mapNotNull { it.reference?.resolve() as? RsTraitItem }
        .toList()

val RsStructOrEnumItemElement.derivedTraitsToMetaItems: Map<RsTraitItem, RsMetaItem>
    get() = deriveMetaItems
        .mapNotNull { meta -> (meta.reference?.resolve() as? RsTraitItem)?.let { it to meta } }
        .toMap()

private val RsStructOrEnumItemElement.deriveMetaItems: Sequence<RsMetaItem>
    get() = queryAttributes
        .deriveAttributes
        .flatMap { it.metaItemArgs?.metaItemList?.asSequence() ?: emptySequence() }


fun RsStructOrEnumItemElement.searchForImplementations(): Query<RsImplItem> {
    return ReferencesSearch.search(this, this.useScope)
        .mapQuery { ref ->
            PsiTreeUtil.getTopmostParentOfType(ref.element, RsTypeReference::class.java)?.parent
        }
        .filterIsInstanceQuery<RsImplItem>()
}
