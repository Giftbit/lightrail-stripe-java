package com.lightrail.exceptions;

public class ThirdPartyException extends Exception {
    public ThirdPartyException (Exception e) {
        super(e);
    }
}
