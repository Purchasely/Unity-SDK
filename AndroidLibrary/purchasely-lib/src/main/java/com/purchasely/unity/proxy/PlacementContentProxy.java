package com.purchasely.unity.proxy;

public interface PlacementContentProxy extends PresentationResultProxy {
	void onContentLoaded(Boolean loaded);
	void onContentClosed();
}
