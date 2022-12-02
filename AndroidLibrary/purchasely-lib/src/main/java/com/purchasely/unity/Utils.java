package com.purchasely.unity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.purchasely.billing.Store;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.google.GoogleStore;
import io.purchasely.models.PLYPlan;
import io.purchasely.models.PLYProduct;
import io.purchasely.models.PLYSubscriptionData;

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

	static String serializeSubscriptions(List<PLYSubscriptionData> subscriptions) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < subscriptions.size(); i++) {
			result.put(new JSONObject(subscriptions.get(i).toMap()));
		}
		return result.toString();
	}

	static DateFormat getIso8601Format() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		return dateFormat;
	}
}
