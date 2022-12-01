package com.purchasely.unity.proxy;

public interface JsonErrorProxy {
	void onSuccess(String json);
	void onError(String error);
}
