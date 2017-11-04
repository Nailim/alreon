package com.rumblerat.android.anemoi;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class copilotService extends Service{
	/** Used variables */
	
	private NotificationManager mNM;										// For showing and hiding notifications
	
	private final IBinder mBinder = new LocalBinder();						// Service binding object
	
	/** Lifecycle methods */
	/** Called when the service is first created. */
	@Override
	 public void onCreate() {
		mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		showNotification();
	}
	
	@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

	
	/** Called when the service is destroyed. */
	@Override
    public void onDestroy() {
        // Cancel the persistent notification.
        mNM.cancel(R.string.copilot_service);
    }
	
	/** The rest of the lot */
	
    /** When binding to the service, we return an interface to our messenger for sending messages to the service. */
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	/** Class for clients to access. */
	public class LocalBinder extends Binder {
		 copilotService getService() {
			 return copilotService.this;
		 }
	}
	
	/**
     * Show a notification while this service is running.
     */
	private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.copilot_service_started);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification(R.drawable.icon_copilot, text, System.currentTimeMillis());

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MCP.class), 0);

        // Set the info for the views that show in the notification panel.
        notification.setLatestEventInfo(this, getText(R.string.copilot_service), text, contentIntent);

        // Send the notification.
        // We use a layout id because it is a unique number.  We use it later to cancel.
        mNM.notify(R.string.copilot_service, notification);
    }
}