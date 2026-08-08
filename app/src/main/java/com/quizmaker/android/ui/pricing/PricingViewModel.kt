package com.quizmaker.android.ui.pricing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.BuildConfig
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.payment.RazorpayResult
import com.quizmaker.android.core.payment.RazorpayResultBus
import com.quizmaker.android.data.model.PricingPlan
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.PaymentRepository
import com.quizmaker.android.repository.PricingRepository
import com.razorpay.Checkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class PricingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val badge: String = "",
    val plan: PricingPlan? = null,
    val features: List<String> = emptyList(),
    val isProcessingPayment: Boolean = false,
    val paymentError: String? = null,
    val paymentSuccess: Boolean = false
)

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pricingRepository: PricingRepository,
    private val paymentRepository: PaymentRepository,
    private val razorpayResultBus: RazorpayResultBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(PricingUiState())
    val uiState: StateFlow<PricingUiState> = _uiState.asStateFlow()

    private var userEmail: String = ""
    private var userCountry: String? = null
    private var pendingOrderId: String? = null

    init {
        load()
        // MainActivity forwards Razorpay's callback here; only react while a checkout this
        // ViewModel actually started is in flight (pendingOrderId), so a stray/late emission
        // from a previous screen instance can't be misattributed to this one.
        viewModelScope.launch {
            razorpayResultBus.results.collect { result -> handleRazorpayResult(result) }
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val profileResult = authRepository.getCurrentProfile()
            val profile = (profileResult as? AppResult.Success)?.data
            userEmail = profile?.email.orEmpty()
            userCountry = profile?.country
            val isIndianUser = profile?.country?.equals("india", ignoreCase = true) == true

            when (val pricingResult = pricingRepository.getPricing()) {
                is AppResult.Success -> _uiState.value = PricingUiState(
                    isLoading = false,
                    badge = pricingResult.data.badge,
                    plan = if (isIndianUser) pricingResult.data.indiaPlan else pricingResult.data.globalPlan,
                    features = pricingResult.data.features
                )
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = pricingResult.message)
            }
        }
    }

    fun dismissPaymentError() {
        _uiState.value = _uiState.value.copy(paymentError = null)
    }

    fun startCheckout(activity: Activity) {
        val plan = _uiState.value.plan ?: return
        val userId = authRepository.currentUserId() ?: return
        if (BuildConfig.RAZORPAY_KEY_ID.isBlank()) {
            _uiState.value = _uiState.value.copy(paymentError = "Payments aren't configured yet. Please try again later.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, paymentError = null)
            when (val orderResult = paymentRepository.createOrder(plan.amountMinor, plan.currency)) {
                is AppResult.Success -> {
                    pendingOrderId = orderResult.data.id
                    val opened = openRazorpayCheckout(activity, orderResult.data)
                    if (!opened) {
                        pendingOrderId = null
                        _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentError = "Couldn't open the checkout screen. Please try again.")
                    }
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentError = orderResult.message)
            }
        }
    }

    private fun openRazorpayCheckout(activity: Activity, order: PaymentRepository.RazorpayOrder): Boolean {
        val checkout = Checkout().apply { setKeyID(BuildConfig.RAZORPAY_KEY_ID) }
        val options = JSONObject().apply {
            put("name", "Yuno LMS")
            put("description", "Premium Membership")
            put("order_id", order.id)
            put("currency", order.currency)
            put("amount", order.amount)
            put("prefill", JSONObject().apply { put("email", userEmail) })
            put("theme", JSONObject().apply { put("color", "#2563EB") })
        }
        return runCatching { checkout.open(activity, options) }.isSuccess
    }

    private fun handleRazorpayResult(result: RazorpayResult) {
        val orderId = pendingOrderId ?: return
        val userId = authRepository.currentUserId() ?: return
        val plan = _uiState.value.plan ?: return

        when (result) {
            is RazorpayResult.Success -> {
                pendingOrderId = null
                viewModelScope.launch {
                    when (val verify = paymentRepository.verifyPayment(
                        razorpayOrderId = result.orderId ?: orderId,
                        razorpayPaymentId = result.paymentId,
                        razorpaySignature = result.signature.orEmpty(),
                        userId = userId,
                        country = userCountry
                    )) {
                        is AppResult.Success -> _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentSuccess = true)
                        is AppResult.Error -> {
                            paymentRepository.recordAttempt(userId, plan.amountMinor, plan.currency, "failed", verify.message)
                            _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentError = verify.message)
                        }
                    }
                }
            }
            is RazorpayResult.Failure -> {
                pendingOrderId = null
                val cancelled = result.code == Checkout.PAYMENT_CANCELED
                val message = result.description?.ifBlank { null }
                    ?: if (cancelled) "User closed Razorpay popup" else "Payment failed"
                viewModelScope.launch {
                    paymentRepository.recordAttempt(userId, plan.amountMinor, plan.currency, if (cancelled) "cancelled" else "failed", message)
                }
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    paymentError = if (cancelled) "Payment cancelled." else message
                )
            }
        }
    }
}
