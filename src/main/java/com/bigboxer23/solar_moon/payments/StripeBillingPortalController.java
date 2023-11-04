package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.payments.StripeBillingPortalComponent;
import com.bigboxer23.payments.StripeSubscriptionComponent;
import com.bigboxer23.payments.SubscriptionPriceInfo;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.web.Transaction;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
public class StripeBillingPortalController {

	private final StripeBillingPortalComponent component = new StripeBillingPortalComponent();

	private final StripeSubscriptionComponent subscriptionComponent = new StripeSubscriptionComponent();

	private static final Logger logger = LoggerFactory.getLogger(StripeBillingPortalController.class);

	@Transaction
	@PostMapping("v1/billing/portal")
	public String createCustomerPortalSession(HttpServletRequest request) throws StripeException {
		Customer customer = StripeCheckoutController.authorize(request, StripeCheckoutController.customerComponent);
		if (customer == null) {
			logger.warn("Bad customer id");
			return "";
		}
		return component.createCustomerPortalSession(customer);
	}

	@Transaction
	@GetMapping("v1/billing/subscriptions")
	public List<SubscriptionPriceInfo> getActiveSubscriptions(HttpServletRequest request) throws StripeException {
		Customer customer = StripeCheckoutController.authorize(request, StripeCheckoutController.customerComponent);
		if (customer == null) {
			logger.warn("Bad customer id");
			return Collections.emptyList();
		}
		return subscriptionComponent.getActiveSubscriptionPriceInfo(customer.getStripeCustomerId());
	}

	public void createCustomerBillingPortal() throws StripeException
	{
		component.createCustomerBillingPortal();
	}
}
