package com.woocommerce.android.ui.products

import android.os.Parcelable
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.assisted.AssistedFactory
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class ProductShippingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    parameterRepository: ParameterRepository,
    private val productRepository: ProductDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val KEY_PRODUCT_PARAMETERS = "key_product_parameters"
    }
    private val navArgs: ProductShippingFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(
        savedState,
        ViewState(
            shippingData = navArgs.shippingData,
            isShippingClassSectionVisible = navArgs.requestCode == RequestCodes.PRODUCT_DETAIL_SHIPPING
        )
    )
    private var viewState by viewStateData

    val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_PRODUCT_PARAMETERS, savedState)
    }

    private val originalShippingData = navArgs.shippingData

    val shippingData
        get() = viewState.shippingData

    private val hasChanges: Boolean
        get() = shippingData != originalShippingData

    fun onDataChanged(
        weight: Float? = shippingData.weight,
        length: Float? = shippingData.length,
        width: Float? = shippingData.width,
        height: Float? = shippingData.height,
        shippingClassSlug: String? = shippingData.shippingClassSlug,
        shippingClassId: Long? = shippingData.shippingClassId
    ) {
        viewState = viewState.copy(
            shippingData = ShippingData(
                weight = weight,
                length = length,
                width = width,
                height = height,
                shippingClassSlug = shippingClassSlug,
                shippingClassId = shippingClassId
            )
        )
    }

    fun onExit() {
        if (hasChanges) {
            triggerEvent(ExitWithResult(shippingData))
        } else {
            triggerEvent(Exit)
        }
    }

    fun getShippingClassByRemoteShippingClassId(remoteShippingClassId: Long) =
        productRepository.getProductShippingClassByRemoteId(remoteShippingClassId)?.name
            ?: shippingData.shippingClassSlug ?: ""

    override fun onCleared() {
        super.onCleared()
        productRepository.onCleanup()
    }

    @Parcelize
    data class ViewState(
        val shippingData: ShippingData = ShippingData(),
        val isShippingClassSectionVisible: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class ShippingData(
        val weight: Float? = null,
        val length: Float? = null,
        val width: Float? = null,
        val height: Float? = null,
        val shippingClassSlug: String? = null,
        val shippingClassId: Long? = null
    ) : Parcelable

    @AssistedFactory
    interface Factory : ViewModelAssistedFactory<ProductShippingViewModel>
}
