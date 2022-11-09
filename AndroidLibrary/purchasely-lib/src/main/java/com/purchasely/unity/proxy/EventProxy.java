package com.purchasely.unity.proxy;

import io.purchasely.ext.PLYEvent;

public interface EventProxy {
	void onEventReceived(PLYEvent event);
}
