package com.slbcsukapura.pemantauan;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.location.Location;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.util.HashMap;

import static android.content.ContentValues.TAG;
import static android.widget.Toast.makeText;


public class LoginActivity extends AppCompatActivity {
    EditText Nis, Password;
    Button LogIn ;
    String PasswordHolder, NisHolder;
    String finalResult ;
    String HttpURL = "https://slbcsukapura.com/android/APILogin.php";
    Boolean CheckEditText ;
    ProgressDialog progressDialog;
    HashMap<String,String> hashMap = new HashMap<>();
    HttpParse httpParse = new HttpParse();
    public static final String UserNis = "";
    DatabaseHelper MyDB;

    Location location;
    private BroadcastReceiver locationUpdateReceiver;
    private BroadcastReceiver predictedLocationReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //double lat = location.getLatitude();
        //double lng = location.getLongitude();



        setContentView(R.layout.activity_login);
        MyDB = new DatabaseHelper(this);
        Cursor res = MyDB.LihatData();
        if(res.moveToNext()){
            finish();
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            getPermissionToReadUserLocation();
//        }
        Nis = (EditText)findViewById(R.id.nis);
        Password = (EditText)findViewById(R.id.password);
        LogIn = (Button)findViewById(R.id.Login);

        //startService(new Intent(LoginActivity.this, LocationTrace.class));

        LogIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                CheckEditTextIsEmptyOrNot();

                if(CheckEditText){

                    UserLoginFunction(NisHolder, PasswordHolder);

                }
                else {

                    Toast.makeText(LoginActivity.this, "Please fill all form fields.", Toast.LENGTH_LONG).show();

                }

            }
        });

        LocalBroadcastManager.getInstance(this).registerReceiver(
                locationUpdateReceiver,
                new IntentFilter("LocationUpdated"));

        LocalBroadcastManager.getInstance(this).registerReceiver(
                predictedLocationReceiver,
                new IntentFilter("PredictLocation"));

    }
    // private static final int READ_LOCATION_PERMISSIONS_REQUEST = 1;

    public void CheckEditTextIsEmptyOrNot(){

        NisHolder = Nis.getText().toString();
        PasswordHolder = Password.getText().toString();

        if(TextUtils.isEmpty(NisHolder) || TextUtils.isEmpty(PasswordHolder))
        {
            CheckEditText = false;
        }
        else {

            CheckEditText = true ;
        }
    }

    public void UserLoginFunction(final String nis, final String password){

        class UserLoginClass extends AsyncTask<String,Void,String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                progressDialog = ProgressDialog.show(LoginActivity.this,"Loading Data",null,true,true);
            }

            @Override
            protected void onPostExecute(String httpResponseMsg) {

                super.onPostExecute(httpResponseMsg);

                progressDialog.dismiss();
                Log.i("Hasil", "onPostExecute: "+httpResponseMsg);
                if(httpResponseMsg.equalsIgnoreCase("Data Matched")){

                    MyDB.SimpanData(NisHolder, PasswordHolder);
                    finish();

                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);

                    intent.putExtra(UserNis,nis);

                    startActivity(intent);

                }
                else{

                    Toast.makeText(LoginActivity.this, "Nis atau Password salah.", Toast.LENGTH_LONG).show();
                }

            }

            @Override
            protected String doInBackground(String... params) {

                hashMap.put("nis",params[0]);

                hashMap.put("password",params[1]);

                finalResult = httpParse.postRequest(hashMap, HttpURL);

                return finalResult;
            }
        }

        UserLoginClass userLoginClass = new UserLoginClass();

        userLoginClass.execute(nis,password);
    }



}
