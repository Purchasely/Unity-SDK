package com.purchasely.unity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Keep;

import com.purchasely.unity.proxy.FetchPresentationProxy;
import com.purchasely.unity.proxy.PaywallInterceptorProxy;
import com.purchasely.unity.proxy.PlacementContentProxy;
import com.purchasely.unity.proxy.JsonErrorProxy;
import com.purchasely.unity.proxy.PresentationResultProxy;
import com.purchasely.unity.proxy.StartProxy;
import com.purchasely.unity.proxy.UserLoginProxy;

import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.purchasely.ext.PLYPresentation;
import io.purchasely.ext.PLYPresentationAction;
import io.purchasely.ext.PLYPresentationViewProperties;
import io.purchasely.ext.PLYProcessActionListener;
import io.purchasely.ext.Purchasely;
import io.purchasely.models.PLYError;
import io.purchasely.models.PLYPlan;
import io.purchasely.models.PLYPromoOffer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

@Keep
public class PurchaselyBridge {
    private StartProxy _startProxy;
    private UserLoginProxy _userLoginProxy;
    static PlacementContentProxy placementContentProxy;
    private JsonErrorProxy _restoreProductsProxy;
    private PresentationResultProxy _presentationResultProxy;
    private JsonErrorProxy _productProxy;
    private JsonErrorProxy _planProxy;
    private JsonErrorProxy _allProductsProxy;
    private JsonErrorProxy _planPurchaseProxy;
    private JsonErrorProxy _userSubscriptionsProxy;
    private FetchPresentationProxy _presentationProxy;
    private PaywallInterceptorProxy _paywallInterceptorProxy;

    private PLYProcessActionListener processActionListener;
    private PLYPresentationAction paywallAction;

    WeakReference<Activity> unityActivity;

    static PresentationActivityCache presentationActivityCache = null;

