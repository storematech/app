package com.quizmaker.android.core.payment

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of a Razorpay checkout attempt, as reported to [com.razorpay.PaymentResultWithDataListener]. */
sealed class RazorpayResult {
    data class Success(val orderId: String?, val paymentId: String, val signature: String?) : RazorpayResult()
    data class Failure(val code: Int, val description: String?) : RazorpayResult()
}

/**
 * Razorpay's checkout SDK reports its result to the launching [android.app.Activity] via an
 * interface callback, not a normal Compose/coroutine API — MainActivity implements that
 * interface and forwards results here, so whichever screen actually started the checkout
 * (PricingViewModel) can react without MainActivity needing to know about it directly.
 */
@Singleton
class RazorpayResultBus @Inject constructor() {
    private val _results = MutableSharedFlow<RazorpayResult>(extraBufferCapacity = 1)
    val results: SharedFlow<RazorpayResult> = _results

    suspend fun emit(result: RazorpayResult) {
        _results.emit(result)
    }
}
