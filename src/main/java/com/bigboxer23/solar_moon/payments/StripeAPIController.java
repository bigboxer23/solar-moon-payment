package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.solar_moon.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.CognitoUserAttributes;
import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.bigboxer23.solar_moon.web.Transaction;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.moshi.Moshi;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.model.billingportal.Configuration;
import com.stripe.model.checkout.Session;
import com.stripe.net.*;
import com.stripe.param.billingportal.ConfigurationCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
public class StripeAPIController {
	private static final Logger logger = LoggerFactory.getLogger(StripeAPIController.class);

	private static final Gson gson = new Gson();

	@Value("${domain}")
	private String domain;

	static {
		logger.warn("starting server");
		Stripe.apiKey = PropertyUtils.getProperty("stripe.api.key");
	}

	@Transaction
	@PostMapping(value = "/v1/create-checkout-session")
	public String createCheckoutSession(HttpServletRequest servletRequest, @RequestBody String priceId)
			throws StripeException {
		CheckoutPrice price = gson.fromJson(priceId, CheckoutPrice.class);
		logger.warn("create-checkout-session " + price.getId() + " q:" + price.getCount());
		SessionCreateParams params = SessionCreateParams.builder()
				.setUiMode(SessionCreateParams.UiMode.EMBEDDED)
				.setMode(SessionCreateParams.Mode.SUBSCRIPTION)
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
}