    @Keep
    public PurchaselyBridge(Activity activity, String apiKey, String userId, boolean readyToOpenDeeplink,
                            int storeFlags, int logLevel, int runningMode, StartProxy proxy) {
        _startProxy = proxy;

        unityActivity = new WeakReference<>(activity);

        Purchasely.Builder builder = new Purchasely.Builder(activity.getApplicationContext())
                .apiKey(apiKey)
                .readyToOpenDeeplink(readyToOpenDeeplink)
                .logLevel(Utils.parseLogLevel(logLevel))
                .runningMode(Utils.parseMode(runningMode))
                .stores(Utils.parseStoreFlags(storeFlags));


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
    public void setReadyToOpenDeeplink(boolean ready) {
        Purchasely.setReadyToOpenDeeplink(ready);
    }

    @Keep
    public void showContentForPlacement(
            Activity activity,
            String placementId,
            PlacementContentProxy proxy,
            String contentId) {

        placementContentProxy = proxy;
        Intent intent = new Intent(activity, PresentationActivity.class);
        intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PLACEMENT);
        if (!placementId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PLACEMENT_ID, placementId);
        if (!contentId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

        activity.startActivity(intent);
    }

    @Keep
    public void showContentForPresentation(Activity activity, String presentationId,
                                           PlacementContentProxy proxy, String contentId) {
        placementContentProxy = proxy;

        Intent intent = new Intent(activity, PresentationActivity.class);

        intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PRESENTATION);

        if (!presentationId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
        if (!contentId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

        activity.startActivity(intent);
    }

    @Keep
    public void showContentForProduct(Activity activity, String productId,
                                      PlacementContentProxy proxy, String contentId, String presentationId) {
        placementContentProxy = proxy;

        Intent intent = new Intent(activity, PresentationActivity.class);

        intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PRODUCT);

        if (!productId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PRODUCT_ID, productId);
        if (!presentationId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
        if (!contentId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);

        activity.startActivity(intent);
    }

    @Keep
    public void showContentForPlan(Activity activity, String planId,
                                   PlacementContentProxy proxy, String contentId, String presentationId) {
        placementContentProxy = proxy;

        Intent intent = new Intent(activity, PresentationActivity.class);

        intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PLAN);

        if (!planId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PLAN_ID, planId);
        if (!presentationId.isEmpty())
            intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
        if (!contentId.isEmpty())
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
            if (plyProduct == null) {
                _productProxy.onError("Could not find product " + productId);
                return null;
            }

            _productProxy.onSuccess(Utils.serializeProduct(plyProduct));
            return null;
        }, throwable -> {
            _productProxy.onError(throwable.getMessage());
            return null;
        });
    }

    @Keep
    public void planWithIdentifier(String planId, JsonErrorProxy planProxy) {
        _planProxy = planProxy;

        Purchasely.plan(planId, plan -> {
            if (plan == null) {
                _planProxy.onError("Could not find plan " + planId);
                return null;
            }

            _planProxy.onSuccess(Utils.serializePlan(plan));
            return null;
        }, throwable -> {
            _planProxy.onError(throwable.getMessage());
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
    public void purchase(Activity activity, String planId, String offerId, String contentId, JsonErrorProxy planPurchaseProxy) {
        _planPurchaseProxy = planPurchaseProxy;

        if (contentId.isEmpty())
            contentId = null;

        String finalContentId = contentId;
        Purchasely.plan(planId, plan -> {
            if (plan == null) {
                _planPurchaseProxy.onError("Could not find plan " + planId);
                _planPurchaseProxy = null;
                return null;
            }

            PLYPromoOffer offer = null;
            for (PLYPromoOffer promoOffer : plan.getPromoOffers()) {
                if (promoOffer.getVendorId().equals(offerId)) {
                    offer = promoOffer;
                    break;
                }
            }

            Purchasely.purchase(activity, plan, offer, finalContentId, plan1 -> {
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
    public boolean isDeeplinkHandled(String url) {
        return Purchasely.isDeeplinkHandled(Uri.parse(url));
    }

    @Keep
    public boolean isAnonymous() {
        return Purchasely.isAnonymous();
    }

    @Keep
    public boolean isEligibleForIntroOffer(String planVendorId) {
        final boolean[] isEligible = {false};
        CountDownLatch latch = new CountDownLatch(1);

        Purchasely.plan(planVendorId, plan -> {
            if (plan == null) {
                Log.e("PurchaselyBridge", "Could not find plan " + planVendorId);
                isEligible[0] = false;
            } else {
                for (PLYPromoOffer offer : plan.getPromoOffers()) {
                    if (plan.isEligibleToIntroOffer(offer.getStoreOfferId())) {
                        isEligible[0] = true;
                        break;
                    }
                }
            }
            latch.countDown();
            return null;
            },
            throwable -> {
                latch.countDown();
                isEligible[0] = false;
                return null;
            }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return isEligible[0];
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
        DateFormat dateFormat = Utils.getIso8601Format();
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
            DateFormat dateFormat = Utils.getIso8601Format();
            return dateFormat.format(attribute);
        }

        return attribute.toString();
    }

    @Keep
    public void userDidConsumeSubscriptionContent() {
        Purchasely.userDidConsumeSubscriptionContent();
    }

    @Keep
    public void setPaywallActionInterceptor(PaywallInterceptorProxy proxy) {
        _paywallInterceptorProxy = proxy;

        Purchasely.setPaywallActionsInterceptor((info, plyPresentationAction, map, plyProcessActionListener) -> {
            processActionListener = plyProcessActionListener;
            paywallAction = plyPresentationAction;

            String actionJson = Utils.parseActionParameters(info, map, plyPresentationAction);
            _paywallInterceptorProxy.onAction(actionJson);
        });
    }

    @Keep
    public void fetchPresentation(String presentationId, String contentId, FetchPresentationProxy presentationProxy) {
        _presentationProxy = presentationProxy;

        PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
                null, presentationId, null, null, contentId, true,
                null, null, null, null);

        Purchasely.fetchPresentation(properties, fetchPresentationCallback());
    }

    @Keep
    public void fetchPresentationForPlacement(String placementId, String contentId, FetchPresentationProxy presentationProxy) {
        _presentationProxy = presentationProxy;

        PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
                placementId, null, null, null, contentId, true,
                null, null, null, null);

        Purchasely.fetchPresentation(properties, fetchPresentationCallback());
    }

    @Keep
    public void clientPresentationOpened(PLYPresentation presentation) {
        Purchasely.clientPresentationDisplayed(presentation);
    }

    @Keep
    public void clientPresentationClosed(PLYPresentation presentation) {
        Purchasely.clientPresentationClosed(presentation);
    }

    @Keep
    public void showContentForPresentation(Activity activity, PLYPresentation presentation,
                                           PlacementContentProxy proxy) {
        placementContentProxy = proxy;

        Intent intent = new Intent(activity, PresentationActivity.class);

        intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, PresentationActivity.CODE_PRESENTATION_WITH_VIEW);

        intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentation.getId());
        intent.putExtra(PresentationActivity.EXTRA_PLACEMENT_ID, presentation.getPlacementId());

        intent.putExtra(PresentationActivity.EXTRA_PRESENTATION, presentation);

        activity.startActivity(intent);
    }

    private void showPresentation() {
        Activity activity = unityActivity.get();
        presentationActivityCache.relaunch(activity);
    }

    private void closePresentation() {
        Activity purchaselyActivity = null;
        if (presentationActivityCache != null && presentationActivityCache.activity != null) {
            purchaselyActivity = presentationActivityCache.activity.get();
            purchaselyActivity.finish();
            presentationActivityCache.activity = null;
        }
    }

    private void hidePresentation() {
        Activity purchaselyActivity = null;
        if (presentationActivityCache != null && presentationActivityCache.activity != null) {
            purchaselyActivity = presentationActivityCache.activity.get();
        }

        Activity unityActivitySaved = unityActivity.get();
        Activity currentActivity = purchaselyActivity != null ? purchaselyActivity : unityActivitySaved;
        Intent intent = new Intent(currentActivity, unityActivitySaved.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        unityActivitySaved.startActivity(intent);

    }

    private Function1<PLYPlan, Unit> purchaseRestoredCallback() {
        return plyPlan -> {
            _restoreProductsProxy.onSuccess(Utils.serializePlan(plyPlan));
            _restoreProductsProxy = null;
            return null;
        };
    }

    @Keep
    public void processPaywallAction(Activity activity, boolean processAction) {
        if (processActionListener == null) return;

        PLYPresentationAction action = PLYPresentationAction.CLOSE;
        if (paywallAction != null) action = paywallAction;

        switch (action) {
            case PROMO_CODE:
            case RESTORE:
            case PURCHASE:
            case LOGIN:
            case OPEN_PRESENTATION:
                processActionWithPaywallActivity(activity, processAction);
                break;
            default:
                activity.runOnUiThread(() -> processActionListener.processAction(processAction));
        }
    }

    private void processActionWithPaywallActivity(Activity activity, boolean processAction) {
        if (processActionListener == null) return;

        boolean softRelaunched = presentationActivityCache != null && presentationActivityCache.relaunch(activity);
        if (softRelaunched) {
            activity.runOnUiThread(() -> processActionListener.processAction(processAction));
        } else {
            new Thread(() -> {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Log.e("PurchaselyBridge", "Process action error.", e);
                } finally {
                    activity.runOnUiThread(() -> processActionListener.processAction(processAction));
                }
            }).start();
        }
    }

    private Function1<PLYError, Unit> purchaseRestoreErrorCallback() {
        return plyError -> {
            _restoreProductsProxy.onError(plyError.toString());
            _restoreProductsProxy = null;
            return null;
        };
    }

    protected void finalize() {
        placementContentProxy = null;
        _presentationProxy = null;
        processActionListener = null;
        paywallAction = null;
        unityActivity = null;
        presentationActivityCache = null;
        Purchasely.close();
    }

    private Function2<PLYPresentation, PLYError, Unit> fetchPresentationCallback() {
        return (presentation, error) -> {
            if (error != null) {
                _presentationProxy.onError(error.getMessage());
            }

            if (presentation != null) {
                _presentationProxy.onPresentationFetched(Utils.parsePresentation(presentation), presentation);
            }

            return null;
        };
    }

    static class PresentationActivityCache {
        String presentationId = null;
        String placementId = null;
        String productId = null;
        String planId = null;
        String contentId = null;
        int actionCode = -1;

        WeakReference<PresentationActivity> activity = null;

        boolean relaunch(Activity unityActivity) {
            PresentationActivity backgroundActivity = null;

            if (activity != null) {
                backgroundActivity = activity.get();
            }

            Intent intent;

            boolean relaunchExisting = backgroundActivity != null
                    && !backgroundActivity.isFinishing()
                    && !backgroundActivity.isDestroyed();


            if (relaunchExisting) {
                intent = new Intent(backgroundActivity, PresentationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            } else {
                intent = new Intent(unityActivity, PresentationActivity.class);
            }

            intent.putExtra(PresentationActivity.EXTRA_PRESENTATION_ID, presentationId);
            intent.putExtra(PresentationActivity.EXTRA_PRODUCT_ID, productId);
            intent.putExtra(PresentationActivity.EXTRA_PLAN_ID, planId);
            intent.putExtra(PresentationActivity.EXTRA_PLACEMENT_ID, placementId);
            intent.putExtra(PresentationActivity.EXTRA_CONTENT_ID, contentId);
            intent.putExtra(PresentationActivity.EXTRA_ACTION_CODE, actionCode);

            unityActivity.startActivity(intent);
            return relaunchExisting;
        }
    }
}
