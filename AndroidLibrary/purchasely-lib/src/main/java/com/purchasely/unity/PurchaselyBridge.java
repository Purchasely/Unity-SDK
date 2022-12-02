package com.purchasely.unity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Keep;

import com.purchasely.unity.proxy.EventProxy;
import com.purchasely.unity.proxy.PlacementContentProxy;
import com.purchasely.unity.proxy.JsonErrorProxy;
import com.purchasely.unity.proxy.PresentationResultProxy;
import com.purchasely.unity.proxy.StartProxy;
import com.purchasely.unity.proxy.UserLoginProxy;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.purchasely.ext.Purchasely;
import io.purchasely.models.PLYError;
import io.purchasely.models.PLYPlan;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

@Keep
public class PurchaselyBridge {
	private StartProxy _startProxy;
	private final EventProxy _eventProxy;
	private UserLoginProxy _userLoginProxy;
	static PlacementContentProxy placementContentProxy;
	private JsonErrorProxy _restoreProductsProxy;
	private PresentationResultProxy _presentationResultProxy;
	private JsonErrorProxy _productProxy;
	private JsonErrorProxy _planProxy;
	private JsonErrorProxy _allProductsProxy;
	private JsonErrorProxy _planPurchaseProxy;
	private JsonErrorProxy _userSubscriptionsProxy;

	@Keep
	public PurchaselyBridge(Activity activity, String apiKey, String userId, boolean readyToPurchase,
	                        int storeFlags, int logLevel, int runningMode, StartProxy proxy,
	                        EventProxy eventProxy) {
		_startProxy = proxy;
		_eventProxy = eventProxy;

		Purchasely.Builder builder = new Purchasely.Builder(activity.getApplicationContext())
				.apiKey(apiKey)
				.isReadyToPurchase(readyToPurchase)
				.logLevel(Utils.parseLogLevel(logLevel))
				.runningMode(Utils.parseMode(runningMode))
				.stores(Utils.parseStoreFlags(storeFlags))
				.eventListener(plyEvent ->
						_eventProxy.onEventReceived(plyEvent.getName(), plyEvent.getProperties().toJson()));

		if (!userId.isEmpty())
			builder = builder.userId(userId);

		builder.build();

		Purchasely.start((success, error) -> {
			if (_startProxy == null)
				return null;

			_startProxy.onStartCompleted(success, error == null ? "" : error.toString());
			_startProxy = null;
			return null;
		});
	}

	@Keep
	public void userLogin(String userId, UserLoginProxy userLoginProxy) {
		_userLoginProxy = userLoginProxy;

		Purchasely.userLogin(userId, refreshRequired -> {
			_userLoginProxy.onUserLogin(refreshRequired);
			_userLoginProxy = null;

			return null;
		});
	}

	@Keep
	public void setIsReadyToPurchase(boolean ready) {
		Purchasely.setReadyToPurchase(ready);
	}

	@Keep
	public void showContentForPlacement(Activity activity, String placementId,
	                                    PlacementContentProxy proxy, String contentId) {
		placementContentProxy = proxy;

		Intent intent = new Intent(activity, PresentationActivity.class);

		intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PLACEMENT);

		intent.putExtra(PresentationActivity.EXTRA_PLACEMENT_ID, placementId);
		intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

