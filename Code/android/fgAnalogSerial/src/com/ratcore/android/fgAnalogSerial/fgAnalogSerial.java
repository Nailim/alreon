package com.ratcore.android.fgAnalogSerial;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.ToggleButton;

public class fgAnalogSerial extends Activity implements SeekBar.OnSeekBarChangeListener {
	
	// Power management
	protected PowerManager.WakeLock mWakeLock = null;
	
	// Esthetic progress bar
	private ProgressDialog myProgressDialog = null;
	
	// Sound stream requirements
	private static final Integer minBufferSize = android.media.AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_8BIT);
	private final AudioTrack oTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 48000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_8BIT, minBufferSize, AudioTrack.MODE_STREAM);
	
	// Sound buffer stuff
	private static final Integer sampleLenght = 55;
	
	//private byte[] sndBufferSample = new byte[sampleLenght];
	private byte[] sndBufferTrack = new byte[minBufferSize];
	
	private byte[] sndSample = new byte[sampleLenght];
	private byte[][] sndSamples = new byte[256][55];
	
	// Gear variables
	private float gearThrottle = 0;
	private float gearElevator = (float) 0.5;
	private float gearAileron = (float) 0.5;
	private float gearRudder = (float) 0;
	
	// "Serial" buffer queue
	//private Queue<Character> sQueue = new LinkedList<Character>();
	private BlockingQueue<Character> sQueue = new LinkedBlockingQueue<Character>();
	
	// Sensors manager
	private SensorManager mSensorManager = null;
	private SensorEventListener mSensorEventListener = new SensorEventListener() {

		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		public void onSensorChanged(SensorEvent e) {
			synchronized (this) {
			if (e.values[1] > 90) {
				gearAileron = (90 / -90);
			} else if (e.values[1] < -90) {
				gearAileron = (-90 / -90);
			} else {
				gearAileron = (e.values[1] / -90);
			}
			gearElevator = (e.values[2] / -90);
			}
		}

	};
	
	// Loop control
	private Boolean sndLoop = false;
	
	// UI stuff
	private ToggleButton tbtnConnection = null;
	private Button btnRudderReset = null;
	private SeekBar sbRudder = null;
	private SeekBar sbThrottle = null;
	
	private final static int idSeekBarRudder = 0xA1;
	private final static int idSeekBarThrottle = 0xA2;
	
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		mainSetup();
	}
	
	@Override
	 protected void onResume() {
		 super.onResume();
		 //mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
	}
	    
	 @Override
	 protected void onStop() {
		 mSensorManager.unregisterListener(mSensorEventListener);
		 super.onStop();
	 }
   

	@Override
	public void onDestroy() {		
		// Releasing power management control
		this.mWakeLock.release();
		
		// Stopping and releasing sound access
		this.oTrack.stop();
		this.oTrack.release();
		
		super.onDestroy();
	}
	
	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
		switch (arg0.getId()) {
		case idSeekBarRudder:
			gearRudder = (float) ((float) (arg1 - 50) / 50);
			break;
		case idSeekBarThrottle:
			gearThrottle = (float) ((float) arg1 / 100);
			break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}
	
	// Esthetic progress bar startup
	private void mainSetup() {
		myProgressDialog = ProgressDialog.show(fgAnalogSerial.this, "Please wait ...", "Setting up ...", true);
		
		Thread mainSetupThread = new Thread(null, doMainSetup, "mainSetup");
		mainSetupThread.start();
	}
	
	// Starting thread to do the initialization in the background
	private Runnable doMainSetup = new Runnable() {
		public void run() {
			backgroundMianSetup();
			
			// Fake work :P
			try {
				Thread.sleep(1000);
			} catch (Exception e) { }
			
			myProgressDialog.dismiss();
		}
	};
	
	// Doing the actual initialization
	private void backgroundMianSetup() {
		// Setting volume control hook
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		
		// Preventing device from sleeping
		final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		this.mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "fgAnalogSerial");
		this.mWakeLock.acquire();
		
		// Acquiring sensors handle
		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
		
		// Connect UI elements
		sbRudder = (SeekBar) findViewById(R.id.sbRudder);
		sbThrottle = (SeekBar) findViewById(R.id.sbThrottle);
		
		sbRudder.setId(idSeekBarRudder);
		sbThrottle.setId(idSeekBarThrottle);
		
		sbRudder.setOnSeekBarChangeListener(this);
		sbThrottle.setOnSeekBarChangeListener(this);
		
		btnRudderReset = (Button) findViewById(R.id.btnRudderReset);
		btnRudderReset.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				sbRudder.setProgress(sbRudder.getMax()/2);
				//gearRudder = (50/100);
			}
		});
		
		tbtnConnection = (ToggleButton) findViewById(R.id.tbtnConnection);
		tbtnConnection.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				Toast.makeText(getBaseContext(), "ON/OFF button test", Toast.LENGTH_SHORT).show();
