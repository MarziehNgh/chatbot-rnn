package com.example.mohammad.myapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {


    ImageView startRecording;
    WavRecorder wavRecorder;
    String my_path;
    TextView recording_state_tv, chat_tv;
    boolean recording = false;
    EditText server_url_ed;
    String serverResponseMessage;
    BottomNavigationView navigation;
    LinearLayout home, dashboard, notifications;
    public TextToSpeech t1;

//    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();


    public void toggle(){
        if (recording){
            startRecording.setImageResource(R.drawable.ic_microphone_none);
            recording = false;
            recording_state_tv.setText("ready!");
            wavRecorder.stopRecording();
            new UploadFileAsync().execute("");
        }else{
            startRecording.setImageResource(R.drawable.ic_microphone_recording);
            recording = true;
            recording_state_tv.setText("recording");
            wavRecorder.startRecording();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startRecording = findViewById(R.id.start_record_btn);
        recording_state_tv = findViewById(R.id.recording_state_tv);
        chat_tv = findViewById(R.id.chat_tv);
        server_url_ed = findViewById(R.id.server_url);
        home = findViewById(R.id.home);
        dashboard = findViewById(R.id.dashboard);
        notifications = findViewById(R.id.notifications);
//        StrictMode.setThreadPolicy(policy);
        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {

            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        home.setVisibility(View.VISIBLE);
                        dashboard.setVisibility(View.GONE);
                        notifications.setVisibility(View.GONE);
                        return true;
                    case R.id.navigation_dashboard:
                        home.setVisibility(View.GONE);
                        dashboard.setVisibility(View.VISIBLE);
                        notifications.setVisibility(View.GONE);
                        return true;
                    case R.id.navigation_notifications:
                        home.setVisibility(View.GONE);
                        dashboard.setVisibility(View.GONE);
                        notifications.setVisibility(View.VISIBLE);
                        return true;
                }
                server_url_ed.setText(Config.SERVER_URL);
                return false;
            }
        });


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        server_url_ed.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Config.SERVER_URL = s.toString();
            }
        });

        my_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AudioRecording.wav";
        wavRecorder = new WavRecorder(my_path);

        startRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggle();
            }
        });

    }

    private class UploadFileAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {

            try {
                String sourceFileUri = my_path;

                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(sourceFileUri);

                if (sourceFile.isFile()) {

                    try {
                        String upLoadServerUri = Config.SERVER_URL;

                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(
                                sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("bill", sourceFileUri);

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                                + sourceFileUri + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math
                                    .min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0,
                                    bufferSize);

                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);

                        // Responses from the server (code and message)
                        int serverResponseCode = conn.getResponseCode();
                        serverResponseMessage = conn
                                .getResponseMessage();
//                        Log.e("MGH", "doInBackground: "+serverResponseMessage + serverResponseCode);

                        if (serverResponseCode == 200) {

//                            MGH added todo
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(conn.getInputStream()));
                            String inputLine;
                            StringBuffer response = new StringBuffer();

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();
                            serverResponseMessage = response.toString();
                            // messageText.setText(msg);
                            //Toast.makeText(ctx, "File Upload Complete.",
                            //      Toast.LENGTH_SHORT).show();

                            // recursiveDelete(mDirectory1);



                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();

                    } catch (Exception e) {

                        // dialog.dismiss();
                        e.printStackTrace();

                    }
                    // dialog.dismiss();

                } // End else block


            } catch (Exception ex) {
                // dialog.dismiss();

                ex.printStackTrace();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            try {
//                Toast.makeText(MainActivity.this, ""+serverResponseMessage, Toast.LENGTH_SHORT).show();

                JSONObject jsonObject = new JSONObject(serverResponseMessage);
                String text = chat_tv.getText() + "\nyou: " + jsonObject.getString("request") + "\nbot: " + jsonObject.getString("response");
                chat_tv.setText(text);
                startRecording.setEnabled(true);
                t1.speak(jsonObject.getString("response"), TextToSpeech.QUEUE_FLUSH, null);

            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "something went wrong!!!", Toast.LENGTH_SHORT).show();
                startRecording.setEnabled(true);
                e.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            startRecording.setEnabled(false);
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }




}
