package com.scan.out;

import android.app.Application;
import android.content.res.Configuration;

/**
 * Created by brendaramires on 19/01/16.
 */
public class BluetoothApplication extends Application {

    //number of scans until post time
    private final static int max = 40;
    BluetoothHandler b;
    int counter;
    public static String region = "6";

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        b = null;
        counter = 0;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public BluetoothHandler getBluetoothHandler(){
        return b;
    }

    public void setBluetoothHandler(BluetoothHandler b){
        this.b = b;
    }

    public boolean isPostTime(){
        counter++;
        System.out.println("More " + (max - counter) + " to post");
        counter = counter % max;
        if (counter > 0){
            return false;
        }
        return true;
    }

    public int region(){
        return Integer.parseInt(region);
    }

    public static void setRegion(int r){
        int regionNumber = Integer.parseInt(region);
        regionNumber += r;
        region = Integer.toString(regionNumber);
    }
}
