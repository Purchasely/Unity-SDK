package com.purchasely.unity.proxy;

public interface EventProxy {
	void onEventReceived(String id, String name, String propertiesJson);
}
