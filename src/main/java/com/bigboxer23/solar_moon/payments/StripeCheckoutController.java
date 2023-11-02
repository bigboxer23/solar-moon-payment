package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.solar_moon.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.CognitoUserAttributes;
import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.bigboxer23.solar_moon.web.Transaction;
import com.google.gson.Gson;
import com.squareup.moshi.Moshi;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StripeCheckoutController {
	private static final Logger logger = LoggerFactory.getLogger(StripeCheckoutController.class);

	private static final Gson gson = new Gson();

	protected static final CustomerComponent customerComponent = new CustomerComponent();

	@Value("${domain}")
	private String domain;

	static {
		logger.warn("starting server");
		Stripe.apiKey = PropertyUtils.getProperty("stripe.api.key");
	}

	@Transaction
	@PostMapping(value = "/v1/create-checkout-session")
	public String createCheckoutSession(HttpServletRequest request, @RequestBody String checkoutJSON)
			throws StripeException {
		CheckoutPrice price = gson.fromJson(checkoutJSON, CheckoutPrice.class);
		Customer customer = authorize(request, customerComponent);
		if (customer == null) {
			logger.warn("Bad customer id");
			return "";
		}
		logger.warn("create-checkout-session " + price.getId() + " q:" + price.getCount());
		SessionCreateParams params = SessionCreateParams.builder()
				.setUiMode(SessionCreateParams.UiMode.EMBEDDED)
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
				.setCustomer(customer.getStripeCustomerId())
				.setCustomerUpdate(SessionCreateParams.CustomerUpdate.builder()
						.setAddress(SessionCreateParams.CustomerUpdate.Address.AUTO)
						.build())
				.setReturnUrl(domain + "/return?session_id={CHECKOUT_SESSION_ID}")
				.setAutomaticTax(SessionCreateParams.AutomaticTax.builder()
						.setEnabled(true)
						.build())
				.addLineItem(SessionCreateParams.LineItem.builder()
						.setQuantity(price.getCount())
						.setPrice(price.getId())
						.build())
				.build();

		Session session = Session.create(params);

		Map<String, String> map = new HashMap<>();
		map.put(
				"clientSecret",
				session.getRawJsonObject().getAsJsonPrimitive("client_secret").getAsString());
		return gson.toJson(map);
	}

	@Transaction
	@GetMapping("/session-status")
	public String sessionStatus(HttpServletRequest servletRequest) throws StripeException {
		logger.warn("session-status");
		Session session = Session.retrieve(servletRequest.getParameter("session_id"));

		Map<String, String> map = new HashMap<>();
		map.put(
				"status",
				session.getRawJsonObject().getAsJsonPrimitive("status").getAsString());
		map.put(
				"customer_email",
				session.getRawJsonObject()
						.getAsJsonObject("customer_details")
						.getAsJsonPrimitive("email")
						.getAsString());

		return gson.toJson(map);
	}

	public static Customer authorize(HttpServletRequest servletRequest, CustomerComponent component) {

		String[] chunks = Optional.ofNullable(servletRequest.getHeader(HttpHeaders.AUTHORIZATION))
				.map(authHeader -> authHeader.split("\\."))
				.orElse(null);
		if (chunks == null || chunks.length != 3) {
			return null;
		}
		try {
			return Optional.ofNullable(new Moshi.Builder()
							.build()
							.adapter(CognitoUserAttributes.class)
							.fromJson(new String(Base64.getUrlDecoder().decode(chunks[1]))))
					.map(CognitoUserAttributes::getSub)
					.map(component::findCustomerByCustomerId)
					.orElse(null);
		} catch (IOException e) {
			logger.warn("authorize", e);
		}
		return null;
	}
}
