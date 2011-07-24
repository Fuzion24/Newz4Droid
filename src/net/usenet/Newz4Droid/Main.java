package net.usenet.Newz4Droid;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;

public class Main extends TabActivity {
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);

	    Resources res = getResources(); // Resource object to get Drawables
	    TabHost tabHost = getTabHost();  // The activity TabHost
	    TabHost.TabSpec spec;  // Resusable TabSpec for each tab
	    Intent intent;  // Reusable Intent for each tab

	    // Create an Intent to launch an Activity for the tab (to be reused)
	    intent = new Intent().setClass(this, NZBList.class);
	    // Initialize a TabSpec for each tab and add it to the TabHost
	    //TODO: Create a widget with the download Queue info
	    
	    spec = tabHost.newTabSpec("NZBList").setIndicator("NZBList",
	                      res.getDrawable(R.drawable.ic_tab_nzblist))
	                  .setContent(intent);
	    tabHost.addTab(spec);

	    intent = new Intent().setClass(this, DownloadQueue.class);
	    spec = tabHost.newTabSpec("DownloadQueue").setIndicator("Download Queue",
	                      res.getDrawable(R.drawable.ic_tab_downloadqueue))
	                  .setContent(intent);
	    tabHost.addTab(spec);
	     
	    // Do the same for the other tabs
	    intent = new Intent().setClass(this, DownloadStatus.class);	    
	     spec = tabHost.newTabSpec("Status").setIndicator("Status",
	                      res.getDrawable(R.drawable.ic_tab_status)) 
	                  .setContent(intent);
	    tabHost.addTab(spec);
	    tabHost.setCurrentTab(0);
	}
}
