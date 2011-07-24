package net.usenet.Newz4Droid;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import net.usenet.NetworkInterface.Article;
import net.usenet.NetworkInterface.NNTPclient;
import net.usenet.NetworkInterface.NNTPclient.NNTPException;
import net.usenet.NetworkInterface.Segment;
import net.usenet.NetworkInterface.ServerSettings;
import net.usenet.yEnc.YEncDecoder;
import net.usenet.yEnc.YEncException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.widget.Toast;

public class DownloadService extends Service {
	private static Context DOWNLOAD_STATUS_CONTEXT;
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	private Boolean stopDownload = false;
	public static boolean isDownloading = false;
	private boolean errorDownloading = false;
	private File extStorageDir;
	public static uiData mUIdata;
	public static ServerSettings serverSettings;
	public static List<Article> downloadQueue;
	private NotificationManager mNotificationManager;
	private static final int HELLO_ID = 1;
	private String DOWNLOAD_PATH;
	
	Thread t;

	@Override
	public IBinder onBind(Intent arg0) {
		// This is used for interprocess communication, we don't need it. return
		// null
		return null;
	}

	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER = l;
	}

	@Override
	public void onCreate() {
		super.onCreate();

				getDownloadQueue();
				
		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		extStorageDir = Environment.getExternalStorageDirectory();
		DOWNLOAD_PATH = extStorageDir.getPath() +  "/Download/";
		
		
		mUIdata = new uiData();
		if(!isDownloading)
		{
			startDownload();
		}
		if (DOWNLOAD_STATUS_CONTEXT != null)
			Toast.makeText(DOWNLOAD_STATUS_CONTEXT, "MyService stopped",
					Toast.LENGTH_SHORT);
	}
	public static void setContext(Context context) {
		DOWNLOAD_STATUS_CONTEXT = context;
	}
	private void startDownload()
	{
		if (!isDownloading) {
			t = new Thread(new Runnable() {
				public void run() {
					downloadQueue();
				}
			});

			t.start();
		}
	}
	private void downloadQueue() {
		isDownloading = true;
		File downloadDirectory;
		
		downloadDirectory = new File(DOWNLOAD_PATH);
		downloadDirectory.mkdir();
		downloadDirectory = new File(DOWNLOAD_PATH + "Temp/");
		downloadDirectory.mkdir();
		while (!downloadQueue.isEmpty()) {
			Article file = downloadQueue.get(0);
			file.calcFileSize();

			for (Segment seg : file.Segments) {
				
				
				if (stopDownload) {
					stopDownload(file);
					return;
				}else if(errorDownloading)
				{
					return;
				}
				downloadSeg(seg);
			} // End Segments Download

			File fo = null;
			FileOutputStream fso = null;
			FileInputStream segmentStream;
			boolean fileCorrupted = false;
			int lastIndex = 0;
			int currentIndex = 0;
			byte buffer[] = new byte[2048];
			int bytesRead = 0;
			try {
				fo = new File(DOWNLOAD_PATH + file.fileName);				
				fso = new FileOutputStream(fo);
				
				for (Segment seg : file.Segments) {
					if (!seg.messedUp) {
						currentIndex = seg.partBegin - 1;
						
						if (lastIndex != currentIndex) {
							final int zerosToWrite = currentIndex - lastIndex;
							for (int i = 0; i < zerosToWrite; i++)
								fso.write(0);
						}
						// write segment to file
						File segmentFile = new File(DOWNLOAD_PATH + "Temp/" + seg.msgID);
						segmentStream = new FileInputStream(segmentFile);
						
						while((bytesRead = segmentStream.read(buffer)) > 0)
						{
							fso.write(buffer,0,bytesRead);							
						}
							segmentStream.close();
							segmentFile.delete();
							lastIndex = seg.partEnd;
					}else
					{
						fileCorrupted = true;
					}
				}
				fo = new File(DOWNLOAD_PATH + file.fileName);				
				fso = new FileOutputStream(fo); 
				
				//All data is written to file here. 

				fso.close();
				
				if (fileCorrupted) {	
					File renameFile = new File(DOWNLOAD_PATH + "1" + file.fileName.toUpperCase());
					fo.renameTo(renameFile);
					renameFile.renameTo(new File(DOWNLOAD_PATH + file.fileName.toUpperCase()));
					}

				downloadQueue.remove(0);
				saveDownloadQueue();
				
			} catch (Exception e) {
				mUIdata.statusText = "Error Saving file: ";
				mUIdata.progressText = e.toString();
				if (UI_UPDATE_LISTENER != null) {
					UI_UPDATE_LISTENER.updateUI(mUIdata);
				}
			}
		}	
				finishedDownload();

		/*	} catch (Exception e) {
				final String errorString = e.toString();

				mUIdata.statusText = "Error Downloading: ";
				mUIdata.progressText = errorString;
				if (UI_UPDATE_LISTENER != null) {
					UI_UPDATE_LISTENER.updateUI(mUIdata);
				}
				System.out.print("Error in downloading: " + e.toString());

				int icon = R.drawable.nzb;
				CharSequence tickerText = "Error Downloading";
				long when = System.currentTimeMillis();
				Notification notification = new Notification(icon, tickerText,
						when);
				Context context = getApplicationContext();
				CharSequence contentTitle = "Error Downloading";
				CharSequence contentText = "Please see the Status tab for more info";
				Intent notificationIntent = new Intent(this, Main.class);
				PendingIntent contentIntent = PendingIntent.getActivity(this,
						0, notificationIntent, 0);
				notification.setLatestEventInfo(context, contentTitle,
						contentText, contentIntent);
				mNotificationManager.notify(HELLO_ID, notification);
			} finally {

				serverConnection.disconnect();
				this.onDestroy();
				downloading = false;
			}*/
	}

	private void downloadSeg(Segment seg)
	{
		try {
			NNTPclient serverConnection = null;
			serverConnection = new NNTPclient(serverSettings);
			
			ByteArrayOutputStream oStream;
			YEncDecoder decoder = new YEncDecoder();
			
			try{
				serverConnection.connect();
			}catch(NNTPException nntpException)
			{
				mUIdata.statusText = "Error Connecting: ";
				mUIdata.progressText = nntpException.toString();
				if (UI_UPDATE_LISTENER != null) {
					UI_UPDATE_LISTENER.updateUI(mUIdata);
				}
				errorDownloading = true;
				return;
			}
			oStream = serverConnection.DownloadBody(seg.msgID);

			decoder.setInputStream(new ByteArrayInputStream(oStream
					.toByteArray()));
			long t0 = System.currentTimeMillis();
			seg.partBegin = decoder.getPartBegin();
			seg.partEnd = decoder.getPartEnd();
			seg.partNum = decoder.getPartNumber();
			seg.fileName = decoder.getFileName();
			File outputFile = new File(DOWNLOAD_PATH + "Temp/" + seg.msgID);
			outputFile.createNewFile();
			FileOutputStream fsoDecodedSegment = new FileOutputStream(outputFile);
			oStream.reset();
			//seg.decodedStream = new ByteArrayOutputStream();
			decoder.setOutputStream(oStream);
			decoder.decode();
			oStream.writeTo(fsoDecodedSegment);
			oStream.reset();
			fsoDecodedSegment.close();
			seg.parentFile.fileName = seg.fileName;
			long t1 = System.currentTimeMillis();
			t1 = t1 - t0;
			double sDecodeTime = t1;
			updateStatus(sDecodeTime, seg.parentFile, seg, serverConnection);

		} catch (NNTPException nntpException) {
			// Catch 430 Article doesn't exist and fill the file with
			// 0x00
			if (nntpException.getCode() == 430) {
				seg.decodedStream = null;
				seg.messedUp = true;

			}else
			{	
				seg.decodedStream = null;
				seg.messedUp = true;
				mUIdata.statusText = "Error Downloading: ";
				mUIdata.progressText = nntpException.toString();
				if (UI_UPDATE_LISTENER != null) {
					UI_UPDATE_LISTENER.updateUI(mUIdata);
				}
			}
			
		} catch (YEncException yencException) {
			seg.decodedStream = null;
			seg.messedUp = true;
			mUIdata.statusText = "Error Downloading: ";
			mUIdata.progressText = yencException.toString();
			if (UI_UPDATE_LISTENER != null) {
				UI_UPDATE_LISTENER.updateUI(mUIdata);
			}
		}catch(IOException e)
		{	seg.decodedStream = null;
			seg.messedUp = true;
			mUIdata.statusText = "Error Downloading: ";
			mUIdata.progressText = e.toString();
			if (UI_UPDATE_LISTENER != null) {
				UI_UPDATE_LISTENER.updateUI(mUIdata);
			}
			return;
		}

	
	}
	private void updateStatus(double sDecodeTime, Article file, Segment seg,
			NNTPclient serverConnection) {
		final double percentComplete = file.updatePerectComplete(seg.size);
		final int fileSize = (int) (file.fileSize / 1000);
		final String fileName = seg.fileName;
		final int KBperSec = serverConnection.KBperSec;
		final int amountDownloaded = (int) (file.totalDownloaded / 1024);
		// TODO: do a better job displaying KB vs Bytes vs Megabytes

		mUIdata.percentComplete = percentComplete;
		mUIdata.progressText = "File: " + fileName + "\r\n" + "Speed:  "
				+ KBperSec + "KB/sec\r\n" + "Progress: "
				+ String.format("%.2f", percentComplete) + "%\r\n"
				+ "Downloaded: " + amountDownloaded + " KBytes Of " + fileSize
				+ " KBytes";

		if (UI_UPDATE_LISTENER != null) {
			UI_UPDATE_LISTENER.updateUI(mUIdata);
		}

		int icon = R.drawable.nzb;
		CharSequence tickerText = "Downloading";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		Context context = getApplicationContext();
		CharSequence contentTitle = "Downloading: "
				+ String.format("%.2f", percentComplete) + "% at " + KBperSec
				+ "KB/sec";
		CharSequence contentText = fileName;
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		mNotificationManager.notify(HELLO_ID, notification);
	}

	private void finishedDownload() {
		mUIdata.statusText = "Finished Downloading: ";
		if (UI_UPDATE_LISTENER != null) {
			UI_UPDATE_LISTENER.updateUI(mUIdata);
		}

		int icon = R.drawable.nzb;
		CharSequence tickerText = "Finished Downloading";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		Context context = getApplicationContext();
		CharSequence contentTitle = "Download Stopped";
		CharSequence contentText = "All downloads finished successfully";
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		mNotificationManager.notify(HELLO_ID, notification);
	}

	private void stopDownload(Article file) {
		file.percentComplete = 0;
		file.totalDownloaded = 0;

		mUIdata.statusText = "Stopped";
		mUIdata.progressText = "";
		mUIdata.percentComplete = 0;
		if (UI_UPDATE_LISTENER != null) {
			UI_UPDATE_LISTENER.updateUI(mUIdata);
		}

		int icon = R.drawable.nzb;
		CharSequence tickerText = "Download Stopped";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(icon, tickerText, when);
		Context context = getApplicationContext();
		CharSequence contentTitle = "Download Stopped";
		CharSequence contentText = "Download canceled by user";
		Intent notificationIntent = new Intent(this, Main.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		mNotificationManager.notify(HELLO_ID, notification);
		isDownloading = false;
		this.onDestroy();
	}
	
	@Override
	public void onDestroy() {
		stopDownload = true;
		saveDownloadQueue();
		super.onDestroy();

	}
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	startDownload();
        return START_STICKY;
    }
	private void saveDownloadQueue()
	{
		try {
			FileOutputStream fos = openFileOutput(serverSettings.DOWNLOAD_QUEUE_FILE, Context.MODE_PRIVATE);
		      ObjectOutputStream oos = new ObjectOutputStream(fos);
		      oos.writeObject(downloadQueue);
		      oos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void getDownloadQueue()
	{
		try {
			FileInputStream fis = openFileInput(serverSettings.DOWNLOAD_QUEUE_FILE);
		    ObjectInputStream ois = new ObjectInputStream(fis);
		    downloadQueue = (List<Article>) ois.readObject();
		    ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
