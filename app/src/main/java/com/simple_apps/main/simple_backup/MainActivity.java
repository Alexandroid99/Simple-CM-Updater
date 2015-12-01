package com.simple_apps.main.simple_backup;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {

    private Calendar calendar;
    private DownloadManager downloadManager;
    private long downloadID;
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private SimpleDateFormat dateFormatter;
    private int lastInstallation;
    private int today;
    public BroadcastReceiver downloadReady;

    private String fileUri;

    @Override
    protected void onResume(){
        super.onResume();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = this.getPreferences(Context.MODE_PRIVATE);
        editor = preferences.edit();

        dateFormatter = new SimpleDateFormat("dd-MM-yy");

        try {
            lastInstallation = preferences.getInt("date", 0);
        }  catch (NullPointerException e) {
            e.printStackTrace();
        }

      TimerTask task = new TimerTask().setVariables(this, downloadID,dateFormatter);
        task.run();
    }

    private int checkTime() {
        if (calendar == null){
            calendar = Calendar.getInstance();
        }
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        Logger.getLogger("checkTime").log(Level.SEVERE, String.valueOf(hour));

        if(hour >= 12 && hour <= 15){
            return 0;
        }
        return 1;
    }

    private boolean checkEnergy() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING;

        return isCharging;
    }

    private void downloadFile(){
        // https://download.cyanogenmod.org/get/bacon-latest.zip

        if(downloadManager == null){
            downloadManager = (DownloadManager) this.getSystemService(DOWNLOAD_SERVICE);
        }
        Uri uri = Uri.parse("https://download.cyanogenmod.org/get/bacon-latest.zip");
        DownloadManager.Request request = new DownloadManager.Request(uri);

            request.setDestinationInExternalPublicDir("/cmupdater", "CM-Latest.zip");

       downloadID = downloadManager.enqueue(request);
     //   Logger.getLogger("DOWNLOAD-ID").log(Level.SEVERE,String.valueOf(downloadID));


    }

    private void createrecoveryScript(){
        String backupCommands ="\"backup SCDBE\"";
        String installCommands ="\"install /storage/emulated/0/cmupdater/CM-Latest.zip\"";


        Shell.SU.run("echo " + backupCommands +" > /cache/recovery/openrecoveryscript");
        Shell.SU.run("echo " + installCommands +" >> /cache/recovery/openrecoveryscript");
        Shell.SU.run("am broadcast android.intent.action.ACTION_SHUTDOWN");
        Shell.SU.run("sleep 5");
        Shell.SU.run("reboot recovery");
    }

    private boolean checkWifi(){
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null && wifiNetwork.isConnected())
        {
            return true;
        }
//        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
//        if (mobileNetwork != null && mobileNetwork.isConnected())
//        {
//            return true;
//        }
//        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
//        if (activeNetwork != null && activeNetwork.isConnected())
//        {
//            return true;
//        }
        return false;
    }

    private void checkNumbersOfBackups(){
    }

    private class TimerTask extends Thread {
        private Context context = null;
        private long downloadID;
        private long reference;
        private Calendar tmpCalendar;
        private SimpleDateFormat formatter;
        private boolean installedW = false;
        IntentFilter intentFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        BroadcastReceiver downloadReady = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        //        Logger.getLogger("DOWNLOAD-ID-NEW").log(Level.SEVERE,String.valueOf(downloadID));
        //        Logger.getLogger("REFERENCE-ID").log(Level.SEVERE,String.valueOf(reference));

                if (downloadID == reference){
                    createrecoveryScript();
                }
            }
        } ;

        public TimerTask setVariables(Context context, long downloadId, SimpleDateFormat formatter ) {
            this.context = context;
            this.downloadID = downloadId;
            this.formatter = formatter;
            return this;
        }

        public void run() {
            tmpCalendar = Calendar.getInstance();
            today = tmpCalendar.DAY_OF_YEAR;



            while(true){

                if(lastInstallation == 0 || lastInstallation<today){
                    if(checkTime() == 0){

                        if(checkEnergy() && checkWifi()){
                            downloadFile();
                            editor.putInt("date", today);
                            lastInstallation=today;
                            try {
                                Thread.sleep(300000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        //    Logger.getLogger("SCRIPT").log(Level.SEVERE,"STARTING");

                            createrecoveryScript();
                        }

                    }else{
                        try {
                            Thread.sleep(7100000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                }else{
                    try {
                        Thread.sleep(43200000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