//				Toast.makeText(getBaseContext(), Integer.toString((int) '@'), Toast.LENGTH_SHORT).show();
				
				if (sndLoop) {
					sndLoop = false;
				} else {
					sndLoop = true;
					
					Thread sndModulationThread = new Thread(null, doSoundModulation, "soundModulation");
					sndModulationThread.start();
					
					Thread dataModulationThread = new Thread(null, doDataModulation, "DataModulation");
					//dataModulationThread.start();
				}
			}
		});

		// Load character sound samples in to memory
		InputStream inputStream;
		
		try {
			inputStream = getResources().openRawResource(R.raw.snd);
			inputStream.read(sndSample);
			inputStream.close();
			
			inputStream = getResources().openRawResource(R.raw.snd00000000);
			inputStream.read(sndSamples[0]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000001);
			inputStream.read(sndSamples[1]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000010);
			inputStream.read(sndSamples[2]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000011);
			inputStream.read(sndSamples[3]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000100);
			inputStream.read(sndSamples[4]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000101);
			inputStream.read(sndSamples[5]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000110);
			inputStream.read(sndSamples[6]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00000111);
			inputStream.read(sndSamples[7]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001000);
			inputStream.read(sndSamples[8]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001001);
			inputStream.read(sndSamples[9]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001010);
			inputStream.read(sndSamples[10]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001011);
			inputStream.read(sndSamples[11]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001100);
			inputStream.read(sndSamples[12]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001101);
			inputStream.read(sndSamples[13]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001110);
			inputStream.read(sndSamples[14]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00001111);
			inputStream.read(sndSamples[15]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010000);
			inputStream.read(sndSamples[16]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010001);
			inputStream.read(sndSamples[17]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010010);
			inputStream.read(sndSamples[18]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010011);
			inputStream.read(sndSamples[19]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010100);
			inputStream.read(sndSamples[20]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010101);
			inputStream.read(sndSamples[21]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010110);
			inputStream.read(sndSamples[22]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00010111);
			inputStream.read(sndSamples[23]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011000);
			inputStream.read(sndSamples[24]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011001);
			inputStream.read(sndSamples[25]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011010);
			inputStream.read(sndSamples[26]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011011);
			inputStream.read(sndSamples[27]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011100);
			inputStream.read(sndSamples[28]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011101);
			inputStream.read(sndSamples[29]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011110);
			inputStream.read(sndSamples[30]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00011111);
			inputStream.read(sndSamples[31]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100000);
			inputStream.read(sndSamples[32]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100001);
			inputStream.read(sndSamples[33]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100010);
			inputStream.read(sndSamples[34]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100011);
			inputStream.read(sndSamples[35]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100100);
			inputStream.read(sndSamples[36]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100101);
			inputStream.read(sndSamples[37]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100110);
			inputStream.read(sndSamples[38]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00100111);
			inputStream.read(sndSamples[39]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101000);
			inputStream.read(sndSamples[40]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101001);
			inputStream.read(sndSamples[41]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101010);
			inputStream.read(sndSamples[42]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101011);
			inputStream.read(sndSamples[43]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101100);
			inputStream.read(sndSamples[44]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101101);
			inputStream.read(sndSamples[45]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101110);
			inputStream.read(sndSamples[46]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00101111);
			inputStream.read(sndSamples[47]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110000);
			inputStream.read(sndSamples[48]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110001);
			inputStream.read(sndSamples[49]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110010);
			inputStream.read(sndSamples[50]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110011);
			inputStream.read(sndSamples[51]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110100);
			inputStream.read(sndSamples[52]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110101);
			inputStream.read(sndSamples[53]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110110);
			inputStream.read(sndSamples[54]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00110111);
			inputStream.read(sndSamples[55]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111000);
			inputStream.read(sndSamples[56]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111001);
			inputStream.read(sndSamples[57]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111010);
			inputStream.read(sndSamples[58]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111011);
			inputStream.read(sndSamples[59]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111100);
			inputStream.read(sndSamples[60]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111101);
			inputStream.read(sndSamples[61]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111110);
			inputStream.read(sndSamples[62]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd00111111);
			inputStream.read(sndSamples[63]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000000);
			inputStream.read(sndSamples[64]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000001);
			inputStream.read(sndSamples[65]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000010);
			inputStream.read(sndSamples[66]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000011);
			inputStream.read(sndSamples[67]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000100);
			inputStream.read(sndSamples[68]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000101);
			inputStream.read(sndSamples[69]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000110);
			inputStream.read(sndSamples[70]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01000111);
			inputStream.read(sndSamples[71]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001000);
			inputStream.read(sndSamples[72]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001001);
			inputStream.read(sndSamples[73]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001010);
			inputStream.read(sndSamples[74]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001011);
			inputStream.read(sndSamples[75]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001100);
			inputStream.read(sndSamples[76]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001101);
			inputStream.read(sndSamples[77]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001110);
			inputStream.read(sndSamples[78]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01001111);
			inputStream.read(sndSamples[79]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010000);
			inputStream.read(sndSamples[80]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010001);
			inputStream.read(sndSamples[81]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010010);
			inputStream.read(sndSamples[82]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010011);
			inputStream.read(sndSamples[83]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010100);
			inputStream.read(sndSamples[84]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010101);
			inputStream.read(sndSamples[85]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010110);
			inputStream.read(sndSamples[86]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01010111);
			inputStream.read(sndSamples[87]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011000);
			inputStream.read(sndSamples[88]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011001);
			inputStream.read(sndSamples[89]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011010);
			inputStream.read(sndSamples[90]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011011);
			inputStream.read(sndSamples[91]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011100);
			inputStream.read(sndSamples[92]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011101);
			inputStream.read(sndSamples[93]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011110);
			inputStream.read(sndSamples[94]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01011111);
			inputStream.read(sndSamples[95]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100000);
			inputStream.read(sndSamples[96]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100001);
			inputStream.read(sndSamples[97]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100010);
			inputStream.read(sndSamples[98]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100011);
			inputStream.read(sndSamples[99]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100100);
			inputStream.read(sndSamples[100]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100101);
			inputStream.read(sndSamples[101]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100110);
			inputStream.read(sndSamples[102]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01100111);
			inputStream.read(sndSamples[103]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101000);
			inputStream.read(sndSamples[104]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101001);
			inputStream.read(sndSamples[105]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101010);
			inputStream.read(sndSamples[106]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101011);
			inputStream.read(sndSamples[107]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101100);
			inputStream.read(sndSamples[108]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101101);
			inputStream.read(sndSamples[109]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101110);
			inputStream.read(sndSamples[110]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01101111);
			inputStream.read(sndSamples[111]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110000);
			inputStream.read(sndSamples[112]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110001);
			inputStream.read(sndSamples[113]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110010);
			inputStream.read(sndSamples[114]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110011);
			inputStream.read(sndSamples[115]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110100);
			inputStream.read(sndSamples[116]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110101);
			inputStream.read(sndSamples[117]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110110);
			inputStream.read(sndSamples[118]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01110111);
			inputStream.read(sndSamples[119]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111000);
			inputStream.read(sndSamples[120]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111001);
			inputStream.read(sndSamples[121]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111010);
			inputStream.read(sndSamples[122]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111011);
			inputStream.read(sndSamples[123]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111100);
			inputStream.read(sndSamples[124]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111101);
			inputStream.read(sndSamples[125]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111110);
			inputStream.read(sndSamples[126]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd01111111);
			inputStream.read(sndSamples[127]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000000);
			inputStream.read(sndSamples[128]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000001);
			inputStream.read(sndSamples[129]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000010);
			inputStream.read(sndSamples[130]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000011);
			inputStream.read(sndSamples[131]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000100);
			inputStream.read(sndSamples[132]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000101);
			inputStream.read(sndSamples[133]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000110);
			inputStream.read(sndSamples[134]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10000111);
			inputStream.read(sndSamples[135]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001000);
			inputStream.read(sndSamples[136]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001001);
			inputStream.read(sndSamples[137]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001010);
			inputStream.read(sndSamples[138]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001011);
			inputStream.read(sndSamples[139]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001100);
			inputStream.read(sndSamples[140]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001101);
			inputStream.read(sndSamples[141]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001110);
			inputStream.read(sndSamples[142]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10001111);
			inputStream.read(sndSamples[143]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010000);
			inputStream.read(sndSamples[144]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010001);
			inputStream.read(sndSamples[145]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010010);
			inputStream.read(sndSamples[146]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010011);
			inputStream.read(sndSamples[147]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010100);
			inputStream.read(sndSamples[148]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010101);
			inputStream.read(sndSamples[149]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010110);
			inputStream.read(sndSamples[150]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10010111);
			inputStream.read(sndSamples[151]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011000);
			inputStream.read(sndSamples[152]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011001);
			inputStream.read(sndSamples[153]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011010);
			inputStream.read(sndSamples[154]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011011);
			inputStream.read(sndSamples[155]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011100);
			inputStream.read(sndSamples[156]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011101);
			inputStream.read(sndSamples[157]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011110);
			inputStream.read(sndSamples[158]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10011111);
			inputStream.read(sndSamples[159]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100000);
			inputStream.read(sndSamples[160]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100001);
			inputStream.read(sndSamples[161]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100010);
			inputStream.read(sndSamples[162]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100011);
			inputStream.read(sndSamples[163]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100100);
			inputStream.read(sndSamples[164]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100101);
			inputStream.read(sndSamples[165]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100110);
			inputStream.read(sndSamples[166]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10100111);
			inputStream.read(sndSamples[167]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101000);
			inputStream.read(sndSamples[168]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101001);
			inputStream.read(sndSamples[169]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101010);
			inputStream.read(sndSamples[170]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101011);
			inputStream.read(sndSamples[171]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101100);
			inputStream.read(sndSamples[172]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101101);
			inputStream.read(sndSamples[173]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101110);
			inputStream.read(sndSamples[174]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10101111);
			inputStream.read(sndSamples[175]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110000);
			inputStream.read(sndSamples[176]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110001);
			inputStream.read(sndSamples[177]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110010);
			inputStream.read(sndSamples[178]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110011);
			inputStream.read(sndSamples[179]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110100);
			inputStream.read(sndSamples[180]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110101);
			inputStream.read(sndSamples[181]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110110);
			inputStream.read(sndSamples[182]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10110111);
			inputStream.read(sndSamples[183]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111000);
			inputStream.read(sndSamples[184]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111001);
			inputStream.read(sndSamples[185]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111010);
			inputStream.read(sndSamples[186]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111011);
			inputStream.read(sndSamples[187]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111100);
			inputStream.read(sndSamples[188]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111101);
			inputStream.read(sndSamples[189]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111110);
			inputStream.read(sndSamples[190]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd10111111);
			inputStream.read(sndSamples[191]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000000);
			inputStream.read(sndSamples[192]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000001);
			inputStream.read(sndSamples[193]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000010);
			inputStream.read(sndSamples[194]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000011);
			inputStream.read(sndSamples[195]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000100);
			inputStream.read(sndSamples[196]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000101);
			inputStream.read(sndSamples[197]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000110);
			inputStream.read(sndSamples[198]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11000111);
			inputStream.read(sndSamples[199]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001000);
			inputStream.read(sndSamples[200]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001001);
			inputStream.read(sndSamples[201]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001010);
			inputStream.read(sndSamples[202]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001011);
			inputStream.read(sndSamples[203]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001100);
			inputStream.read(sndSamples[204]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001101);
			inputStream.read(sndSamples[205]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001110);
			inputStream.read(sndSamples[206]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11001111);
			inputStream.read(sndSamples[207]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010000);
			inputStream.read(sndSamples[208]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010001);
			inputStream.read(sndSamples[209]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010010);
			inputStream.read(sndSamples[210]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010011);
			inputStream.read(sndSamples[211]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010100);
			inputStream.read(sndSamples[212]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010101);
			inputStream.read(sndSamples[213]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010110);
			inputStream.read(sndSamples[214]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11010111);
			inputStream.read(sndSamples[215]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011000);
			inputStream.read(sndSamples[216]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011001);
			inputStream.read(sndSamples[217]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011010);
			inputStream.read(sndSamples[218]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011011);
			inputStream.read(sndSamples[219]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011100);
			inputStream.read(sndSamples[220]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011101);
			inputStream.read(sndSamples[221]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011110);
			inputStream.read(sndSamples[222]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11011111);
			inputStream.read(sndSamples[223]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100000);
			inputStream.read(sndSamples[224]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100001);
			inputStream.read(sndSamples[225]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100010);
			inputStream.read(sndSamples[226]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100011);
			inputStream.read(sndSamples[227]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100100);
			inputStream.read(sndSamples[228]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100101);
			inputStream.read(sndSamples[229]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100110);
			inputStream.read(sndSamples[230]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11100111);
			inputStream.read(sndSamples[231]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101000);
			inputStream.read(sndSamples[232]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101001);
			inputStream.read(sndSamples[233]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101010);
			inputStream.read(sndSamples[234]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101011);
			inputStream.read(sndSamples[235]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101100);
			inputStream.read(sndSamples[236]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101101);
			inputStream.read(sndSamples[237]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101110);
			inputStream.read(sndSamples[238]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11101111);
			inputStream.read(sndSamples[239]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110000);
			inputStream.read(sndSamples[240]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110001);
			inputStream.read(sndSamples[241]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110010);
			inputStream.read(sndSamples[242]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110011);
			inputStream.read(sndSamples[243]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110100);
			inputStream.read(sndSamples[244]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110101);
			inputStream.read(sndSamples[245]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110110);
			inputStream.read(sndSamples[246]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11110111);
			inputStream.read(sndSamples[247]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111000);
			inputStream.read(sndSamples[248]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111001);
			inputStream.read(sndSamples[249]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111010);
			inputStream.read(sndSamples[250]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111011);
			inputStream.read(sndSamples[251]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111100);
			inputStream.read(sndSamples[252]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111101);
			inputStream.read(sndSamples[253]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111110);
			inputStream.read(sndSamples[254]);
			inputStream.close();

			inputStream = getResources().openRawResource(R.raw.snd11111111);
			inputStream.read(sndSamples[255]);
			inputStream.close();

			
		} catch (IOException e) {
			//e.printStackTrace();
		}
		
		// Starting sound access
		oTrack.play();
	}
	
	// Starting thread to do the data to character conversion for serial modulation
	private Runnable doDataModulation = new Runnable() {
		public void run() {
			
			char charBuffer[] = new char[5];
			
			while (sndLoop) {
				sQueue.add('\n');
				
//				// Throttle
				charBuffer = String.format("%1.3f", gearThrottle).toCharArray();
				sQueue.add(charBuffer[0]);
				sQueue.add(charBuffer[1]);
				sQueue.add(charBuffer[2]);
				sQueue.add(charBuffer[3]);
				sQueue.add(charBuffer[4]);
				
//				// Elevator
				
				charBuffer = String.format("%1.3f", gearElevator).toCharArray();
				sQueue.add('\t');
				sQueue.add(charBuffer[0]);
				sQueue.add(charBuffer[1]);
				sQueue.add(charBuffer[2]);
				sQueue.add(charBuffer[3]);
				sQueue.add(charBuffer[4]);
				
//				// Rudder
				charBuffer = String.format("%1.3f", gearRudder).toCharArray();
				sQueue.add('\t');
				sQueue.add(charBuffer[0]);
				sQueue.add(charBuffer[1]);
				sQueue.add(charBuffer[2]);
				sQueue.add(charBuffer[3]);
				sQueue.add(charBuffer[4]);
				
//				// Aileron
				charBuffer = String.format("%1.3f", gearAileron).toCharArray();
				sQueue.add('\t');
				sQueue.add(charBuffer[0]);
				sQueue.add(charBuffer[1]);
				sQueue.add(charBuffer[2]);
				sQueue.add(charBuffer[3]);
				sQueue.add(charBuffer[4]);
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					// e.printStackTrace();
				}
				
			}
		}
	};
	
	// Starting thread to do the character queue to sound modulation and play-back
	private Runnable doSoundModulation = new Runnable() {
		public void run() {

			char charBufferThrottle[] = new char[4];
			char charBufferElevator[] = new char[4];
			char charBufferRudder[] = new char[4];
			char charBufferAileron[] = new char[4];
			
			for (int i = 0; i < minBufferSize; i++) {
				sndBufferTrack[i] = sndSample[0];
			}
			
			for (int i = 5; i < sampleLenght; i++) {
				//sndBufferTrack[i+sampleLenght*0] = sndSamples[(int) '\n'][i];
				//sndBufferTrack[i+sampleLenght*1] = sndSamples[(int) '\r'][i];
				//sndBufferTrack[i+sampleLenght*2] = sndSamples[(int) '\n'][i];
				//sndBufferTrack[i+sampleLenght*3] = sndSamples[(int) '\r'][i];
				//sndBufferTrack[i+sampleLenght*8] = sndSamples[(int) '\t'][i];
				//sndBufferTrack[i+sampleLenght*13] = sndSamples[(int) '\t'][i];
				//sndBufferTrack[i+sampleLenght*18] = sndSamples[(int) '\t'][i];
				//sndBufferTrack[i+sampleLenght*23] = sndSamples[(int) '\n'][i];
				//sndBufferTrack[i+sampleLenght*24] = sndSamples[(int) '\r'][i];
				
				//sndBufferTrack[i+sampleLenght*4] = sndSamples[(int) '\n'][i];
				//sndBufferTrack[i+sampleLenght*5] = sndSamples[(int) '\r'][i];
				sndBufferTrack[i+sampleLenght*6] = sndSamples[(int) '\n'][i];
				sndBufferTrack[i+sampleLenght*7] = sndSamples[(int) '\r'][i];
				sndBufferTrack[i+sampleLenght*12] = sndSamples[(int) '\t'][i];
				sndBufferTrack[i+sampleLenght*17] = sndSamples[(int) '\t'][i];
				sndBufferTrack[i+sampleLenght*22] = sndSamples[(int) '\t'][i];
				//sndBufferTrack[i+sampleLenght*27] = sndSamples[(int) '\n'][i];
				//sndBufferTrack[i+sampleLenght*28] = sndSamples[(int) '\r'][i];
			}
			
			while (sndLoop) {
				
				charBufferThrottle = String.format("%1.2f", gearThrottle).toCharArray();
				charBufferElevator = String.format("%1.2f", gearElevator).toCharArray();
				charBufferRudder = String.format("%1.2f", gearRudder).toCharArray();
				charBufferAileron = String.format("%1.2f", gearAileron).toCharArray();
				
				for (int i = 5; i < sampleLenght; i++) {
////					sndBufferTrack[i+sampleLenght*0] = sndSamples[(int) '\n'][i];
////					sndBufferTrack[i+sampleLenght*1] = sndSamples[(int) '\r'][i];
////					sndBufferTrack[i+sampleLenght*2] = sndSamples[(int) '\n'][i];
////					sndBufferTrack[i+sampleLenght*3] = sndSamples[(int) '\r'][i];
//					sndBufferTrack[i+sampleLenght*4] = sndSamples[(int) charBufferThrottle[0]][i];
//					sndBufferTrack[i+sampleLenght*5] = sndSamples[(int) charBufferThrottle[1]][i];
//					sndBufferTrack[i+sampleLenght*6] = sndSamples[(int) charBufferThrottle[2]][i];
//					sndBufferTrack[i+sampleLenght*7] = sndSamples[(int) charBufferThrottle[3]][i];
////					sndBufferTrack[i+sampleLenght*8] = sndSamples[(int) '\t'][i];
//					sndBufferTrack[i+sampleLenght*9] = sndSamples[(int) charBufferElevator[0]][i];
//					sndBufferTrack[i+sampleLenght*10] = sndSamples[(int) charBufferElevator[1]][i];
//					sndBufferTrack[i+sampleLenght*11] = sndSamples[(int) charBufferElevator[2]][i];
//					sndBufferTrack[i+sampleLenght*12] = sndSamples[(int) charBufferElevator[3]][i];
////					sndBufferTrack[i+sampleLenght*13] = sndSamples[(int) '\t'][i];
//					sndBufferTrack[i+sampleLenght*14] = sndSamples[(int) charBufferRudder[0]][i];
//					sndBufferTrack[i+sampleLenght*15] = sndSamples[(int) charBufferRudder[1]][i];
//					sndBufferTrack[i+sampleLenght*16] = sndSamples[(int) charBufferRudder[2]][i];
//					sndBufferTrack[i+sampleLenght*17] = sndSamples[(int) charBufferRudder[3]][i];
////					sndBufferTrack[i+sampleLenght*18] = sndSamples[(int) '\t'][i];
//					sndBufferTrack[i+sampleLenght*19] = sndSamples[(int) charBufferAileron[0]][i];
//					sndBufferTrack[i+sampleLenght*20] = sndSamples[(int) charBufferAileron[1]][i];
//					sndBufferTrack[i+sampleLenght*21] = sndSamples[(int) charBufferAileron[2]][i];
//					sndBufferTrack[i+sampleLenght*22] = sndSamples[(int) charBufferAileron[3]][i];
////					sndBufferTrack[i+sampleLenght*23] = sndSamples[(int) '\n'][i];
////					sndBufferTrack[i+sampleLenght*24] = sndSamples[(int) '\r'][i];
					
				
				sndBufferTrack[i+sampleLenght*8] = sndSamples[(int) charBufferThrottle[0]][i];
				sndBufferTrack[i+sampleLenght*9] = sndSamples[(int) charBufferThrottle[1]][i];
				sndBufferTrack[i+sampleLenght*10] = sndSamples[(int) charBufferThrottle[2]][i];
				sndBufferTrack[i+sampleLenght*11] = sndSamples[(int) charBufferThrottle[3]][i];
//				sndBufferTrack[i+sampleLenght*12] = sndSamples[(int) '\t'][i];
				sndBufferTrack[i+sampleLenght*13] = sndSamples[(int) charBufferElevator[0]][i];
				sndBufferTrack[i+sampleLenght*14] = sndSamples[(int) charBufferElevator[1]][i];
				sndBufferTrack[i+sampleLenght*15] = sndSamples[(int) charBufferElevator[2]][i];
				sndBufferTrack[i+sampleLenght*16] = sndSamples[(int) charBufferElevator[3]][i];
//				sndBufferTrack[i+sampleLenght*17] = sndSamples[(int) '\t'][i];
				sndBufferTrack[i+sampleLenght*18] = sndSamples[(int) charBufferRudder[0]][i];
				sndBufferTrack[i+sampleLenght*19] = sndSamples[(int) charBufferRudder[1]][i];
				sndBufferTrack[i+sampleLenght*20] = sndSamples[(int) charBufferRudder[2]][i];
				sndBufferTrack[i+sampleLenght*21] = sndSamples[(int) charBufferRudder[3]][i];
//				sndBufferTrack[i+sampleLenght*22] = sndSamples[(int) '\t'][i];
				sndBufferTrack[i+sampleLenght*23] = sndSamples[(int) charBufferAileron[0]][i];
				sndBufferTrack[i+sampleLenght*24] = sndSamples[(int) charBufferAileron[1]][i];
				sndBufferTrack[i+sampleLenght*25] = sndSamples[(int) charBufferAileron[2]][i];
				sndBufferTrack[i+sampleLenght*26] = sndSamples[(int) charBufferAileron[3]][i];
//				sndBufferTrack[i+sampleLenght*27] = sndSamples[(int) '\n'][i];
//				sndBufferTrack[i+sampleLenght*28] = sndSamples[(int) '\r'][i];
				}
				
				oTrack.write(sndBufferTrack, 0, minBufferSize);
				
//				oTrack.write(sndSamples[(int) '\n'], 0, sampleLenght);
//				
//				charBuffer = String.format("%1.3f", gearThrottle).toCharArray();
//				oTrack.write(sndSamples[(int) charBuffer[0]], 0, sampleLenght);
//				oTrack.write(sndSamples[(int) charBuffer[1]], 0, sampleLenght);
//				oTrack.write(sndSamples[(int) charBuffer[2]], 0, sampleLenght);
//				oTrack.write(sndSamples[(int) charBuffer[3]], 0, sampleLenght);
//				oTrack.write(sndSamples[(int) charBuffer[4]], 0, sampleLenght);
				
//				
//				if (sQueue.isEmpty()) {
//					oTrack.write(sndSamples[0], 0, sampleLenght);
////					while (sQueue.isEmpty()) {
////						try {
////							Thread.sleep(10);
////						} catch (InterruptedException e) {
////							//e.printStackTrace();
////						}
////					}
//				} else {
//					oTrack.write(sndSamples[(int) sQueue.poll()], 0, sampleLenght);
//				}
//				
			}
		}
	};
}