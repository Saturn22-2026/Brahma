from .payments import UniversalPaymentIntent, UniversalTransactionResult, Currency
from .connectors import PaymentProviderAdapter

class ChameleonPaymentRouter:
    """
    Routes universal payment intents to the correct financial adapter.
    """
    def __init__(self):
        self._adapters = {}

    def register_adapter(self, adapter: PaymentProviderAdapter):
        self._adapters[adapter.provider_name] = adapter
        print(f"[Router] Registered Chameleon Adapter: {adapter.provider_name}")

    def process(self, intent: UniversalPaymentIntent, preferred_rail: str = None) -> UniversalTransactionResult:
        # Default routing logic (e.g., use Stripe for USD, CBDC for BTC)
        if not preferred_rail:
            if intent.currency == Currency.USD:
                preferred_rail = "STRIPE"
            elif intent.currency == Currency.BTC:
                preferred_rail = "FUTURE_CBDC_RAIL"
            else:
                # Fallback to the first available adapter if no matches
                preferred_rail = list(self._adapters.keys())[0] if self._adapters else None

        if not preferred_rail or preferred_rail not in self._adapters:
            raise ValueError(f"Payment rail {preferred_rail} is not integrated into the Chameleon system.")

        adapter = self._adapters[preferred_rail]

        print(f"\n[Router] Routing ${intent.amount/100:.2f} {intent.currency.value} via {preferred_rail}...")
        result = adapter.process_payment(intent)

        return result
