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
	final static String EXTRA_SHOW_CLOSE_BUTTON = "EXTRA_SHOW_CLOSE_BUTTON";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.purchasely_activity);

		Intent intent = getIntent();
		String contentId = intent.getStringExtra(EXTRA_CONTENT_ID);
		String placementId = intent.getStringExtra(EXTRA_PLACEMENT_ID);
		boolean displayCloseButton = intent.getBooleanExtra(EXTRA_SHOW_CLOSE_BUTTON, false);

		if (contentId.isEmpty())
			contentId = null;

		PLYPresentationViewProperties properties = new PLYPresentationViewProperties(placementId, null, null, null, contentId, displayCloseButton, new Function1<Boolean, Unit>() {
			@Override
			public Unit invoke(Boolean isLoaded) {
				PurchaselyBridge.placementContentProxy.onContentLoaded(isLoaded);
				return null;
			}
		}, new Function0<Unit>() {
			@Override
			public Unit invoke() {
				PurchaselyBridge.placementContentProxy.onContentClosed();

				((ViewGroup) findViewById(R.id.content)).removeAllViews();
				supportFinishAfterTransition();
				return null;
			}
		});

		Purchasely.presentationView(this, properties, new Function2<PLYProductViewResult, PLYPlan, Unit>() {
			@Override
			public Unit invoke(PLYProductViewResult plyProductViewResult, PLYPlan plyPlan) {
				PurchaselyBridge.placementContentProxy.onPresentationResult(parseProductViewResult(plyProductViewResult), plyPlan);
				return null;
			}
		});

		((ViewGroup) findViewById(R.id.content)).removeAllViews();
	}

	private int parseProductViewResult(PLYProductViewResult productViewResult) {
		if (productViewResult == PLYProductViewResult.PURCHASED)
			return 0;
		if (productViewResult == PLYProductViewResult.RESTORED)
			return 1;

		return 2;
	}
}
