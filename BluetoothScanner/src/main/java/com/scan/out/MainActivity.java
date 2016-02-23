package com.scan.out;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

	private BluetoothHandler bluetoothHandler;
	private static final long MINUTE = 1000 * 60;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		bluetoothHandler = new BluetoothHandler(this);

		BluetoothApplication app = (BluetoothApplication)getApplication();
		app.setBluetoothHandler(bluetoothHandler);

		//Setting alarm to initiates a service
		AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
		alarm.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
				MINUTE/4,
				MINUTE/4, PendingIntent.getService(this, 0, new Intent(this, BluetoothService.class), 0));
	}
}