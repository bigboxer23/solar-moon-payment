package com.bigboxer23.payments;

import com.stripe.exception.StripeException;
import org.junit.jupiter.api.Test;

/** */
public class TestStripeBillingPortalComponent {
	private StripeBillingPortalComponent component = new StripeBillingPortalComponent();

	@Test
	public void createCustomerBillingPortal() throws StripeException {
		component.createCustomerBillingPortal();
	}
}
