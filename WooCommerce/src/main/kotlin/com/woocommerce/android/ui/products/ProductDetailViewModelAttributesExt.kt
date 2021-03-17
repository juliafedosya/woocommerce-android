package com.woocommerce.android.ui.products

import com.woocommerce.android.R.string
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.ProductNavigationTarget.AddProductAttributeTerms
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Product

/**
 * Returns the list of attributes assigned to the product
 */
fun ProductDetailViewModel.getProductDraftAttributes(): List<ProductAttribute> {
    return viewState.productDraft?.attributes ?: emptyList()
}

/*
 * Returns the list of term names for a specific attribute assigned to the product
 */
fun ProductDetailViewModel.getProductDraftAttributeTerms(attributeId: Long, attributeName: String): List<String> {
    val attributes = getProductDraftAttributes()
    attributes.forEach { attribute ->
        if (attribute.id == attributeId && attribute.name == attributeName) {
            return attribute.terms
        }
    }
    return emptyList()
}

/**
 * Returns the draft attribute matching the passed id and name
 */
private fun ProductDetailViewModel.getDraftAttribute(attributeId: Long, attributeName: String): ProductAttribute? {
    return getProductDraftAttributes().firstOrNull {
        it.id == attributeId && it.name == attributeName
    }
}

/**
 * Adds a new term to a the product draft attributes
 */
fun ProductDetailViewModel.addAttributeTermToDraft(attributeId: Long, attributeName: String, termName: String) {
    val updatedTerms = ArrayList<String>()
    var isVisible = ProductAttribute.DEFAULT_VISIBLE
    var isVariation = ProductAttribute.DEFAULT_IS_VARIATION

    // find this attribute in the draft attributes
    getDraftAttribute(attributeId, attributeName)?.let { thisAttribute ->
        // make sure this term doesn't already exist in this attribute
        thisAttribute.terms.forEach {
            if (it.equals(termName, ignoreCase = true)) {
                triggerEvent(ShowSnackbar(string.product_term_name_already_exists))
                return
            }
        }

        // add its terms to our updated term list
        updatedTerms.addAll(thisAttribute.terms)
        isVisible = thisAttribute.isVisible
        isVariation = thisAttribute.isVariation
    }

    // add the passed term to our updated term list
    updatedTerms.add(termName)

    // get the current draft attributes
    val draftAttributes = getProductDraftAttributes()

    // create an updated list without this attribute, then add a new one with the updated terms
    ArrayList<ProductAttribute>().also { updatedAttributes ->
        updatedAttributes.addAll(draftAttributes.filterNot { attribute ->
            attribute.id == attributeId && attribute.name == attributeName
        })

        updatedAttributes.add(
            ProductAttribute(
                id = attributeId,
                name = attributeName,
                terms = updatedTerms,
                isVisible = isVisible,
                isVariation = isVariation
            )
        )

        updateProductDraft(attributes = updatedAttributes)
    }
}

/**
 * Removes a term from the product draft attributes
 */
fun ProductDetailViewModel.removeAttributeTermFromDraft(attributeId: Long, attributeName: String, termName: String) {
    // find this attribute in the draft attributes
    val thisAttribute = getDraftAttribute(attributeId, attributeName)
    if (thisAttribute == null) {
        // TODO
        return
    }

    // created an updated list of terms without the passed one
    val updatedTerms = ArrayList<String>().also { terms ->
        terms.addAll(thisAttribute.terms.filterNot { it.equals(termName, ignoreCase = true) })
    }

    // get the current draft attributes
    val draftAttributes = getProductDraftAttributes()

    // create an updated list without this attribute, then add a new one with the updated terms
    val updatedAttributes = ArrayList<ProductAttribute>().also {
        draftAttributes.filter {
            it.id != attributeId && it.name != attributeName
        }
    }.also {
        it.add(ProductAttribute(
            id = attributeId,
            name = attributeName,
            terms = updatedTerms,
            isVisible = thisAttribute.isVisible,
            isVariation = thisAttribute.isVariation
        ))
    }

    updateProductDraft(attributes = updatedAttributes)
}

/**
 * Returns true if an attribute with this name is assigned to the product draft
 */
private fun ProductDetailViewModel.containsAttributeName(attributeName: String): Boolean {
    viewState.productDraft?.attributes?.forEach {
        if (it.name.equals(attributeName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

/**
 * Called from the attribute list when the user enters a new attribute
 */
fun ProductDetailViewModel.addLocalAttribute(attributeName: String) {
    if (containsAttributeName(attributeName)) {
        triggerEvent(ShowSnackbar(string.product_attribute_name_already_exists))
        return
    }

    // get the list of current attributes
    val attributes = ArrayList<ProductAttribute>()
    viewState.productDraft?.attributes?.let {
        attributes.addAll(it)
    }

    // add the new one to the list
    attributes.add(
        ProductAttribute(
            id = 0L,
            name = attributeName,
            terms = emptyList(),
            isVisible = ProductAttribute.DEFAULT_VISIBLE,
            isVariation = ProductAttribute.DEFAULT_IS_VARIATION
        )
    )

    // update the draft with the new list
    updateProductDraft(attributes = attributes)

    // take the user to the add attribute terms screen
    triggerEvent(AddProductAttributeTerms(0L, attributeName))
}


/**
 * Saves any attribute changes to the backend
 */
fun ProductDetailViewModel.saveAttributeChanges() {
    if (hasAttributeChanges() && checkConnection()) {
        launch {
            viewState.productDraft?.attributes?.let { attributes ->
                val result = productRepository.updateProductAttributes(getRemoteProductId(), attributes)
                if (!result) {
                    triggerEvent(ShowSnackbar(string.product_attributes_error_saving))
                }
            }
        }
    }
}


