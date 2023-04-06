package com.purchasely.unity.proxy;

public interface PlacementContentProxy extends PresentationResultProxy {
	void onContentLoaded(boolean loaded);
	void onContentClosed();
}
