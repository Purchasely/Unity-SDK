package com.purchasely.unity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.purchasely.billing.Store;
import io.purchasely.ext.LogLevel;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.PLYRunningMode;
import io.purchasely.google.GoogleStore;
import io.purchasely.models.PLYPlan;
import io.purchasely.models.PLYProduct;
import io.purchasely.models.PLYSubscription;
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
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		return gson.toJson(plan.toMap(), type);
	}

	static String serializeProduct(PLYProduct product) {
		Gson gson = new Gson();
		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		return gson.toJson(product.toMap(), type);
	}

	static String serializeProducts(List<PLYProduct> products) {
		Gson gson = new Gson();
		Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();

		ArrayList<Map<String, Object>> maps = new ArrayList<>();
		for (PLYProduct product : products) {
			maps.add(product.toMap());
		}

		return gson.toJson(maps, type);
	}

	static String serializeSubscriptions(List<PLYSubscriptionData> subscriptions) {
		Gson gson = new Gson();
		Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();

		ArrayList<Map<String, Object>> maps = new ArrayList<>();
		for (PLYSubscriptionData subscriptionData : subscriptions) {
			maps.add(subscriptionData.toMap());
		}

		return gson.toJson(maps, type);
	}
}
