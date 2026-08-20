package com.quizmaker.android.ui.pricing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quizmaker.android.BuildConfig
import com.quizmaker.android.core.analytics.AnalyticsLogger
import com.quizmaker.android.core.network.AppResult
import com.quizmaker.android.core.payment.RazorpayResult
import com.quizmaker.android.core.payment.RazorpayResultBus
import com.quizmaker.android.data.model.PricingPlan
import com.quizmaker.android.data.model.SaleDay
import com.quizmaker.android.repository.AuthRepository
import com.quizmaker.android.repository.PaymentRepository
import com.quizmaker.android.repository.PricingRepository
import com.quizmaker.android.repository.SaleDayRepository
import com.quizmaker.android.util.TrialStatus
import com.quizmaker.android.util.trialStatus
import com.razorpay.Checkout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

data class PricingUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val badge: String = "",
    val plan: PricingPlan? = null,
    val features: List<String> = emptyList(),
    val isProcessingPayment: Boolean = false,
    val paymentError: String? = null,
    val paymentSuccess: Boolean = false,
    val trialStatus: TrialStatus = TrialStatus.Premium,
    /** Same screen doubles as "license details" for an already-paid account — see PricingScreen. */
    val isPremium: Boolean = false,
    val userType: String? = null,
    val licenseExpiredDate: String? = null,
    /** Non-null while a `sale_day` window is currently live — drives the red sale theme. */
    val activeSale: SaleDay? = null,
    /** Set by [PricingViewModel.activateSalePrice]; null once the 15-minute window lapses. */
    val saleActivatedUntil: Instant? = null
) {
    fun isSalePriceActive(now: Instant = Clock.System.now()): Boolean =
        activeSale != null && saleActivatedUntil != null && now < saleActivatedUntil

    /** The plan's price with the active sale's discount applied, or null if there's nothing to discount. */
    fun discountedAmount(): Int? {
        val discountPercent = activeSale?.discountPercent?.takeIf { it > 0 } ?: return null
        val amount = plan?.amount ?: return null
        return (amount.toLong() * (100 - discountPercent) / 100).toInt()
    }

    fun discountedAmountMinor(): Int? {
        val discountPercent = activeSale?.discountPercent?.takeIf { it > 0 } ?: return null
        val amountMinor = plan?.amountMinor ?: return null
        return (amountMinor.toLong() * (100 - discountPercent) / 100).toInt()
    }
}

