package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.solar_moon.SubscriptionComponent;
import com.bigboxer23.solar_moon.web.Transaction;
import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import com.stripe.param.SubscriptionListParams;
import com.stripe.param.SubscriptionUpdateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
public class StripeWebHookController {
	private static final Logger logger = LoggerFactory.getLogger(StripeWebHookController.class);

	private final SubscriptionComponent subscriptionComponent = new SubscriptionComponent();

	@Value("${stripe.wh.secret}")
	private String endpointSecret;

	@Transaction
	@PostMapping("/webhook")
	public String webhook(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody)
			throws StripeException {
		String sigHeader = request.getHeader("Stripe-Signature");
		Event event = null;

		try {
			event = Webhook.constructEvent(requestBody, sigHeader, endpointSecret);
		} catch (JsonSyntaxException e) {
			// Invalid payload
			response.setStatus(400);
			return "";
		} catch (SignatureVerificationException e) {
			// Invalid signature
			response.setStatus(400);
			return "";
		}

		// Deserialize the nested object inside the event
		EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
		StripeObject stripeObject = null;
		if (dataObjectDeserializer.getObject().isPresent()) {
			stripeObject = dataObjectDeserializer.getObject().get();
		} else {
			// Deserialization failed, probably due to an API version mismatch.
			// Refer to the Javadoc documentation on `EventDataObjectDeserializer` for
			// instructions on how to handle this case, or return an error here.
		}
		logger.info("event: " + event.getType());
		// Handle the event
		switch (event.getType()) {
			case "invoice.payment_succeeded":
				Invoice invoice = (Invoice) stripeObject;
				if (invoice.getBillingReason().equals("subscription_create")) {
					// The subscription automatically activates after successful payment
					// Set the payment method used to pay the first invoice
					// as the default payment method for that subscription
					String subscriptionId = invoice.getSubscription();
					String paymentIntentId = invoice.getPaymentIntent();

					// Retrieve the payment intent used to pay the subscription
					PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);

					// Set the default payment method
					Subscription subscription = Subscription.retrieve(subscriptionId);
					SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
							.setDefaultPaymentMethod(paymentIntent.getPaymentMethod())
							.build();
					Subscription updatedSubscription = subscription.update(params);
					logger.warn("Default payment method set for subscription: " + paymentIntent.getPaymentMethod());
				}
				logger.warn("Payment succeeded for invoice: " + event.getId());
				updateActiveSubscriptionCount(invoice.getCustomer());
				break;
			case "invoice.paid":
				updateActiveSubscriptionCount(((Invoice) stripeObject).getCustomer());
				break;
			case "invoice.payment_failed":
				// If the payment fails or the customer does not have a valid payment method,
				// an invoice.payment_failed event is sent, the subscription becomes past_due.
				// Use this webhook to notify your user that their payment has
				// failed and to retrieve new card details.
				updateActiveSubscriptionCount(((Invoice) stripeObject).getCustomer());
				break;
			case "customer.subscription.updated":
			case "customer.subscription.deleted":
				updateActiveSubscriptionCount(((com.stripe.model.Subscription) stripeObject).getCustomer());
				break;
			default:
				// Unhandled event type
				logger.warn("Unhandled event type: " + event.getType());
		}
		response.setStatus(200);
		return "";
	}

	private void updateActiveSubscriptionCount(String stripeCustomerId) throws StripeException {
		long[] count = {0};
		StripeCollection<Subscription> subscriptions = StripeClient.builder()
				.setApiKey(Stripe.apiKey)
				.build()
				.subscriptions()
				.list(new SubscriptionListParams.Builder()
						.setCustomer(stripeCustomerId)
						.build());
		subscriptions.getData().forEach(subscription -> {
			if (subscription.getStatus().equals("active")) {
				subscription.getItems().getData().forEach(item -> {
					count[0] = count[0] + item.getQuantity();
				});
			}
		});
		String customerId = Optional.ofNullable(
						StripeCheckoutController.customerComponent.findCustomerByStripeCustomerId(stripeCustomerId))
				.map(com.bigboxer23.solar_moon.data.Customer::getCustomerId)
				.orElse("");
		subscriptionComponent.updateSubscription(customerId, (int) count[0]);
	}
}
