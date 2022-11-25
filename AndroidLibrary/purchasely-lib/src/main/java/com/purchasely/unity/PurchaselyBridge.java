package com.purchasely.unity;

import android.app.Activity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.purchasely.unity.proxy.EventProxy;
import com.purchasely.unity.proxy.PlacementContentProxy;
import com.purchasely.unity.proxy.StartProxy;
import com.purchasely.unity.proxy.UserLoginProxy;

import java.util.ArrayList;
import java.util.List;

import io.purchasely.billing.Store;
import io.purchasely.ext.EventListener;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYEvent;
import io.purchasely.ext.PLYPresentationViewProperties;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.ext.Purchasely;
import io.purchasely.google.GoogleStore;
import io.purchasely.models.PLYError;
import io.purchasely.models.PLYPlan;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Keep
public class PurchaselyBridge {
	private StartProxy _startProxy;
	private final EventProxy _eventProxy;
	private UserLoginProxy _userLoginProxy;
	private PlacementContentProxy _placementContentProxy;

	@Keep
	public PurchaselyBridge(Activity activity, String apiKey, String userId, boolean readyToPurchase,
	                        int storeFlags, int logLevel, int runningMode, StartProxy proxy, EventProxy eventProxy) {
		_startProxy = proxy;
		_eventProxy = eventProxy;

		Purchasely.Builder builder = new Purchasely.Builder(activity.getApplicationContext())
				.apiKey(apiKey)
				.isReadyToPurchase(readyToPurchase)
				.logLevel(parseLogLevel(logLevel))
				.runningMode(parseMode(runningMode))
				.stores(parseStoreFlags(storeFlags))
				.eventListener(new EventListener() {
					@Override
					public void onEvent(@NonNull PLYEvent plyEvent) {
						_eventProxy.onEventReceived(plyEvent.getName(), plyEvent.getProperties().toJson());
					}
				});

		if (!userId.isEmpty())
			builder = builder.userId(userId);

		builder.build();

		Purchasely.start(new Function2<Boolean, PLYError, Unit>() {
			@Override
			public Unit invoke(Boolean success, PLYError error) {
				if (_startProxy == null)
					return null;

				_startProxy.onStartCompleted(success, error == null ? "" : error.toString());
				_startProxy = null;
				return null;
			}
		});
	}

	@Keep
	public void userLogin(String userId, UserLoginProxy userLoginProxy) {
		_userLoginProxy = userLoginProxy;

		Purchasely.userLogin(userId, new Function1<Boolean, Unit>() {
			@Override
			public Unit invoke(Boolean refreshRequired) {
				_userLoginProxy.onUserLogin(refreshRequired);
				_userLoginProxy = null;

				return null;
			}
		});
	}

	@Keep
	public void setIsReadyToPurchase(boolean ready) {
		Purchasely.setReadyToPurchase(ready);
	}

	@Keep
	public void showContentForPlacement(Activity activity, String placementId, Boolean displayCloseButton, PlacementContentProxy proxy, String contentId) {
		_placementContentProxy = proxy;

		if (contentId.isEmpty())
			contentId = null;

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(placementId, null, null, null, contentId, displayCloseButton, new Function1<Boolean, Unit>() {
			@Override
			public Unit invoke(Boolean isLoaded) {
				_placementContentProxy.onContentLoaded(isLoaded);
				return null;
			}
		}, new Function0<Unit>() {
			@Override
			public Unit invoke() {
				_placementContentProxy.onContentClosed();
				return null;
			}
		});

		Purchasely.presentationView(activity, properties, new Function2<PLYProductViewResult, PLYPlan, Unit>() {
			@Override
			public Unit invoke(PLYProductViewResult plyProductViewResult, PLYPlan plyPlan) {
				_placementContentProxy.onPresentationResult(parseProductViewResult(plyProductViewResult), plyPlan);
				return null;
			}
		});
	}

	private List<Store> parseStoreFlags(int storeFlags) {
		ArrayList<io.purchasely.billing.Store> stores = new ArrayList<>();
		if ((storeFlags & 0x00000002) != 0)
			stores.add(new GoogleStore());
//		if ((storeFlags & 0x00000004) != 0)
//			stores.add(new AmazonStore());
//		if ((storeFlags & 0x00000008) != 0)
//			stores.add(new HuaweiStore());

		return stores;
	}

	private LogLevel parseLogLevel(int level) {
		if (level == 0)
			return LogLevel.DEBUG;
		if (level == 2)
			return LogLevel.WARN;
		if (level == 3)
			return LogLevel.ERROR;

		return LogLevel.INFO;
	}

	private PLYRunningMode parseMode(int mode) {
		if (mode == 0)
			return PLYRunningMode.Observer.INSTANCE;
		if (mode == 1)
			return PLYRunningMode.PaywallObserver.INSTANCE;
		if (mode == 2)
			return PLYRunningMode.PaywallOnly.INSTANCE;
		if (mode == 3)
			return PLYRunningMode.TransactionOnly.INSTANCE;

		return PLYRunningMode.Full.INSTANCE;
	}

	private int parseProductViewResult(PLYProductViewResult productViewResult) {
		if (productViewResult == PLYProductViewResult.PURCHASED)
			return 0;
		if (productViewResult == PLYProductViewResult.RESTORED)
			return 1;

		return 2;
	}

	protected void finalize() {
		Purchasely.close();
	}
}
