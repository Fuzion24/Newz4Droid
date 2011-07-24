package net.usenet.Newz4Droid;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.widget.Toast;

public class Preferences extends PreferenceActivity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	    Toast.makeText(Preferences.this, "Set your server settings here",  Toast.LENGTH_LONG).show();

	}
	
}