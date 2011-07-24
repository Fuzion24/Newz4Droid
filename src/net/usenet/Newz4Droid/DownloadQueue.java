package net.usenet.Newz4Droid;

import java.util.LinkedList;

import net.usenet.NetworkInterface.Article;
import net.usenet.NetworkInterface.ServerSettings;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
//TODO: Save download queue even when program exits.  Add clear queue button
public class DownloadQueue extends Activity{
	ArrayAdapter<Article> aa;
	ListView listview;
	private ServerSettings serverSettings;
	Intent svc;
	  public void onCreate(Bundle savedInstanceState) {
		 super.onCreate(savedInstanceState);
		 setContentView(R.layout.downloadqueue);
		 listview = (ListView) findViewById(R.id.listViewDownloadQueue);
		 Button dnloadButton = (Button) findViewById(R.id.btnDownload);
		 Button stopButton = (Button) findViewById(R.id.btnStopDownload);
      	 svc = new Intent(getApplicationContext(),DownloadService.class);
    	 startService(svc);
		   stopButton.setOnClickListener(new View.OnClickListener() {
	             public void onClick(View v) {
				     Toast.makeText(getApplicationContext(), "Stopping Download", Toast.LENGTH_LONG).show();;
				     if(svc != null)
					 {
						stopService(svc);
					 }
	             }
	         });
		  
		   dnloadButton.setOnClickListener(new View.OnClickListener() {
	             public void onClick(View v) {
	            	 getServerSettings();
	            	 if(serverSettings == null)
	            	 {
	            		 Toast.makeText(getApplicationContext(), "Please set your server settings correctly", Toast.LENGTH_LONG).show(); 	
	            		 return;
	            	 }else if(serverSettings.port == -1 || serverSettings.userName == null || serverSettings.hostName == null || serverSettings.passWord == null)
	            	 {
	            		 Toast.makeText(getApplicationContext(), "Please set your server settings correctly", Toast.LENGTH_LONG).show();
	            		 return;
	            	 }else if(DownloadService.downloadQueue.isEmpty())
	            	 {
	            		 Toast.makeText(getApplicationContext(), "              No items in the Download Queue \r\n Add items to the dowload queue from NZB List tab", Toast.LENGTH_LONG).show(); 
	            		 return;
	            	 }else        	 
	            	 {         
		             	 Toast beginDownloadToast = Toast.makeText(getApplicationContext(), "            Beginning Download\r\n Check the Status tab for more info", Toast.LENGTH_LONG); 
		        	     beginDownloadToast.show();						 
		        	     svc = new Intent(getApplicationContext(),DownloadService.class);
		        	     DownloadService.serverSettings = serverSettings;
		        	     startService(svc);		        	     
	            	 }
	            	 
			      
	             }
	         });
		   if (DownloadService.downloadQueue == null) DownloadService.downloadQueue = new LinkedList<Article>();
		   aa = new ArrayAdapter<Article>(this, android.R.layout.simple_list_item_1, DownloadService.downloadQueue);
		   aa.setNotifyOnChange(true);
		   listview.setAdapter(aa);	 
		   registerForContextMenu(listview);
	   	  }
		public void onCreateContextMenu(ContextMenu menu, View v,
				ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);
			menu.setHeaderTitle("Queue Menu");
			menu.add(0, v.getId(), 0, "Delete from Queue");
		} 
		public boolean onContextItemSelected(MenuItem item) {
			
			ContextMenuInfo menuInfo = (ContextMenuInfo) item.getMenuInfo();
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		
			if (item.getTitle() == "Delete from Queue") {
				if(DownloadService.isDownloading)
				{
					  Toast downloadQueueError = Toast.makeText(getApplicationContext(), "Please stop download before removing items from download queue", Toast.LENGTH_LONG); 		        	    
					  downloadQueueError.show();
				}else
				{
					DownloadService.downloadQueue.remove(info.position);
					aa = new ArrayAdapter<Article>(this, android.R.layout.simple_list_item_1, DownloadService.downloadQueue);
					listview.setAdapter(aa);
					aa.setNotifyOnChange(true);
				}
			} 
			return true;
		}
	  protected void onResume ()
	  {
		  super.onResume();
		 aa.notifyDataSetChanged();
	  }
	  protected void onRestart()
	  {
		  super.onResume();
		  aa.notifyDataSetChanged();
	  }

	  void getServerSettings()
		{
			if(serverSettings == null)
			{
				 SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
				
				serverSettings = new ServerSettings();
				serverSettings.hostName = preferences.getString("serverHostname", null);
				serverSettings.userName = preferences.getString("username", null);
				serverSettings.passWord = preferences.getString("password", null);
				serverSettings.requiresSSL =  preferences.getBoolean("useSSL", false);
				serverSettings.connectTimeoutMs = 2000;
				serverSettings.requiresLogin = true;
				try
				{
					serverSettings.port = Integer.parseInt(preferences.getString("portNumber", "N/A"));
				}catch(Exception e)
				{
					serverSettings.port = 119;
				}
				
				
			}
		}
}
