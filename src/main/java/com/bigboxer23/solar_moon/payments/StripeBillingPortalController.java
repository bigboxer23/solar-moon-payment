package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.payments.StripeBillingPortalComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.web.Transaction;
import com.stripe.exception.StripeException;
import com.stripe.model.billingportal.Configuration;
import com.stripe.param.billingportal.ConfigurationCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
public class StripeBillingPortalController {

	private final StripeBillingPortalComponent component = new StripeBillingPortalComponent();
	private static final Logger logger = LoggerFactory.getLogger(StripeBillingPortalController.class);

	@Value("${stripe.productId}")
	private String productId;

	@Value("${stripe.subscription.price.mo}")
	private String monthlyPrice;

	@Value("${stripe.subscription.price.yr}")
	private String annualPrice;

	/**
	 * Programmatically create a portal
	 *
	 * @throws StripeException
	 */
	public void createPortal() throws StripeException {

		ConfigurationCreateParams configParams = ConfigurationCreateParams.builder()
				.setBusinessProfile(ConfigurationCreateParams.BusinessProfile.builder()
						.setHeadline("Solar Moon Analytics")
						.build())
				.setFeatures(ConfigurationCreateParams.Features.builder()
						.setInvoiceHistory(ConfigurationCreateParams.Features.InvoiceHistory.builder()
								.setEnabled(true)
								.build())
						.setPaymentMethodUpdate(ConfigurationCreateParams.Features.PaymentMethodUpdate.builder()
								.setEnabled(true)
								.build())
						.setCustomerUpdate(ConfigurationCreateParams.Features.CustomerUpdate.builder()
								.setEnabled(true)
								.setAllowedUpdates(Arrays.asList(
										ConfigurationCreateParams.Features.CustomerUpdate.AllowedUpdate.TAX_ID,
										ConfigurationCreateParams.Features.CustomerUpdate.AllowedUpdate.ADDRESS))
								.build())
						.setSubscriptionUpdate(ConfigurationCreateParams.Features.SubscriptionUpdate.builder()
								.setEnabled(true)
								.addProduct(ConfigurationCreateParams.Features.SubscriptionUpdate.Product.builder()
										.addAllPrice(Arrays.asList(monthlyPrice, annualPrice))
										.setProduct(productId)
										.build())
								.setDefaultAllowedUpdates(Arrays.asList(
										ConfigurationCreateParams.Features.SubscriptionUpdate.DefaultAllowedUpdate
												.PRICE,
										ConfigurationCreateParams.Features.SubscriptionUpdate.DefaultAllowedUpdate
												.QUANTITY))
								.setProrationBehavior(
										ConfigurationCreateParams.Features.SubscriptionUpdate.ProrationBehavior.NONE)
								.build())
						.setSubscriptionCancel(ConfigurationCreateParams.Features.SubscriptionCancel.builder()
								.setEnabled(true)
								.setMode(ConfigurationCreateParams.Features.SubscriptionCancel.Mode.AT_PERIOD_END)
								.setProrationBehavior(
										ConfigurationCreateParams.Features.SubscriptionCancel.ProrationBehavior.NONE)
								.build())
						.setSubscriptionPause(ConfigurationCreateParams.Features.SubscriptionPause.builder()
								.setEnabled(false)
								.build())
						.build())
				.build();
		Configuration.create(configParams);
	}

	@Transaction
	@PostMapping("v1/create-customer-portal-session")
	public String createCustomerPortalSession(HttpServletRequest request) throws StripeException {
		Customer customer = StripeCheckoutController.authorize(request, StripeCheckoutController.customerComponent);
		if (customer == null) {
			logger.warn("Bad customer id");
			return "";
		}
		return component.createCustomerPortalSession(customer);
	}
}
