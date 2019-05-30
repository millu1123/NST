package com.sample.tutorial3;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.os.AsyncTask;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import java.net.HttpURLConnection;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity implements OnClickListener {

    //private ImageView image01;
    private static final String TAG = "WIFIScanner";
    private static String IP_ADDRESS = "ip-nst.iptime.org:5000";

    // WifiManager variable
    WifiManager wifimanager;
    // UI variable
    EditText loc_text,sec_text;
    Button btnScanStart, btnInfo, btnSendMsg, btnHelp;
    TextView textA, textB, textC, textD, textE, textF, textG, textH, textI, textJ;
    private int scanCount = 0;
    String text = "";
    String result = "";
    String serv_result = "";
    String msg = "";
    String loc = "";
    String sec = "";
    private List<ScanResult> mScanResult; // ScanResult List

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setup UI
        btnScanStart = (Button) findViewById(R.id.btnScanStart);
        btnInfo = (Button) findViewById(R.id.btnInfo);
        btnSendMsg = (Button) findViewById(R.id.btnSendMsg);
        btnHelp = (Button) findViewById(R.id.btnHelp);

        // Setup OnClickListener
        btnScanStart.setOnClickListener(this);
        btnInfo.setOnClickListener(this);
        btnSendMsg.setOnClickListener(this);
        btnHelp.setOnClickListener(this);

        // Setup WIFI
        wifimanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        Log.d(TAG, "Setup WIfiManager getSystemService");

        if(Build.VERSION.SDK_INT>=23 && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.CHANGE_WIFI_STATE) !=PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.INTERNET) !=PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.INTERNET, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
            },200);
        }


        // if WIFIEnabled
        if (wifimanager.isWifiEnabled() == false) {
            wifimanager.setWifiEnabled(true);
            printToast("wifi enabled");
        }

    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getWIFIScanResult(); // get WIFISCanResult
                wifimanager.startScan();
                if(scanCount==10) unregisterReceiver(mReceiver); // stop WIFISCan

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    public void getWIFIScanResult() {
        // Scan count
        mScanResult = wifimanager.getScanResults();
        String rssi1_name = "NST1";
        String rssi2_name = "NST2";
        String rssi3_name = "NST3";
        String rssi4_name = "NST4";
        String rs1 = "";
        String rs2 = "";
        String rs3 = "";
        String rs4 = "";
        String loc=loc_text.getText().toString();
        String sec=sec_text.getText().toString();

        scanCount++;

        for (int i = 0; i < mScanResult.size(); i++) {
            ScanResult result = mScanResult.get(i);

            if (result.SSID.toString().equals(rssi1_name)) {
                rs1 = String.valueOf(result.level);
            }
            if (result.SSID.toString().equals(rssi2_name)) {
                rs2 = String.valueOf(result.level);
            }
            if (result.SSID.toString().equals(rssi3_name)) {
                rs3 = String.valueOf(result.level);
            }
            if (result.SSID.toString().equals(rssi4_name)) {
                rs4 = String.valueOf(result.level);
            }
        }

        if (rs1.equals("")) rs1 = "-255";
        if (rs2.equals("")) rs2 = "-255";
        if (rs3.equals("")) rs3 = "-255";
        if (rs4.equals("")) rs4 = "-255";

        InsertData task = new InsertData();
        task.execute("http://" + IP_ADDRESS + "/post_insertDB", rs1, rs2, rs3, rs4, sec,loc);
    }

    public void printToast(String messageToast) {
        Toast.makeText(this, messageToast, Toast.LENGTH_LONG).show();
    }

    class InsertData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(MainActivity.this,
                    "Please Wait", null, true, true);
        }


        @Override
        protected void onPostExecute(String Result) {
            super.onPostExecute(Result);

            progressDialog.dismiss();
            Log.d(TAG, "POST response  - " + Result);
        }


        @Override
        protected String doInBackground(String... params) {

            String values1 = (String) params[1];
            String values2 = (String) params[2];
            String values3 = (String) params[3];
            String values4 = (String) params[4];
            String values5 = (String) params[5];
            String values6 = (String) params[6];
            String serverURL = (String) params[0];
            String postParameters = "values1=" + values1 + "&values2=" + values2 + "&values3=" + values3 + "&values4=" + values4 + "&sec=" + values5 + "&loc=" + values6;


            try {

                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();


                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();


                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();


                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "POST response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }


                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line = null;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }


                bufferedReader.close();
                serv_result = sb.toString();

                return serv_result;


            } catch (Exception e) {

                Log.d(TAG, "InsertData: Error ", e);

                return new String("Error: " + e.getMessage());
            }

        }
    }

    @Override
    public void onClick(View v) {

        if (v.getId() == R.id.btnScanStart) {
            Log.d(TAG, "OnClick() btnScanStart");
            scanCount=0;
            initWIFIScan(); // start WIFIScan
        }
        else if(v.getId() == R.id.btnInfo){
            Log.d(TAG, "OnClick() btnInfo");
            getInfo();
        }
        else if(v.getId() == R.id.btnSendMsg){
            Log.d(TAG, "OnClick() btnSendMsg");
            send_message();
        }
        else if(v.getId() == R.id.btnHelp){
            Log.d(TAG, "OnClick() btnHelp");
            showHelp();
        }

    }

    public void initWIFIScan() {
        // init WIFISCAN

        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.input_loc, null);
        builder.setView(view);
        builder.setTitle("FP측정");

        loc_text = (EditText)view.findViewById(R.id.loc);
        loc_text.requestFocus();
        sec_text = (EditText)view.findViewById(R.id.sec);

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                loc = loc_text.getText().toString();
                sec = sec_text.getText().toString();

                printToast(loc + "   " + sec);
                printToast("WIFI scan for DB !!!");

                final IntentFilter filter = new IntentFilter(
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
                registerReceiver(mReceiver, filter);
                wifimanager.startScan();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }

    public void getInfo() {
        final String serverURL = "http://" + IP_ADDRESS + "/guestInfo";
        Log.d(TAG, "get value");

        new Thread() {
            public void run() {
                try {


                    URL url = new URL(serverURL);
                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                    httpURLConnection.setRequestMethod("GET");
                    httpURLConnection.connect();

                    int responseStatusCode = httpURLConnection.getResponseCode();
                    Log.d(TAG, "POST response code - " + responseStatusCode);

                    InputStream inputStream;
                    if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                        inputStream = httpURLConnection.getInputStream();
                    } else {
                        inputStream = httpURLConnection.getErrorStream();
                    }

                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    StringBuilder sb = new StringBuilder();
                    String line = null;

                    while ((line = bufferedReader.readLine()) != null) {
                        sb.append(line);
                    }

                    bufferedReader.close();
                    serv_result = sb.toString();
                    Log.d(TAG, "return : " + serv_result);

                    Message msg = Message.obtain();
                    msg.obj = serv_result;
                    handler.sendMessage(msg);

                } catch (Exception e) {
                    Log.d(TAG, "InsertData: Error ", e);
                }
            }
        }.start();
    }

    public Handler handler = new Handler() {
        public void handleMessage(Message msg){
            //setContentView(R.layout.display_map);



            String Info = (String)msg.obj;
            String[] arr = new String[10];

            Dialog dialog = new Dialog(MainActivity.this){
                @Override
                public void onBackPressed()
                {
                    super.onBackPressed();
                    this.dismiss();
                }
            };


            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());


            Point size = new Point();
            getWindowManager().getDefaultDisplay().getSize(size);
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.width = size.x;

            dialog.setContentView(R.layout.display_map);
            dialog.setCancelable(false); //화면 눌렀을 때 안 꺼지게
            dialog.getWindow().setAttributes(lp);
            //dialog.setTitle("고객위치정보");

            textA = (TextView)dialog.findViewById(R.id.A);
            textB = (TextView)dialog.findViewById(R.id.B);
            textC = (TextView)dialog.findViewById(R.id.C);
            textD = (TextView)dialog.findViewById(R.id.D);
            textE = (TextView)dialog.findViewById(R.id.E);
            textF = (TextView)dialog.findViewById(R.id.F);
            //textG = (TextView)dialog.findViewById(R.id.G);
            textH = (TextView)dialog.findViewById(R.id.H);
            textI = (TextView)dialog.findViewById(R.id.I);
            textJ = (TextView)dialog.findViewById(R.id.J);

            dialog.show();
            //Window window = dialog.getWindow();
            //window.setAttributes(lp);

            StringTokenizer st = new StringTokenizer(Info);
            String temp = "";

            int i = 0;
            while(st.hasMoreTokens()) {
                arr[i] = st.nextToken();
                i++;
            }

            textA.setText(arr[0]);
            textB.setText(arr[1]);
            textC.setText(arr[2]);
            textD.setText(arr[3]);
            textE.setText(arr[4]);
            textF.setText(arr[5]);
            //textG.setText(arr[6]);
            textH.setText(arr[7]);
            textI.setText(arr[8]);
            textJ.setText(arr[9]);
        }
    };

    public void send_message(){
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.topMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);
        msg = "";
        dialog.setTitle("알림이");
        dialog.setView(input);
        dialog.setView(container);
        dialog.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                msg = input.getText().toString();
                msg = msg.replace(System.getProperty("line.separator"), "\n");
                final String serverURL = "http://" + IP_ADDRESS + "/setMessage";
                final String postParameters = "message=" + msg;
                Log.d(TAG, "get value");

                new Thread() {
                    public void run() {
                        try {

                            URL url = new URL(serverURL);
                            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                            httpURLConnection.setReadTimeout(5000);
                            httpURLConnection.setConnectTimeout(5000);
                            httpURLConnection.setRequestMethod("POST");
                            httpURLConnection.setDoOutput(true);
                            httpURLConnection.connect();

                            OutputStream outputStream = httpURLConnection.getOutputStream();
                            outputStream.write(postParameters.getBytes("UTF-8"));
                            outputStream.flush();
                            outputStream.close();

                            int responseStatusCode = httpURLConnection.getResponseCode();
                            Log.d(TAG, "POST response code - " + responseStatusCode);

                            InputStream inputStream;
                            if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                                inputStream = httpURLConnection.getInputStream();
                            } else {
                                inputStream = httpURLConnection.getErrorStream();
                            }

                            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                            StringBuilder sb = new StringBuilder();
                            String line = null;

                            while ((line = bufferedReader.readLine()) != null) {
                                sb.append(line);
                            }

                            bufferedReader.close();
                            Log.d(TAG,"return : "+sb.toString());

                        } catch (Exception e) {
                            Log.d(TAG, "InsertData: Error ", e);
                        }
                    }
                }.start();
            }
        });
        dialog.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        dialog.show();
    }

    public void showHelp(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.help, null);
        builder.setView(view);
        builder.setTitle("도움말");

        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.show();
    }
}