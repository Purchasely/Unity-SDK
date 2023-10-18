package com.purchasely.unity.proxy;

public interface IntroOfferEligibilityProxy {
	void onSuccess(boolean isEligible);
	void onError(String error);
}
