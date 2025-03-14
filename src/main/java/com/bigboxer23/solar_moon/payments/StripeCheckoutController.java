package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.payments.CheckoutPrice;
import com.bigboxer23.payments.StripeCheckoutComponent;
import com.bigboxer23.solar_moon.customer.CustomerComponent;
import com.bigboxer23.solar_moon.data.Customer;
import com.bigboxer23.solar_moon.lambda.data.CognitoUserAttributes;
import com.bigboxer23.solar_moon.web.Transaction;
import com.google.gson.Gson;
import com.squareup.moshi.Moshi;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class StripeCheckoutController {
	private static final Gson gson = new Gson();

	protected static final CustomerComponent customerComponent = new CustomerComponent();

	private static final StripeCheckoutComponent checkoutComponent = new StripeCheckoutComponent();

	@Transaction
	@PostMapping(value = "/v1/billing/checkout")
	public String createCheckoutSession(HttpServletRequest request, @RequestBody String checkoutJSON)
			throws StripeException {
		Customer customer = authorize(request, customerComponent);
		if (customer == null) {
			log.warn("Bad customer id");
			return "";
		}
		return gson.toJson(
				checkoutComponent.createCheckoutSession(customer, gson.fromJson(checkoutJSON, CheckoutPrice.class)));
	}

	@Transaction
	@GetMapping("/v1/billing/status")
	public String sessionStatus(HttpServletRequest servletRequest) throws StripeException {
		log.warn("session-status");
		return gson.toJson(checkoutComponent.sessionStatus(servletRequest.getParameter("session_id")));
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
					.flatMap(component::findCustomerByCustomerId)
					.orElse(null);
		} catch (IOException e) {
			log.warn("authorize", e);
		}
		return null;
	}
}
