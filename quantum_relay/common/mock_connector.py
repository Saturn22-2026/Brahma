from .connectors import PaymentProviderAdapter
from .payments import UniversalPaymentIntent, UniversalTransactionResult, TransactionStatus
import uuid

class MockStripeConnector(PaymentProviderAdapter):
    """
    A simulated Stripe implementation.
    """
    @property
    def provider_name(self) -> str:
        return "Stripe-Mock"

    def process_payment(self, intent: UniversalPaymentIntent) -> UniversalTransactionResult:
        # Simulate logic: amounts ending in .99 fail
        if intent.amount % 100 == 99:
            return UniversalTransactionResult(
                provider=self.provider_name,
                provider_transaction_id="",
                status=TransactionStatus.FAILED,
                error_code="CARD_DECLINED",
                error_message="Your card has insufficient funds."
            )

        return UniversalTransactionResult(
            provider=self.provider_name,
            provider_transaction_id=f"ch_{uuid.uuid4().hex[:12]}",
            status=TransactionStatus.SUCCESS
        )

    def refund_payment(self, provider_transaction_id: str, amount: int) -> UniversalTransactionResult:
        return UniversalTransactionResult(
            provider=self.provider_name,
            provider_transaction_id=f"re_{uuid.uuid4().hex[:12]}",
            status=TransactionStatus.REFUNDED
        )
