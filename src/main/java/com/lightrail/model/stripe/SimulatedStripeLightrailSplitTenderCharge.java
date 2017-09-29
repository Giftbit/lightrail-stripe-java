package com.lightrail.model.stripe;

import com.lightrail.exceptions.*;
import com.stripe.model.Charge;

import java.io.IOException;
import java.util.Map;

public class SimulatedStripeLightrailSplitTenderCharge extends StripeLightrailSplitTenderCharge {
    Map<String, Object> originalParams;

    SimulatedStripeLightrailSplitTenderCharge(LightrailCharge lightrailCharge, Charge stripeCharge, Map<String, Object> originalParams) {
        super(lightrailCharge, stripeCharge);
        this.originalParams = originalParams;
    }

    public StripeLightrailSplitTenderCharge commit() throws CouldNotFindObjectException, ThirdPartyException, IOException, CurrencyMismatchException, InsufficientValueException, AuthorizationException {
        int lightrailShare = 0;
        int stripeShare = 0;
        if (this.getLightrailCharge() != null)
            lightrailShare = getLightrailCharge().getAmount();
        if (this.getStripeCharge() != null)
            stripeShare = getStripeCharge().getAmount().intValue();

        return StripeLightrailSplitTenderCharge.create(originalParams, stripeShare, lightrailShare);
    }

}
