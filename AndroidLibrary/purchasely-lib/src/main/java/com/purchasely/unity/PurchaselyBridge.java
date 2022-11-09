package com.purchasely.unity;

import android.app.Activity;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.purchasely.unity.proxy.EventProxy;
import com.purchasely.unity.proxy.StartProxy;

import java.util.ArrayList;
import java.util.List;

import io.purchasely.amazon.AmazonStore;
import io.purchasely.billing.Store;
import io.purchasely.ext.EventListener;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYEvent;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.ext.Purchasely;
import io.purchasely.google.GoogleStore;
import io.purchasely.huawei.HuaweiStore;
import io.purchasely.models.PLYError;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;

@Keep
public class PurchaselyBridge {
	private StartProxy _startProxy;
	private final EventProxy _eventProxy;

	@Keep
	public PurchaselyBridge(Activity activity, String apiKey, String userId, boolean readyToPurchase,
	                        int storeFlags, int logLevel, int runningMode, EventProxy eventProxy, StartProxy proxy) {
		_startProxy = proxy;
		_eventProxy = eventProxy;

		new Purchasely.Builder(activity.getApplicationContext())
				.apiKey(apiKey)
				.userId(userId)
				.isReadyToPurchase(readyToPurchase)
				.logLevel(parseLogLevel(logLevel))
				.runningMode(parseMode(runningMode))
				.stores(parseStoreFlags(storeFlags))
				.eventListener(new EventListener() {
					@Override
					public void onEvent(@NonNull PLYEvent plyEvent) {
						_eventProxy.onEventReceived(plyEvent);
					}
				})
				.build();

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

	private List<Store> parseStoreFlags(int storeFlags) {
		ArrayList<io.purchasely.billing.Store> stores = new ArrayList<>();
		if ((storeFlags & 0x00000002) != 0)
			stores.add(new GoogleStore());
		if ((storeFlags & 0x00000004) != 0)
			stores.add(new AmazonStore());
		if ((storeFlags & 0x00000008) != 0)
			stores.add(new HuaweiStore());

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

	protected void finalize() {
		Purchasely.close();
	}
}
