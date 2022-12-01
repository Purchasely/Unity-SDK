package com.purchasely.unity;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.purchasely.unity.proxy.EventProxy;
import com.purchasely.unity.proxy.PlacementContentProxy;
import com.purchasely.unity.proxy.StartProxy;
import com.purchasely.unity.proxy.UiListenerProxy;
import com.purchasely.unity.proxy.UserLoginProxy;

import java.util.ArrayList;
import java.util.List;

import io.purchasely.billing.Store;
import io.purchasely.ext.EventListener;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYAlertMessage;
import io.purchasely.ext.PLYEvent;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.ext.PLYUIFragmentType;
import io.purchasely.ext.Purchasely;
import io.purchasely.ext.UIListener;
import io.purchasely.google.GoogleStore;
import io.purchasely.models.PLYError;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Keep
public class PurchaselyBridge {
	private StartProxy _startProxy;
	private final EventProxy _eventProxy;
	private final UiListenerProxy _uiListenerProxy;
	private UserLoginProxy _userLoginProxy;
	static PlacementContentProxy placementContentProxy;

	@Keep
	public PurchaselyBridge(Activity activity, String apiKey, String userId, boolean readyToPurchase,
	                        int storeFlags, int logLevel, int runningMode, StartProxy proxy,
	                        EventProxy eventProxy, UiListenerProxy uiListenerProxy) {
		_startProxy = proxy;
		_eventProxy = eventProxy;
		_uiListenerProxy = uiListenerProxy;

		Purchasely.Builder builder = new Purchasely.Builder(activity.getApplicationContext())
				.apiKey(apiKey)
				.isReadyToPurchase(readyToPurchase)
				.logLevel(parseLogLevel(logLevel))
				.runningMode(parseMode(runningMode))
				.stores(parseStoreFlags(storeFlags))
				.uiListener(new UIListener() {
					@Override
					public void onAlert(@NonNull PLYAlertMessage plyAlertMessage) {
						Alert alert = parseAlert(plyAlertMessage);
						_uiListenerProxy.onAlert(alert.type, alert.message);
					}

					@Override
					public void onFragment(@NonNull androidx.fragment.app.Fragment fragment, @NonNull PLYUIFragmentType plyuiFragmentType) {
						_uiListenerProxy.onView(parseViewType(plyuiFragmentType));
					}
				})
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
	public void showContentForPlacement(Activity activity, String placementId, boolean displayCloseButton, PlacementContentProxy proxy, String contentId) {
		placementContentProxy = proxy;

		Intent intent = new Intent(activity, PurchaselyActivity.class);

		intent.putExtra(PurchaselyActivity.EXTRA_PLACEMENT_ID, placementId);
		intent.putExtra(PurchaselyActivity.EXTRA_CONTENT_ID, contentId);
		intent.putExtra(PurchaselyActivity.EXTRA_SHOW_CLOSE_BUTTON, displayCloseButton);

		activity.startActivity(intent);
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

	protected void finalize() {
		placementContentProxy = null;
		Purchasely.close();
	}

	private static class Alert {
		String message;
		int type;
	}

	private Alert parseAlert(PLYAlertMessage alertMessage) {
		Alert alert = new Alert();
		alert.message = "";

		if (alertMessage instanceof PLYAlertMessage.InAppDeferred) {
			alert.type = 2;
		}

		if (alertMessage instanceof PLYAlertMessage.InAppError) {
			alert.type = 6;
			PLYError error = ((PLYAlertMessage.InAppError) alertMessage).getError();
			if (error != null)
				alert.message = error.toString();
		}

		if (alertMessage instanceof PLYAlertMessage.InAppOptionChangedSuccess) {
			alert.type = 10;
			alert.message = ((PLYAlertMessage.InAppOptionChangedSuccess) alertMessage).getRenewalDate();
		}

		if (alertMessage instanceof PLYAlertMessage.InAppRestorationError) {
			alert.type = 5;
			PLYError error = ((PLYAlertMessage.InAppRestorationError) alertMessage).getError();
			if (error != null)
				alert.message = error.toString();
		}

		if (alertMessage instanceof PLYAlertMessage.InAppRestorationSuccess) {
			alert.type = 4;
		}

		if (alertMessage instanceof PLYAlertMessage.InAppSuccess) {
			alert.type = 1;
		}

		if (alertMessage instanceof PLYAlertMessage.InAppSuccessUnauthentified) {
			alert.type = 3;
		}

		return alert;
	}

	private int parseViewType(PLYUIFragmentType type) {
		if (type == PLYUIFragmentType.CANCELLATION_PAGE)
			return 3;
		if (type == PLYUIFragmentType.SUBSCRIPTION_LIST)
			return 0;

		return 1;
	}
}
