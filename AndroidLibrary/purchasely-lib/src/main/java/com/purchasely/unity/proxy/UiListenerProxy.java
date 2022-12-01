package com.purchasely.unity.proxy;

public interface UiListenerProxy {
	void onAlert(int type, String message);
	void onView(int type);
}