		activity.startActivity(intent);
	}

	@Keep
	public void showContentForPresentation(Activity activity, String presentationId,
	                                       PlacementContentProxy proxy, String contentId) {
		placementContentProxy = proxy;

		Intent intent = new Intent(activity, PresentationActivity.class);

		intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PRESENTATION);

		intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
		intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

		activity.startActivity(intent);
	}

	@Keep
	public void showContentForProduct(Activity activity, String productId,
	                                  PlacementContentProxy proxy, String contentId, String presentationId) {
		placementContentProxy = proxy;

		Intent intent = new Intent(activity, PresentationActivity.class);

		intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PRODUCT);

		intent.putExtra(PresentationActivity.EXTRA_PRODUCT_ID, productId);
		intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
		intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

		activity.startActivity(intent);
	}

	@Keep
	public void showContentForPlan(Activity activity, String planId,
	                               PlacementContentProxy proxy, String contentId, String presentationId) {
		placementContentProxy = proxy;

		Intent intent = new Intent(activity, PresentationActivity.class);

		intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PLAN);

		intent.putExtra(PresentationActivity.EXTRA_PLAN_ID, planId);
		intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
		intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

		activity.startActivity(intent);
	}

	@Keep
	public void restoreAllProducts(boolean silent, JsonErrorProxy proxy) {
		_restoreProductsProxy = proxy;

		if (silent) {
			Purchasely.silentRestoreAllProducts(purchaseRestoredCallback(), purchaseRestoreErrorCallback());
		} else {
			Purchasely.restoreAllProducts(purchaseRestoredCallback(), purchaseRestoreErrorCallback());
		}
	}

	@Keep
	public String getAnonymousUserId() {
		return Purchasely.getAnonymousUserId();
	}

	@Keep
	public void setLanguage(String language) {
		Purchasely.setLanguage(new Locale(language));
	}

	@Keep
	public void userLogout() {
		Purchasely.userLogout();
	}

	@Keep
	public void setDefaultPresentationResultHandler(PresentationResultProxy presentationResultProxy) {
		_presentationResultProxy = presentationResultProxy;

		Purchasely.setDefaultPresentationResultHandler((plyProductViewResult, plan) -> {
			_presentationResultProxy.onPresentationResult(
					Utils.parseProductViewResult(plyProductViewResult), Utils.serializePlan(plan));
			return null;
		});
	}

	@Keep
	public void productWithIdentifier(String productId, JsonErrorProxy proxy) {
		_productProxy = proxy;

		Purchasely.product(productId, plyProduct -> {
			_productProxy.onSuccess(Utils.serializeProduct(plyProduct));
			_productProxy = null;
			return null;
		}, throwable -> {
			_productProxy.onError(throwable.getMessage());
			_productProxy = null;
			return null;
		});
	}

	@Keep
	public void planWithIdentifier(String planId, JsonErrorProxy planProxy) {
		_planProxy = planProxy;

		Purchasely.plan(planId, plan -> {
			_planProxy.onSuccess(Utils.serializePlan(plan));
			_planProxy = null;
			return null;
		}, throwable -> {
			_planProxy.onError(throwable.getMessage());
			_planProxy = null;
			return null;
		});
	}

	@Keep
	public void allProducts(JsonErrorProxy proxy) {
		_allProductsProxy = proxy;

		Purchasely.allProducts(plyProducts -> {
			_allProductsProxy.onSuccess(Utils.serializeProducts(plyProducts));
			_allProductsProxy = null;
			return null;
		}, throwable -> {
			_allProductsProxy.onError(throwable.getMessage());
			_allProductsProxy = null;
			return null;
		});
	}

	@Keep
	public void purchaseWithPlanId(Activity activity, String planId, String contentId, JsonErrorProxy planPurchaseProxy) {
		_planPurchaseProxy = planPurchaseProxy;

		if (contentId.isEmpty())
			contentId = null;

		String finalContentId = contentId;
		Purchasely.plan(planId, plan -> {
			Purchasely.purchase(activity, plan, finalContentId, plan1 -> {
				_planPurchaseProxy.onSuccess(Utils.serializePlan(plan1));
				_planPurchaseProxy = null;
				return null;
			}, throwable -> {
				_planPurchaseProxy.onError(throwable.getMessage());
				_planPurchaseProxy = null;
				return null;
			});

			return null;
		}, throwable -> {
			_planPurchaseProxy.onError(throwable.getMessage());
			_planPurchaseProxy = null;
			return null;
		});
	}

	@Keep
	public void handleDeepLinkUrl(String url) {
		Purchasely.handle(Uri.parse(url));
	}

	@Keep
	public void getUserSubscriptions(JsonErrorProxy proxy) {
		_userSubscriptionsProxy = proxy;

		Purchasely.userSubscriptions(plySubscriptionData -> {
			_userSubscriptionsProxy.onSuccess(Utils.serializeSubscriptions(plySubscriptionData));
			_userSubscriptionsProxy = null;
			return null;
		}, throwable -> {
			_userSubscriptionsProxy.onError(throwable.getMessage());
			_userSubscriptionsProxy = null;
			return null;
		});
	}

	@Keep
	public void presentSubscriptions(Activity activity) {
		Intent intent = new Intent(activity, SubscriptionsActivity.class);
		activity.startActivity(intent);
	}

	@Keep
	public void setUserAttribute(String key, String value) {
		Purchasely.setUserAttribute(key, value);
	}

	@Keep
	public void setUserAttribute(String key, int value) {
		Purchasely.setUserAttribute(key, value);
	}

	@Keep
	public void setUserAttribute(String key, float value) {
		Purchasely.setUserAttribute(key, value);
	}

	@Keep
	public void setUserAttribute(String key, boolean value) {
		Purchasely.setUserAttribute(key, value);
	}

	@Keep
	public void setUserAttributeWithDate(String key, String value) {
		DateFormat dateFormat = getIso8601Format();
		try {
			Date date = dateFormat.parse(value);
			if (date != null)
				Purchasely.setUserAttribute(key, date);
		} catch (Exception e) {
			Log.e("PurchaselyBridge", "setUserAttributeWithDate: ", e);
		}
	}

	@Keep
	public void clearUserAttribute(String key) {
		Purchasely.clearUserAttribute(key);
	}

	@Keep
	public void clearUserAttributes() {
		Purchasely.clearUserAttributes();
	}

	@Keep
	public String getUserAttribute(String key) {
		Object attribute = Purchasely.userAttribute(key);
		if (attribute == null)
			return "";

		if (attribute instanceof Date) {
			DateFormat dateFormat = getIso8601Format();
			return dateFormat.format(attribute);
		}

		return attribute.toString();
	}

	@Keep
	public void userDidConsumeSubscriptionContent() {
		Purchasely.userDidConsumeSubscriptionContent();
	}

	@Keep
	public void setPaywallActionInterceptor() {
		// TODO:
	}

	private Function1<PLYPlan, Unit> purchaseRestoredCallback() {
		return plyPlan -> {
			_restoreProductsProxy.onSuccess(Utils.serializePlan(plyPlan));
			_restoreProductsProxy = null;
			return null;
		};
	}

	private Function1<PLYError, Unit> purchaseRestoreErrorCallback() {
		return plyError -> {
			_restoreProductsProxy.onError(plyError.toString());
			_restoreProductsProxy = null;
			return null;
		};
	}

	private DateFormat getIso8601Format() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		return dateFormat;
	}

	protected void finalize() {
		placementContentProxy = null;
		Purchasely.close();
	}
}
