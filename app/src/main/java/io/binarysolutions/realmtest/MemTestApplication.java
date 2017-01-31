package io.binarysolutions.realmtest;

import android.app.Application;

import io.realm.Realm;

/**
 * Base application to initialize realm
 * Created by Craig on 12/20/2016.
 */

public class MemTestApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

		Realm.init(this);
	}
}
