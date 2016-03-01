package com.scan.out;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by brendaramires on 19/01/16.
 */
public class BluetoothService extends Service {

    private final static String API_KEY = "6SZ54V6R18OAM95O";
    private final static String SERVER_ADDRESS = "http://10.35.99.86:3000/upload";
    public final static String DEVICES_MEASURES = "devicesMeasures.csv";
    private BluetoothApplication bluetoothApplication;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        bluetoothApplication = (BluetoothApplication)getApplication();
//        final BluetoothApplication app = bluetoothApplication;

        //Bluetooth Handler performs all the bluetooth activity
        BluetoothHandler bluetoothHandler = bluetoothApplication.getBluetoothHandler();
        if (bluetoothHandler != null) {
            System.out.println("BLUETOOTH SERVICE: service starts and bluetooth handler is not null");
            // This listener indicates when search is finished
            bluetoothHandler.setOnScanListener(new BluetoothHandler.OnScanListener() {
                @Override
                public void onScanFinished() {
                    // Post not happens all the time
                    if (bluetoothApplication.isPostTime()) {
                        sendPostRequest(getPostDataFormatted());
                    } else {
                        // Destroys service
                        stopSelf();
                    }
                }
            });

            //Begin scan
            bluetoothHandler.beginScan();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        System.out.println("BLUETOOTH SERVICE: onDestroy();");
    }

    // AsyncTask is created to send data
    public void sendPostRequest(final String content) {

        class SendPostReqAsyncTask extends AsyncTask<String, Void, String> {

            @Override
            protected String doInBackground(String... csvContent) {

                System.out.println("BLUETOOTH SERVICE: csv =" + csvContent[0]);

                HttpClient httpClient = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost(SERVER_ADDRESS);

                // Creating the request
                try {
                    // Post content
                    List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();
                    nameValuePairList.add(new BasicNameValuePair("api_key", API_KEY));
                    nameValuePairList.add(new BasicNameValuePair("csv", csvContent[0]));

                    // Content added to request
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(nameValuePairList);
                    httpPost.setEntity(urlEncodedFormEntity);

                    //Sending request
                    try {
                        HttpResponse httpResponse = httpClient.execute(httpPost);
                        return EntityUtils.toString(httpResponse.getEntity());

                    } catch (Exception e) {
                        System.out.println("BLUETOOTH SERVICE: exception - httpResponse :" + e);
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    System.out.println("BLUETOOTH SERVICE: exception - UrlEncodedFormEntity argument :" + e);
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(String response) {
                super.onPostExecute(response);

                //Delete local csv if this version was correctly uploaded
                if(response.equals("OK")) {
                    File file = new File(getApplication().getApplicationContext().getFilesDir() + "/" + DEVICES_MEASURES);

                    System.out.println("BLUETOOTH SERVICE: post ok");
                    if (file.delete()) {
                        System.out.println("BLUETOOTH SERVICE: file deleted");
                    }
                } else {
                    System.out.println("BLUETOOTH SERVICE: post later");
                }
                //Destroy service
                stopSelf();

            }
        }

        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();
        sendPostReqAsyncTask.execute(content);
    }

    // Thingspeak only accepts csv with date
    private String getPostDataFormatted(){
        String content = getLocalCSVContent();
        System.out.println("csv length = " + content.length());
        String csv ="created_at,mac,rssi,region,name\n";
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(Calendar.getInstance().getTime());
        //Heart beat - region number: usually a value between 1 and 6. Other values indicates problem
        String heartBeat = date + ",-,-," + bluetoothApplication.region() + "," + "heartBeat;";
        csv += heartBeat + content;
        return csv.replace(";", "\n");
    }

    //Reading local csv file
    private String getLocalCSVContent() {
        Context context = bluetoothApplication.getBaseContext();
        String text = "";

        try {
            InputStream inputStream = context.openFileInput(DEVICES_MEASURES);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder stringBuilder = new StringBuilder();
                String line = "";

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                inputStream.close();
                text = stringBuilder.toString();

                return text;

            } else {
                System.out.println("BLUETOOTH SERVICE: there is no content");
            }
        } catch (FileNotFoundException e) {
            System.out.println("BLUETOOTH SERVICE: exception - file not found:" + e);
        } catch (IOException e) {
            System.out.println("BLUETOOTH SERVICE: exception - cant read file:" + e);
        } catch (Exception e){
            System.out.println("BLUETOOTH SERVICE: exception: " + e);
        }

        return null;
    }

}