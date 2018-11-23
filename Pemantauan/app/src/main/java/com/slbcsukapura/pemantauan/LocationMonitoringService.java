package com.slbcsukapura.pemantauan;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.HashMap;

import static android.widget.Toast.makeText;


public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private HashMap<String, String> hashMap = new HashMap<>();
    private HttpParse httpParse = new HttpParse();
    private String HttpURL = "https://slbcsukapura.com/android/UpdateLoc.php";
    private String finalResult;
    BroadcastReceiver mReceiver;
    private static final String TAG = LocationMonitoringService.class.getSimpleName();
    private GoogleApiClient mLocationClient;
    private LocationRequest mLocationRequest = new LocationRequest();
    DatabaseHelper MyDB;
    String batreHolder;


    public static final String ACTION_LOCATION_BROADCAST = LocationMonitoringService.class.getName() + "LocationBroadcast";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String EXTRA_AKURASI = "extra_akurasi";
    private static final int LOCATION_INTERVAL = 10000;
    private static final int FASTEST_LOCATION_INTERVAL = 2000;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mLocationClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        mLocationRequest.setInterval(LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_LOCATION_INTERVAL);


        int priority = LocationRequest.PRIORITY_HIGH_ACCURACY; //by default
        //PRIORITY_BALANCED_POWER_ACCURACY, PRIORITY_LOW_POWER, PRIORITY_NO_POWER are the other priority modes


        mLocationRequest.setPriority(priority);
        mLocationClient.connect();

        //Make it stick to the notification panel so it is less prone to get cancelled by the Operating System.
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*
     * LOCATION CALLBACKS
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            Log.d(TAG, "== Error On onConnected() Permission not granted");
            //Permission not granted by user so cancel the further execution.

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, mLocationRequest, this);

        Log.d(TAG, "Connected to Google API");
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection suspended");
    }


    //to get the location change
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed");
          if (location != null) {
            Log.d(TAG, "== location != null");

            //Send result to activities
            sendMessageToUI(String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()),String.valueOf(location.getAccuracy()));
        }

    }

    private void sendMessageToUI(String lat, String lng,String akurasi) {
        String nis = null;
        Log.d(TAG, "Sending info...");

        //get nis
        MyDB = new DatabaseHelper(this);
        Cursor res = MyDB.LihatData();
        if(res.moveToNext()){
            nis = res.getString(0);
        }
        MyDB.close();
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mReceiver = new BatteryBroadcastReceiver();
        if(batreHolder!=null && nis!=null){
                UserUpdateLocFunction(nis, lng, lat, batreHolder);
        }
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lng);
        intent.putExtra(EXTRA_AKURASI, akurasi);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    private class BatteryBroadcastReceiver extends BroadcastReceiver {
        private final static String BATTERY_LEVEL = "level";
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryPct = 0;
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level != -1 && scale != -1) {
                batteryPct = (int) ((level / (float) scale) * 100f);
                batreHolder= Integer.toString(batteryPct);
                Log.d(TAG, "baterai "+ batreHolder);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        startService(restartServiceIntent);
        // unregisterReceiver(mReceiver);
        super.onTaskRemoved(rootIntent);
    }

    public void UserUpdateLocFunction(final String nis, final String longitude, final String lat, final String batre){

        class UserUpdateLocClass extends AsyncTask<String,Void,String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String httpResponseMsg) {

                super.onPostExecute(httpResponseMsg);


                Log.i("Hasil", "onPostExecute: "+httpResponseMsg);
                if(httpResponseMsg.equalsIgnoreCase("Sukses")){

                    makeText(LocationMonitoringService.this, "berhasil mengirim data ke server", Toast.LENGTH_SHORT).show();

                }
                else{

                    makeText(LocationMonitoringService.this, httpResponseMsg, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            protected String doInBackground(String... params) {

                hashMap.put("nis",params[0]);

                hashMap.put("longitude",params[1]);

                hashMap.put("lat",params[2]);

                hashMap.put("batre",params[3]);

                finalResult = httpParse.postRequest(hashMap, HttpURL);

                return finalResult;
            }
        }

        UserUpdateLocClass userUpdateLocClass = new UserUpdateLocClass();

        userUpdateLocClass.execute(nis,longitude,lat,batre);
    }

    @Override
    public void onDestroy() {
        stopSelf();
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        //unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
