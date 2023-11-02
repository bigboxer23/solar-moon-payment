package com.bigboxer23.solar_moon.payments;

import com.bigboxer23.payments.StripeWebhookComponent;
import com.bigboxer23.solar_moon.web.Transaction;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** */
@RestController
public class StripeWebHookController {
	private final StripeWebhookComponent component = new StripeWebhookComponent();

	@Transaction
	@PostMapping("/webhook")
	public String webhook(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody)
			throws StripeException {
		response.setStatus(component.webhook(request.getHeader("Stripe-Signature"), requestBody));
		return "";
	}
}
