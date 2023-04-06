package com.purchasely.unity.proxy;

import io.purchasely.ext.PLYPresentation;

public interface FetchPresentationProxy extends PresentationResultProxy {
	void onPresentationFetched(String json, PLYPresentation presentation);
	void onError(String error);
}
