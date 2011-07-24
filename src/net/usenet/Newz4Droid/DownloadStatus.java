package net.usenet.Newz4Droid;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadStatus extends Activity{
	
	private ProgressBar progressBar;
	private TextView progressText;
	private TextView statusText;
	private DownloadStatus mDownloadStatus;
	
	   public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.downloadstatus);	
			progressText = (TextView) findViewById(R.id.downloadProgress);
			statusText = (TextView) findViewById(R.id.statusText);
			progressBar = (ProgressBar) findViewById(R.id.progressbar);
			//progressBar.setVisibility(View.INVISIBLE);
			progressBar.setProgress(0);
        	statusText.setText("Status: ");
        	mDownloadStatus = this;
        	
	       if(DownloadService.mUIdata != null)
	       {
	       	progressText.setText(DownloadService.mUIdata.progressText);
	    	statusText.setText(DownloadService.mUIdata.statusText);
	    	progressBar.setProgress((int)DownloadService.mUIdata.percentComplete);
	       }
	       
			DownloadService.setContext(getApplicationContext());
	        DownloadService.setUpdateListener(new ServiceUpdateUIListener() {
	            public void updateUI(final uiData Data) {
	              // make sure this runs in the UI thread... since it's messing with views...	            	
	            	mDownloadStatus.runOnUiThread(	          
	                  new Runnable() {
	                    public void run() {
	                    	progressText.setText(Data.progressText);
	                    	statusText.setText(Data.statusText);
	                    	progressBar.setProgress((int)Data.percentComplete);
	                    }
	                  });
	            }
	          });
	    }
	   protected void onResume()
	   {
		   super.onResume();
	       if(DownloadService.mUIdata != null)
	       {
	       	progressText.setText(DownloadService.mUIdata.progressText);
	    	statusText.setText(DownloadService.mUIdata.statusText);
	    	progressBar.setProgress((int)DownloadService.mUIdata.percentComplete);
	       }
	   }
}
