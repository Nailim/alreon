package com.rumblerat.android.anemoi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MCP extends Activity {
	/** Used variables */
	
	private Messenger mBoundAutopilotService;

	private copilotService mBoundCopilotService;
	
	private Boolean preFlighPrep = false;
	
	private Button xButton;
	
	private final Messenger mMessenger = new Messenger(new IncomingHandler());

	/**
	 * Handler of incoming messages from service.
	 */
	class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	            case autopilotService.MSG_SET_VALUE:
	                //mCallbackText.setText("Received from service: " + msg.arg1);
	                break;
	            default:
	                super.handleMessage(msg);
	        }
	    }
	}

	
	/** Lifecycle methods */
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.flight);
        
        xButton = (Button)findViewById(R.id.start);
		xButton.setOnClickListener(mStart);
		
		xButton = (Button)findViewById(R.id.stop);
		xButton.setOnClickListener(mStop);
    }
    
    @Override
	protected void onResume() {
		super.onResume();
		bindService(new Intent(MCP.this, autopilotService.class), mAutopilotConnection, Context.BIND_AUTO_CREATE);

		bindService(new Intent(MCP.this, copilotService.class), mCopilotConnection, Context.BIND_AUTO_CREATE );
	}
    
	@Override
	protected void onPause() {
		super.onPause();
		unbindService(mAutopilotConnection);
		unbindService(mCopilotConnection);
	}
	
//	@Override
//	protected void onStop(){
//		super.onStop();
//	}
//	/** Called when the service is destroyed. */
//	@Override
//	public void onDestroy() {
//	}
	
	/** The rest of the lot */
	
	/** Copilot service connection */
	private ServiceConnection mCopilotConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundCopilotService = ((copilotService.LocalBinder)service).getService();
		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundCopilotService = null;
		}
	};
	
	/** Autopilot service connection */
	private ServiceConnection mAutopilotConnection = new ServiceConnection() {
	    public void onServiceConnected(ComponentName className, IBinder service) {
	        // This is called when the connection with the service has been
	        // established, giving us the service object we can use to
	        // interact with the service.  We are communicating with our
	        // service through an IDL interface, so get a client-side
	        // representation of that from the raw service object.
	        mBoundAutopilotService = new Messenger(service);
	        //mCallbackText.setText("Attached.");

	        // We want to monitor the service for as long as we are
	        // connected to it.
	        try {
	            Message msg = Message.obtain(null, autopilotService.MSG_REGISTER_CLIENT);
	            msg.replyTo = mMessenger;
	            mBoundAutopilotService.send(msg);

	            // Give it some value as an example.
	            msg = Message.obtain(null, autopilotService.MSG_SET_VALUE, this.hashCode(), 0);
	            mBoundAutopilotService.send(msg);
	        } catch (RemoteException e) {
	            // In this case the service has crashed before we could even
	            // do anything with it; we can count on soon being
	            // disconnected (and then reconnected if it can be restarted)
	            // so there is no need to do anything here.
	        }
	    }

	    public void onServiceDisconnected(ComponentName className) {
	        // This is called when the connection with the service has been
	        // unexpectedly disconnected -- that is, its process crashed.
	        mBoundAutopilotService = null;
	        //mCallbackText.setText("Disconnected.");
	    }
	};
	
	private OnClickListener mStart = new OnClickListener() {
		public void onClick(View v) {

			//if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
				startService(new Intent(MCP.this, autopilotService.class));
				startService(new Intent(MCP.this, copilotService.class));
			//
			//	if (!mBoundLocalService.isCameraActive()) {
			//		mBoundLocalService.giveView(sv, spinnerAutofocus.getSelectedItemPosition(), spinnerResolution.getSelectedItemPosition(), resWidth, resHeight, spinnerType.getSelectedItemPosition(), lpsTime,
			//									mYearStart, mMonthStart, mDayStart, mHourStart, mMinuteStart, mYearEnd, mMonthEnd, mDayEnd, mHourEnd, mMinuteEnd);
			//	}
			//
			//	xButton = (Button)findViewById(R.id.grabCamera);
			//	xButton.setVisibility(View.GONE);
			//	xButton = (Button)findViewById(R.id.releaseCamera);
			//	xButton.setVisibility(View.VISIBLE);
			//	xButton = (Button)findViewById(R.id.startCamera);
			//	xButton.setVisibility(View.VISIBLE);
			//	xButton = (Button)findViewById(R.id.stopCamera);
			//	xButton.setVisibility(View.GONE);
			//
			//	xView.setVisibility(View.GONE);
			//
			//}
			//else {
			//	DisplayToast(getString(R.string.error_sdCard));
			//}
		}
	};
	
	private OnClickListener mStop = new OnClickListener() {
		public void onClick(View v) {
			stopService(new Intent(MCP.this, autopilotService.class));
			stopService(new Intent(MCP.this, copilotService.class));
		}
	};

}