package com.bigboxer23;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.staticFiles;

import com.bigboxer23.solar_moon.lambda.utils.PropertyUtils;
import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Server {
	private static Logger logger = LoggerFactory.getLogger(Server.class);

	public static void main(String[] args) throws InterruptedException {
		logger.warn("starting server");
		port(4242);
		Thread.sleep(5000);
		Stripe.apiKey = PropertyUtils.getProperty("stripe.api.key");

		staticFiles.externalLocation(Paths.get("public").toAbsolutePath().toString());

		Gson gson = new Gson();

		post(
				"/create-checkout-session",
				(request, response) -> {
					logger.warn("create-checkout-session");
					String YOUR_DOMAIN = PropertyUtils.getProperty("domain");
					SessionCreateParams params = SessionCreateParams.builder()
							.setUiMode(SessionCreateParams.UiMode.EMBEDDED)
							.setMode(SessionCreateParams.Mode.PAYMENT)
							.setReturnUrl(YOUR_DOMAIN + "/return?session_id={CHECKOUT_SESSION_ID}")
							.setAutomaticTax(SessionCreateParams.AutomaticTax.builder()
									.setEnabled(true)
									.build())
							.addLineItem(SessionCreateParams.LineItem.builder()
									.setQuantity(1L)
									// Provide the exact Price ID (for example,
									// pr_1234) of the product you want to sell
									.setPrice("{{PRICE_ID}}")
									.build())
							.build();

					Session session = Session.create(params);

					Map<String, String> map = new HashMap();
					map.put(
							"clientSecret",
							session.getRawJsonObject()
									.getAsJsonPrimitive("client_secret")
									.getAsString());

					return map;
				},
				gson::toJson);

		get(
				"/session-status",
				(request, response) -> {
					logger.warn("session-status");
					Session session = Session.retrieve(request.queryParams("session_id"));

					Map<String, String> map = new HashMap();
					map.put(
							"status",
							session.getRawJsonObject()
									.getAsJsonPrimitive("status")
									.getAsString());
					map.put(
							"customer_email",
							session.getRawJsonObject()
									.getAsJsonObject("customer_details")
									.getAsJsonPrimitive("email")
									.getAsString());

					return map;
				},
				gson::toJson);
	}
}
