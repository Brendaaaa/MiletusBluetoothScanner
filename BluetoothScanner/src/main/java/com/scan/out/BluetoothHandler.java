package com.scan.out;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class BluetoothHandler {

	private BluetoothAdapter bluetoothAdapter;

	private boolean bluetoothEnabled = false;
	private boolean bluetoothScanning;

	private ArrayList<String> data;

	private OnScanListener onScanListener;
    private Context context;

    public interface OnScanListener{
    	public void onScanFinished();
    };
    
    public void setOnScanListener(OnScanListener listener){
    	onScanListener = listener;
    }
    
	public BluetoothHandler(Context context) {

		this.context = context;
		data = new ArrayList<String>();

		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.EXTRA_RSSI);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		((MainActivity) context).registerReceiver(broadcastReceiver, filter);

		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Enables Bluetooth
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((MainActivity)context).startActivityForResult(intent, 1);
        } else {
			bluetoothEnabled = true;
        }
	}
	
	public boolean isEnabled(){
		return bluetoothEnabled;
	}

    // scan device
 	public void beginScan() {
 		if (isEnabled()) {
			bluetoothAdapter.startDiscovery();
 		}
 	}

    public String getBluetoothScanInfoFormatted(String date, String macAddress, String rssi, String deviceName){
		String data = "";
		data += date + "," + macAddress + "," + rssi + "," + BluetoothApplication.region + "," + deviceName + ";";
		System.out.println("BLUETOOTH HANDLER: found = "+ data);
		return data;
	}

	private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
				final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
				if (state == BluetoothAdapter.STATE_ON) {
					bluetoothEnabled = true;
					BluetoothApplication.setRegion(-20); //bluetooth was enabled after app has started
				} else {
					bluetoothEnabled = false;
					BluetoothApplication.setRegion(20); //indicates bluetooth it's not working
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				bluetoothScanning = true;
				if (data == null){
					data = new ArrayList<String>();
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				bluetoothScanning = false;
				writeData();
			} else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(Calendar.getInstance().getTime());
				final BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				final int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
				data.add(getBluetoothScanInfoFormatted(date, device.getAddress(), Integer.toString(rssi), device.getName()));
			}
		}
	};

	public void writeData(){

		class WriteDataAsyncTask extends AsyncTask<ArrayList<String>, Void, Boolean> {

			@Override
			protected Boolean doInBackground(ArrayList<String>... content) {

				String newContent = "";
				for (String string : content[0]){
					newContent += string;
				}

				try {
					PrintWriter writer = new PrintWriter(context.openFileOutput(BluetoothService.DEVICES_MEASURES, context.MODE_APPEND));

					if (writer != null) {
						writer.println(newContent);
						writer.println("\r\n");
						writer.close();
						return true;
					}
				} catch (Exception e) {
					System.out.println(e);
					e.printStackTrace();
				}
				return false;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);

				//Scan its over
				if(onScanListener != null){
					onScanListener.onScanFinished();
				}

				//Clean data
				if (result == true){
					data.removeAll(data);
				}
			}
		}

		WriteDataAsyncTask writeDataAsyncTask = new WriteDataAsyncTask();
		writeDataAsyncTask.execute(data);
	}
}