package com.purchasely.unity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import io.purchasely.ext.PLYPresentationViewProperties;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.Purchasely;
import io.purchasely.models.PLYPlan;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class PurchaselyActivity extends AppCompatActivity {
	final static String EXTRA_PLACEMENT_ID = "EXTRA_PLACEMENT_ID";
	final static String EXTRA_CONTENT_ID = "EXTRA_CONTENT_ID";
	final static String EXTRA_PRESENTATION_ID = "EXTRA_PRESENTATION_ID";
	final static String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
	final static String EXTRA_PLAN_ID = "EXTRA_PLAN_ID";

	final static String EXTRA_ACTION_CODE = "EXTRA_ACTION_CODE";

	final static int CODE_PRESENTATION = 112;
	final static int CODE_PLACEMENT = 113;
	final static int CODE_PRODUCT = 114;
	final static int CODE_PLAN = 115;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.purchasely_activity);

		Intent intent = getIntent();

		int code = intent.getIntExtra(EXTRA_ACTION_CODE, -1);
		switch (code) {
			case CODE_PRESENTATION: {
				showPresentationViewForPlacement(intent);
				break;
			}
			case CODE_PLACEMENT: {
				showPresentationViewForPresentation(intent);
				break;
			}
			case CODE_PRODUCT: {
				showPresentationViewForProduct(intent);
				break;
			}
			case CODE_PLAN: {
				showPresentationViewForPlan(intent);
				break;
			}
			default: {
				finish();
				break;
			}
		}
	}

	private void showPresentationViewForPlacement(Intent intent) {
		String placementId = intent.getStringExtra(EXTRA_PLACEMENT_ID);
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);

		if (contentId.isEmpty())
			contentId = null;

		Purchasely.presentationViewForPlacement(this, placementId, contentId,
				contentLoadedCallback(), viewClosedCallback(), productViewResultCallback());
	}

	private void showPresentationViewForPresentation(Intent intent) {
		String presentationId = intent.getStringExtra(EXTRA_PRESENTATION_ID);
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);

		if (contentId.isEmpty())
			contentId = null;

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
				null, presentationId, null, null, contentId, true,
				contentLoadedCallback(), viewClosedCallback());

		Purchasely.presentationView(this, properties, productViewResultCallback());
	}

	private void showPresentationViewForProduct(Intent intent) {
		String productId = intent.getStringExtra(EXTRA_PRODUCT_ID);
		String presentationId = intent.getStringExtra(EXTRA_PRESENTATION_ID);
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);

		if (presentationId.isEmpty())
			presentationId = null;

		if (contentId.isEmpty())
			contentId = null;

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
				null, presentationId, productId, null, contentId, true,
				contentLoadedCallback(), viewClosedCallback());

		Purchasely.presentationView(this, properties, productViewResultCallback());
	}

	private void showPresentationViewForPlan(Intent intent) {
		String planId = intent.getStringExtra(EXTRA_PLAN_ID);
		String presentationId = intent.getStringExtra(EXTRA_PRESENTATION_ID);
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);

		if (presentationId.isEmpty())
			presentationId = null;

		if (contentId.isEmpty())
			contentId = null;

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
				null, presentationId, null, planId, contentId, true,
				contentLoadedCallback(), viewClosedCallback());

		Purchasely.presentationView(this, properties, productViewResultCallback());
	}

	private Function1<Boolean, Unit> contentLoadedCallback() {
		return new Function1<Boolean, Unit>() {
			@Override
			public Unit invoke(Boolean isLoaded) {
				PurchaselyBridge.placementContentProxy.onContentLoaded(isLoaded);
				return null;
			}
		};
	}

	private Function0<Unit> viewClosedCallback() {
		return new Function0<Unit>() {
			@Override
			public Unit invoke() {
				PurchaselyBridge.placementContentProxy.onContentClosed();

				((ViewGroup) findViewById(R.id.content)).removeAllViews();
				supportFinishAfterTransition();
				return null;
			}
		};
	}

	private Function2<PLYProductViewResult, PLYPlan, Unit> productViewResultCallback() {
		return new Function2<PLYProductViewResult, PLYPlan, Unit>() {
			@Override
			public Unit invoke(PLYProductViewResult plyProductViewResult, PLYPlan plyPlan) {
				PurchaselyBridge.placementContentProxy.onPresentationResult(parseProductViewResult(plyProductViewResult), plyPlan);
				return null;
			}
		};
	}

	private int parseProductViewResult(PLYProductViewResult productViewResult) {
		if (productViewResult == PLYProductViewResult.PURCHASED)
			return 0;
		if (productViewResult == PLYProductViewResult.RESTORED)
			return 1;

		return 2;
	}
}
