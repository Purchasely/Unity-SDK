package com.purchasely.unity;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import io.purchasely.billing.Store;
import io.purchasely.ext.DistributionType;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYOfferType;
import io.purchasely.ext.PLYPresentation;
import io.purchasely.ext.PLYPresentationAction;
import io.purchasely.ext.PLYPresentationActionParameters;
import io.purchasely.ext.PLYPresentationInfo;
import io.purchasely.ext.PLYPresentationMetadata;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.ext.PLYSubscriptionOffer;
import io.purchasely.ext.PLYSubscriptionStatus;
import io.purchasely.google.GoogleStore;
import io.purchasely.models.PLYPlan;
import io.purchasely.models.PLYPresentationPlan;
import io.purchasely.models.PLYProduct;
import io.purchasely.models.PLYPromoOffer;
import io.purchasely.models.PLYSubscriptionData;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class Utils {
	static List<Store> parseStoreFlags(int storeFlags) {
		ArrayList<Store> stores = new ArrayList<>();
		if ((storeFlags & 0x00000002) != 0)
			stores.add(new GoogleStore());
//		if ((storeFlags & 0x00000004) != 0)
//			stores.add(new AmazonStore());
//		if ((storeFlags & 0x00000008) != 0)
//			stores.add(new HuaweiStore());

		return stores;
	}

	static LogLevel parseLogLevel(int level) {
		if (level == 0)
			return LogLevel.DEBUG;
		if (level == 2)
			return LogLevel.WARN;
		if (level == 3)
			return LogLevel.ERROR;

		return LogLevel.INFO;
	}

	static PLYRunningMode parseMode(int mode) {
		switch (mode) {
			case 0:
				return PLYRunningMode.Full.INSTANCE;
			case 1:
			case 2:
				return PLYRunningMode.PaywallObserver.INSTANCE;
			default:
				return PLYRunningMode.Full.INSTANCE;
		}
	}

	static int parseProductViewResult(PLYProductViewResult productViewResult) {
		if (productViewResult == PLYProductViewResult.PURCHASED)
			return 0;
		if (productViewResult == PLYProductViewResult.RESTORED)
			return 1;

		return 2;
	}

	static String serializePlan(PLYPlan plan) {
		if (plan == null)
			return "";

		return new JSONObject(plan.toMap()).toString();
	}

	static String serializeProduct(PLYProduct product) {
		if (product == null)
			return "";

		return new JSONObject(product.toMap()).toString();
	}

	static String serializeProducts(List<PLYProduct> products) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < products.size(); i++) {
			result.put(new JSONObject(products.get(i).toMap()));
		}
		return result.toString();
	}

	static int parseOfferType(PLYOfferType type) {
		if (type == null)
			return 0;

		switch (type) {
			case FREE_TRIAL:
				return 1;
			case INTRO_OFFER:
				return 2;
			case PROMO_CODE:
				return 3;
			case PROMOTIONAL_OFFER:
				return 4;
			default:
				return 0;
		}
	}

	static int parseSubscriptionStatus(PLYSubscriptionStatus status) {
		if (status == null)
			return 8;

		switch (status) {
			case AUTO_RENEWING:
				return 0;
			case ON_HOLD:
				return 1;
			case IN_GRACE_PERIOD:
				return 2;
			case AUTO_RENEWING_CANCELED:
				return 3;
			case DEACTIVATED:
				return 4;
			case REVOKED:
				return 5;
			case PAUSED:
				return 6;
			case UNPAID:
				return 7;
		}

		return 8;
	}

	static String serializeSubscriptions(List<PLYSubscriptionData> subscriptions) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < subscriptions.size(); i++) {
			PLYSubscriptionData data = subscriptions.get(i);
			HashMap<String, Object> map = new HashMap<>();
			map.put("plan", transformPlanToMap(data.getPlan()));
			map.put("product", data.getProduct().toMap());
			map.put("contentId", data.getData().getContentId());
			map.put("environment", data.getData().getEnvironment());
			map.put("id", data.getData().getId());
			map.put("isFamilyShared", data.getData().isFamilyShared());
			map.put("offerIdentifier", data.getData().getOfferIdentifier());
			map.put("offerType", parseOfferType(data.getData().getOfferType()));
			map.put("originalPurchasedAt", data.getData().getOriginalPurchasedAt());
			map.put("purchaseToken", data.getData().getPurchaseToken());
			map.put("purchasedDate", data.getData().getPurchasedAt());
			map.put("nextRenewalDate", data.getData().getNextRenewalAt());
			map.put("cancelledDate", data.getData().getCancelledAt());
			map.put("storeCountry", data.getData().getStoreCountry());
			map.put("storeType", data.getData().getStoreType().ordinal());
			Log.d("Kevin", "storeType: " + data.getData().getStoreType().ordinal());
			Log.d("Kevin", "storeType: " + data.getData().getStoreType().name());
			Log.d("Kevin", "storeType: " + data.getData().getStoreType().getDisplayName());
			map.put("status", parseSubscriptionStatus(data.getData().getSubscriptionStatus()));
			result.put(new JSONObject(map));
		}

		return result.toString();
	}

	private static Map<String, Object> transformPlanToMap(PLYPlan plan)  {
		if(plan == null) return new HashMap<>();

		HashMap<String, Object> map = new HashMap<>(plan.toMap());
		if(plan.getType() == DistributionType.CONSUMABLE) {
			map.put("type", DistributionType.CONSUMABLE.ordinal());
		} else if(plan.getType() == DistributionType.NON_CONSUMABLE) {
			map.put("type", DistributionType.RENEWING_SUBSCRIPTION.ordinal());
		} else if(plan.getType() == DistributionType.NON_RENEWING_SUBSCRIPTION) {
			map.put("type", DistributionType.NON_RENEWING_SUBSCRIPTION.ordinal());
		} else if(plan.getType() == DistributionType.UNKNOWN) {
			map.put("type", DistributionType.UNKNOWN.ordinal());
		}
		return map;
	}

	static DateFormat getIso8601Format() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		return dateFormat;
	}

	static String parseActionParameters(PLYPresentationInfo info, PLYPresentationActionParameters presentationActionParameters, PLYPresentationAction action) {
		Uri url = presentationActionParameters.getUrl();
		String urlString = "";
		if (url != null) {
			urlString = url.toString();
		}

		HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("title", presentationActionParameters.getTitle());
		parameters.put("url", urlString);

        PLYPlan plan = presentationActionParameters.getPlan();
        PLYPromoOffer offer = presentationActionParameters.getOffer();
        PLYSubscriptionOffer subscriptionOffer = presentationActionParameters.getSubscriptionOffer();

        if (plan != null)
            parameters.put("plan", plan.toMap());

        if(offer != null)
            parameters.put("offer", offer != null ? toMap(offer) : null);

        if(subscriptionOffer != null)
            parameters.put("subscriptionOffer", subscriptionOffer.toMap());

		HashMap<String, Object> infoMap = new HashMap<>();

		if (info != null) {
			if (info.getContentId() != null)
				infoMap.put("contentId", info.getContentId());
			if (info.getPresentationId() != null)
				infoMap.put("presentationId", info.getPresentationId());
			if (info.getPlacementId() != null)
				infoMap.put("placementId", info.getPlacementId());
			if (info.getAbTestId() != null)
				infoMap.put("abTestId", info.getAbTestId());
			if (info.getAbTestVariantId() != null)
				infoMap.put("abTestVariantId", info.getAbTestVariantId());
		}

		HashMap<String, Object> result = new HashMap<>();
		result.put("info", infoMap);
		result.put("action", action.getValue());
		result.put("parameters", parameters);

		return new JSONObject(result).toString();
	}

	static String parsePresentation(PLYPresentation presentation) {
		Log.d("parsePresentation", presentation.getType().toString());

        Map<String, Object> map = presentation.toMap();

        List<PLYPresentationPlan> plans = presentation.getPlans();
        List<Map<String, String>> mappedPlans = new ArrayList<>();
        for (PLYPresentationPlan plan : plans) {
            mappedPlans.add(toMap(plan));
        }

        map.put("type", presentation.getType().toString().toLowerCase());
        map.put("metadata", toMap(presentation.getMetadata()));
        map.put("plans", mappedPlans);

        return new JSONObject(map).toString();
    }

    static Map<String, String> toMap(PLYPresentationPlan presentationPlan) {
        Map<String, String> resultMap = new HashMap<>();
        if (presentationPlan != null) {
            resultMap.put("planVendorId", presentationPlan.getPlanVendorId());
            resultMap.put("storeProductId", presentationPlan.getStoreProductId());
            resultMap.put("basePlanId", presentationPlan.getBasePlanId());
            resultMap.put("offerId", presentationPlan.getOfferId());
        }
        return resultMap;
    }

    static Map<String, Object> toMap(PLYPresentationMetadata metadata) {
        Map<String, Object> result = new HashMap<>();
        if (metadata != null) {
            Set<String> keys = metadata.keys();
            if (keys != null) {
                for (String key : keys) {
					final Object[] value = new Object[1];
					if (metadata.type(key).equals(String.class.getSimpleName())) {
						metadata.getString(key, new Function1<String, Unit>() {
							@Override
							public Unit invoke(String result) {
								value[0] = result;
								return Unit.INSTANCE;
							}
						});
					}
					else {
						value[0] = metadata.get(key);
					}
                    if (value[0] != null) result.put(key, value[0]);
                }
            }
        }
        return result;
    }

    static Map<String, String> toMap(PLYPromoOffer offer) {
        Map<String, String> map = new HashMap<>();
        map.put("vendorId", offer.getVendorId() != null ? offer.getVendorId() : null);
        map.put("storeOfferId", offer.getStoreOfferId() != null ? offer.getStoreOfferId() : null);
        return map;
    }
}
