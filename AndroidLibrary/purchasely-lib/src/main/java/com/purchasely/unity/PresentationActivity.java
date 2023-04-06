package com.purchasely.unity;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;

import io.purchasely.ext.PLYPresentation;
import io.purchasely.ext.PLYPresentationViewProperties;
import io.purchasely.ext.PLYProductViewResult;
import io.purchasely.ext.Purchasely;
import io.purchasely.models.PLYPlan;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class PresentationActivity extends AppCompatActivity {
	final static String EXTRA_PLACEMENT_ID = "EXTRA_PLACEMENT_ID";
	final static String EXTRA_CONTENT_ID = "EXTRA_CONTENT_ID";
	final static String EXTRA_PRESENTATION_ID = "EXTRA_PRESENTATION_ID";
	final static String EXTRA_PRODUCT_ID = "EXTRA_PRODUCT_ID";
	final static String EXTRA_PLAN_ID = "EXTRA_PLAN_ID";
	final static String EXTRA_PRESENTATION = "EXTRA_PRESENTATION";

	final static String EXTRA_ACTION_CODE = "EXTRA_ACTION_CODE";

	final static int CODE_PRESENTATION_WITH_VIEW = 111;
	final static int CODE_PRESENTATION = 112;
	final static int CODE_PLACEMENT = 113;
	final static int CODE_PRODUCT = 114;
	final static int CODE_PLAN = 115;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_presentation);

		Intent intent = getIntent();

		int code = intent.getIntExtra(EXTRA_ACTION_CODE, -1);

		String placementId = intent.getStringExtra(EXTRA_PLACEMENT_ID);
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);
		String presentationId = intent.getStringExtra(EXTRA_PRESENTATION_ID);
		String productId = intent.getStringExtra(EXTRA_PRODUCT_ID);
		String planId = intent.getStringExtra(EXTRA_PLAN_ID);

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(
				placementId, presentationId, productId, planId, contentId, true,
				contentLoadedCallback(), viewClosedCallback(), null, null);

		FrameLayout frameLayout = findViewById(R.id.content);
		frameLayout.removeAllViews();

		switch (code) {
			case CODE_PRESENTATION: {
				frameLayout.addView(Purchasely.presentationViewForPlacement(this, placementId, contentId,
						contentLoadedCallback(), viewClosedCallback(), productViewResultCallback()));
				break;
			}
			case CODE_PLACEMENT:
			case CODE_PRODUCT:
			case CODE_PLAN: {
				frameLayout.addView(Purchasely.presentationView(this, properties, productViewResultCallback()));
				break;
			}
			case CODE_PRESENTATION_WITH_VIEW:
				PLYPresentation presentation = intent.getParcelableExtra(EXTRA_PRESENTATION);
				if (presentation == null) {
					finish();
					return;
				}

				frameLayout.addView(presentation.buildView(this, properties,
						productViewResultCallback()));
				break;
			default: {
				finish();
				break;
			}
		}

		PurchaselyBridge.PresentationActivityCache cache = new PurchaselyBridge.PresentationActivityCache();

		cache.presentationId = presentationId;
		cache.placementId = placementId;
		cache.productId = productId;
		cache.planId = planId;
		cache.contentId = contentId;

		cache.actionCode = code;

		cache.activity = new WeakReference<>(this);

		PurchaselyBridge.presentationActivityCache = cache;
	}

	private Function2<PLYProductViewResult, PLYPlan, Unit> productViewResultCallback() {
		return (plyProductViewResult, plyPlan) -> {
			PurchaselyBridge.placementContentProxy.onPresentationResult(
					Utils.parseProductViewResult(plyProductViewResult), Utils.serializePlan(plyPlan));
			return null;
		};
	}

	private Function1<Boolean, Unit> contentLoadedCallback() {
		return isLoaded -> {
			PurchaselyBridge.placementContentProxy.onContentLoaded(isLoaded);

			return null;
		};
	}

	private Function0<Unit> viewClosedCallback() {
		return () -> {
			PurchaselyBridge.placementContentProxy.onContentClosed();

			((ViewGroup) findViewById(R.id.content)).removeAllViews();
			supportFinishAfterTransition();
			return null;
		};
	}

	@Override
	protected void onDestroy() {
		PurchaselyBridge.presentationActivityCache.activity = null;
		super.onDestroy();
	}
}
