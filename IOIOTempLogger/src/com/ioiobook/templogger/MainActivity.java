/**
 * code from the book Getting Started with IOIO
 * <br>Copyright 2011 Simon Monk
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package com.ioiobook.templogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import ioio.lib.api.AnalogInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.DigitalOutput.Spec.Mode;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.AbstractIOIOActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class MainActivity extends AbstractIOIOActivity implements OnCheckedChangeListener {
	
	private final static long SAMPLE_PERIOD = 10000; // 10 seconds
	private static final int PLUS_PIN = 44;
	private static final int GND_PIN = 46;
	private static final int INPUT_PIN = 45;	
	
	private TextView temperature_;
	private RadioButton radioF_;
	private ToggleButton logButton_;
	private TextView lastLogLine_;

	private long lastSampleTime_;

	/**
	 * Called when the activity is first created. Here we normally initialize
	 * our GUI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
        temperature_ = (TextView)findViewById(R.id.temp);
        radioF_ = (RadioButton)findViewById(R.id.radio_f);
        logButton_ = (ToggleButton)findViewById(R.id.logButton);
        logButton_.setOnCheckedChangeListener(this);
        lastLogLine_ = (TextView)findViewById(R.id.lastLogLine);
	}

	/**
	 * This is the thread on which all the IOIO activity happens. It will be run
	 * every time the application is resumed and aborted when it is paused. The
	 * method setup() will be called right after a connection with the IOIO has
	 * been established (which might happen several times!). Then, loop() will
	 * be called repetitively until the IOIO gets disconnected.
	 */
	class IOIOThread extends AbstractIOIOActivity.IOIOThread {
		private DigitalOutput plusPin_;
		private DigitalOutput gndPin_;
		private AnalogInput inputPin_;

		/**
		 * Called every time a connection with IOIO has been established.
		 * Typically used to open pins.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#setup()
		 */
		@Override
		protected void setup() throws ConnectionLostException {
			gndPin_ = ioio_.openDigitalOutput(GND_PIN, Mode.NORMAL, false); // gnd supply to temp sensor
			plusPin_ = ioio_.openDigitalOutput(PLUS_PIN, Mode.NORMAL, true); // positive supply to temp sensor
			inputPin_ = ioio_.openAnalogInput(INPUT_PIN);
		}

		/**
		 * Called repetitively while the IOIO is connected.
		 * 
		 * @throws ConnectionLostException
		 *             When IOIO connection is lost.
		 * @throws InterruptedException 
		 * 
		 * @see ioio.lib.util.AbstractIOIOActivity.IOIOThread#loop()
		 */
		@Override
		protected void loop() throws ConnectionLostException {
			try {
				float v = inputPin_.getVoltage();
				String units = "C";
				float temp = (v - 0.5f) * 100.0f; 
				if (radioF_.isChecked()) {
					temp = temp * 9.0f / 5.0f + 32.0f; 
					units = "F";
				}
				// round to 1 dp
				temp = Math.round(temp * 10) / 10.0f;
				updateTempField(temp, units);
				long now = System.currentTimeMillis();
				if (now > lastSampleTime_ + SAMPLE_PERIOD) {
					logTemp(temp, units);
					lastSampleTime_ = now;
				}
				sleep(100);
			} catch (Exception e) {
				toast(e.getMessage());
			}
		}

		private void logTemp(float temp, String units) {
			if (logButton_.isChecked()) {
				Date now = new Date();
				String filename = "temp_" + DateFormat.format("yyyy_MM_dd", now).toString() + ".csv";
				CharSequence time = DateFormat.format("hh:mm:ss", now);
				String line = "" + time + ", " + temp + ", " + units + "\n";
				updateLastLogLine(line);
				appendToFile(filename, line);
			}
		}

		private void appendToFile(String filename, String line) {
			File root = Environment.getExternalStorageDirectory();
            try {
				FileOutputStream f = new FileOutputStream(new File(root, filename), true);
				f.write(line.getBytes());
				f.close();
			} catch (Exception e) {
				toast(e.getMessage());
			}
		}
	}

	/**
	 * A method to create our IOIO thread.
	 * 
	 * @see ioio.lib.util.AbstractIOIOActivity#createIOIOThread()
	 */
	@Override
	protected AbstractIOIOActivity.IOIOThread createIOIOThread() {
		return new IOIOThread();
	}
	
	private void updateTempField(final float temp, final String units) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				temperature_.setText("" + temp + " " + units);
			}
		});
	}
	
	private void updateLastLogLine(final String line) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				lastLogLine_.setText(line);
			}
		});
	}
	
	private void toast(final String message) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Context context = getApplicationContext();
				int duration = Toast.LENGTH_SHORT;

				Toast toast = Toast.makeText(context, message, duration);
				toast.show();
			}
		});
	}

@Override
public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
	if (isChecked) {
		toast("Logging Started");
	}
}


	
}