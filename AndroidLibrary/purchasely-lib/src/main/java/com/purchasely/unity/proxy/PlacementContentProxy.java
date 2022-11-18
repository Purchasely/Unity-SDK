package com.purchasely.unity.proxy;

import io.purchasely.models.PLYPlan;

public interface PlacementContentProxy {
	void onContentLoaded(Boolean loaded);
	void onContentClosed();
	void onPresentationResult(int result, PLYPlan plan);
}
