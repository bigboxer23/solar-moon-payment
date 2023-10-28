package com.bigboxer23.solar_moon.payments;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** */
@SpringBootApplication
@OpenAPIDefinition(
		info =
				@Info(
						title = "Solar Moon Payment Backend",
						version = "1.0",
						description = "interface w/stripe api",
						contact =
								@Contact(
										name = "bigboxer23@gmail.com",
										url = "https://github.com/bigboxer23/solar-moon-payment")))
public class SolarMoonPaymentApp {
	public static void main(String[] args) {
		SpringApplication.run(SolarMoonPaymentApp.class, args);
	}
}
