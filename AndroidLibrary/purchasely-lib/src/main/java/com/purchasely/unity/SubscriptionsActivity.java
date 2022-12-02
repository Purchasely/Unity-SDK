package com.purchasely.unity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.util.Log;

import io.purchasely.ext.Purchasely;

public class SubscriptionsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_subscriptions);

		String packageName = getApplication().getPackageName();

		Fragment fragment = Purchasely.subscriptionsFragment();
		if (fragment == null) {
			Log.d("SubscriptionsActivity", "Subscriptions fragment is null. Will not show.");
			finish();
			return;
		}

		int identifier = getApplication().getResources().getIdentifier("fragmentContainer", "id", packageName);

		getSupportFragmentManager()
				.beginTransaction()
				.addToBackStack(null)
				.replace(identifier, fragment, "SubscriptionsFragment")
				.commitAllowingStateLoss();

		getSupportFragmentManager().addOnBackStackChangedListener(() -> {
			if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
				supportFinishAfterTransition();
			}
		});
	}
}