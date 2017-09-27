package com.lightrail.model.stripe;

import com.lightrail.model.business.LightrailTransaction;

import java.util.HashMap;
import java.util.Map;


public abstract class LightrailBaseTransaction {
    LightrailTransaction transactionObject;

    public LightrailTransaction getTransactionObject() {
        return transactionObject;
    }

    public String getIdempotencyKey() {
        return transactionObject.getUserSuppliedId();
    }

    public String getId() {
        return transactionObject.getTransactionId();
    }

    public Map<String, Object> getMetadata() {
        return transactionObject.getMetadata();
    }

    static Map<String, Object> translateToLightrail(Map<String, Object> params) {
        Map<String, Object> translatedParams = new HashMap<>(params);
        //nothing for now
        return translatedParams;
    }

}