@HiltViewModel
class PricingViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val pricingRepository: PricingRepository,
    private val paymentRepository: PaymentRepository,
    private val razorpayResultBus: RazorpayResultBus,
    private val saleDayRepository: SaleDayRepository,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(PricingUiState())
    val uiState: StateFlow<PricingUiState> = _uiState.asStateFlow()

    private var userEmail: String = ""
    private var userCountry: String? = null
    private var pendingOrderId: String? = null
    private var pendingChargeAmountMinor: Int? = null
    private var pendingChargeAmountMajor: Int? = null

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

            val now = Clock.System.now()
            val activeSale = (saleDayRepository.getSaleDays() as? AppResult.Success)?.data?.firstOrNull { sale ->
                val start = sale.startedAt
                val end = sale.endAt
                start != null && end != null && now >= start && now <= end
            }

            // Already paid: this screen just shows the license instead of a plan to buy, so there's
            // no need to fetch pricing at all (and no reason a pricing-fetch hiccup should ever
            // block a premium account from seeing their own license details). Uses .copy() rather
            // than a fresh PricingUiState so a reload triggered right after a successful purchase
            // (see handleRazorpayResult) doesn't clobber the paymentSuccess flag the UI still needs
            // to show its confirmation snackbar.
            if (profile?.isPremium == true) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null,
                    trialStatus = TrialStatus.Premium,
                    isPremium = true,
                    userType = profile.userType,
                    licenseExpiredDate = profile.licenseExpiredDate,
                    activeSale = null,
                    saleActivatedUntil = null
                )
                return@launch
            }

            val isIndianUser = profile?.country?.equals("india", ignoreCase = true) == true
            when (val pricingResult = pricingRepository.getPricing()) {
                is AppResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        badge = pricingResult.data.badge,
                        plan = if (isIndianUser) pricingResult.data.indiaPlan else pricingResult.data.globalPlan,
                        features = pricingResult.data.features,
                        trialStatus = profile?.trialStatus() ?: TrialStatus.Premium,
                        isPremium = false,
                        activeSale = activeSale
                    )
                    analyticsLogger.logPlansViewed(saleActive = activeSale != null)
                    // The discount now applies itself the moment a live sale is in view — no
                    // manual "Unlock" tap. Guarded by isSalePriceActive() so a pull-to-refresh (or
                    // the load() re-run after a purchase attempt) doesn't keep resetting an
                    // already-running 15-minute window.
                    if (activeSale != null && activeSale.discountPercent > 0 && !_uiState.value.isSalePriceActive()) {
                        activateSalePrice()
                    }
                }
                is AppResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = pricingResult.message)
            }
        }
    }

    /** Starts the 15-minute sale-price window — see [PricingUiState.isSalePriceActive]. Auto-fired
     *  from [load] whenever a sale is live; not user-triggered. */
    private fun activateSalePrice() {
        val discountPercent = _uiState.value.activeSale?.discountPercent ?: return
        _uiState.value = _uiState.value.copy(saleActivatedUntil = Clock.System.now() + 15.minutes)
        analyticsLogger.logSalePriceActivated(discountPercent)
    }

    fun dismissPaymentError() {
        _uiState.value = _uiState.value.copy(paymentError = null)
    }

    fun dismissPaymentSuccess() {
        _uiState.value = _uiState.value.copy(paymentSuccess = false)
    }

    fun startCheckout(activity: Activity) {
        val state = _uiState.value
        val plan = state.plan ?: return
        val userId = authRepository.currentUserId() ?: return
        if (BuildConfig.RAZORPAY_KEY_ID.isBlank()) {
            _uiState.value = _uiState.value.copy(paymentError = "Payments aren't configured yet. Please try again later.")
            return
        }
        // Charge the discounted amount only while the 15-minute sale window the user activated is
        // still running — if it lapsed between activating and tapping Buy Now, this quietly falls
        // back to the regular price rather than honoring an expired discount.
        val chargeAmountMinor = if (state.isSalePriceActive()) state.discountedAmountMinor() ?: plan.amountMinor else plan.amountMinor
        pendingChargeAmountMinor = chargeAmountMinor
        pendingChargeAmountMajor = if (state.isSalePriceActive()) state.discountedAmount() ?: plan.amount else plan.amount

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingPayment = true, paymentError = null)
            when (val orderResult = paymentRepository.createOrder(chargeAmountMinor, plan.currency)) {
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
        val chargedAmountMinor = pendingChargeAmountMinor ?: plan.amountMinor
        val chargedAmountMajor = pendingChargeAmountMajor ?: plan.amount

        when (result) {
            is RazorpayResult.Success -> {
                pendingOrderId = null
                pendingChargeAmountMinor = null
                pendingChargeAmountMajor = null
                viewModelScope.launch {
                    when (val verify = paymentRepository.verifyPayment(
                        razorpayOrderId = result.orderId ?: orderId,
                        razorpayPaymentId = result.paymentId,
                        razorpaySignature = result.signature.orEmpty(),
                        userId = userId,
                        country = userCountry
                    )) {
                        is AppResult.Success -> {
                            _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentSuccess = true)
                            analyticsLogger.logPurchase(chargedAmountMajor, plan.currency, plan.label)
                            analyticsLogger.setPremiumStatus(true)
                            // Same screen now shows the license instead of the buy card once the
                            // profile's userType reflects the plan the backend just verified.
                            load()
                        }
                        is AppResult.Error -> {
                            paymentRepository.recordAttempt(userId, chargedAmountMinor, plan.currency, "failed", verify.message)
                            _uiState.value = _uiState.value.copy(isProcessingPayment = false, paymentError = verify.message)
                        }
                    }
                }
            }
            is RazorpayResult.Failure -> {
                pendingOrderId = null
                pendingChargeAmountMinor = null
                pendingChargeAmountMajor = null
                val cancelled = result.code == Checkout.PAYMENT_CANCELED
                val message = result.description?.ifBlank { null }
                    ?: if (cancelled) "User closed Razorpay popup" else "Payment failed"
                viewModelScope.launch {
                    paymentRepository.recordAttempt(userId, chargedAmountMinor, plan.currency, if (cancelled) "cancelled" else "failed", message)
                }
                _uiState.value = _uiState.value.copy(
                    isProcessingPayment = false,
                    paymentError = if (cancelled) "Payment cancelled." else message
                )
            }
        }
    }
}
