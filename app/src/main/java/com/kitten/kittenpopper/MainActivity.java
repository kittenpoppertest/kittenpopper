package com.kitten.kittenpopper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import layout.KittenPoppingService;

public class MainActivity extends AppCompatActivity {

    private static final int MINIMUM_ANDROID_API_VERSION = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();

        new SettingsFetcher().execute();

        setUserInterface();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= MINIMUM_ANDROID_API_VERSION) {
            if (!Settings.canDrawOverlays(getBaseContext())) {
                Intent permissionIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(permissionIntent);
            }
        }
    }

    private class SettingsFetcher extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            String settings = fetchSettingsFromServer();

            SharedPreferences preferences = getSharedPreferences(getString(R.string.preference_file), MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(getString(R.string.received_settings), settings);
            editor.apply();

            return null;
        }

        private String fetchSettingsFromServer() {
            String settings = null;

            try {
                URL url = new URL(getString(R.string.setting_server_url));
                URLConnection urlConnection = url.openConnection();

                HttpURLConnection httpURLConnection = (HttpURLConnection)urlConnection;
                httpURLConnection.setAllowUserInteraction(false);
                httpURLConnection.setInstanceFollowRedirects(true);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();

                InputStream inputStream = httpURLConnection.getInputStream();
                settings = convertInputStreamToString(inputStream);
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Error fetching settings from server", Toast.LENGTH_SHORT).show();
            }

            return settings;
        }

        private String convertInputStreamToString(InputStream inputStream) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            try {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append('\n');
                }
            } catch (IOException e) {
            } finally {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }

            return stringBuilder.toString();
        }

    }

    private void setUserInterface() {
        Button button = (Button)findViewById(R.id.start_popup_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getSharedPreferences(getString(R.string.preference_file), MODE_PRIVATE);
                if (preferences.getString(getString(R.string.received_settings), null) == null) {
                    return;
                }

                Intent serviceIntent = new Intent(getBaseContext(), KittenPoppingService.class);
                startService(serviceIntent);

                finishAndRemoveTask();
            }
        });
    }
}
