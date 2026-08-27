from abc import ABC, abstractmethod
from .payments import UniversalPaymentIntent, UniversalTransactionResult

class PaymentProviderAdapter(ABC):
    """
    The Chameleon Interface.
    Every payment gateway (Stripe, PayPal, FutureBank) must morph to fit this shape.
    """
    @property
    @abstractmethod
    def provider_name(self) -> str:
        pass

    @abstractmethod
    def process_payment(self, intent: UniversalPaymentIntent) -> UniversalTransactionResult:
        pass

    @abstractmethod
    def refund_payment(self, provider_transaction_id: str, amount: int) -> UniversalTransactionResult:
        pass
