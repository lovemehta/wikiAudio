package com.example.atul.wikiaudio.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.atul.wikiaudio.R;
import com.example.atul.wikiaudio.rest.MediaWikiClient;
import com.example.atul.wikiaudio.rest.ServiceGenerator;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private EditText mUsername, mPassword;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getApplicationContext().getSharedPreferences(
                getString(R.string.pref_file_key),
                Context.MODE_PRIVATE
        );

        if (isLoggedIn())
            launchSearchActivity();

        setContentView(R.layout.activity_start);

        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        Button submit = (Button) findViewById(R.id.submit);

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = mUsername.getText().toString();
                String password = mPassword.getText().toString();
                if (username.isEmpty() || password.isEmpty())
                    Toast.makeText(getApplicationContext(), "Please enter your credentials!",
                            Toast.LENGTH_LONG).show();
                else
                    initiateLogin(username, password);
            }
        });
    }

    private void initiateLogin(final String username, final String password) {
        MediaWikiClient mediaWikiClient = ServiceGenerator.createService(MediaWikiClient.class,
                getApplicationContext());
        Call<ResponseBody> call = mediaWikiClient.getToken("query", "tokens", "login");

        progressDialog = ProgressDialog.show(this, "Log in", "Logging you in...", true);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseStr = response.body().string();
                        String lgToken;
                        JSONObject reader;
                        JSONObject tokenJSONObject;
                        try {
                            reader = new JSONObject(responseStr);
                            tokenJSONObject = reader.getJSONObject("query").getJSONObject("tokens");
                            //noinspection SpellCheckingInspection
                            lgToken = tokenJSONObject.getString("logintoken");
                            completeLogin(username, password, lgToken);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            loginFailed("Server misbehaved! Please try again later.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        loginFailed("Please check your connection!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loginFailed("Please check your connection!");
            }
        });
    }

    private void completeLogin(final String username, final String password, final String token) {
        MediaWikiClient mediaWikiClient = ServiceGenerator.createService(MediaWikiClient.class,
                getApplicationContext());
        Call<ResponseBody> call = mediaWikiClient.login("login", username, password, token);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String responseStr = response.body().string();
                        JSONObject reader;
                        JSONObject loginJSONObject;
                        try {
                            reader = new JSONObject(responseStr);
                            loginJSONObject = reader.getJSONObject("login");
                            String result = loginJSONObject.getString("result");
                            if (result.equals("Success")) {
                                //  Write to shared preferences
                                SharedPreferences.Editor editor = sharedPref.edit();
                                editor.putBoolean(getString(R.string.pref_is_logged_in), true);
                                editor.apply();
                                // Move to new activity
                                launchSearchActivity();
                            } else if (result.equals("Failed")) {
                                loginFailed(loginJSONObject.getString("reason"));
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            loginFailed("Server misbehaved! Please try again later.");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        loginFailed("Please check your connection!");
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(getApplicationContext(),
                        "Please check your connection!",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean isLoggedIn() {
        return sharedPref.getBoolean(getString(R.string.pref_is_logged_in), false);
    }

    private void launchSearchActivity() {
        if (progressDialog != null)
            progressDialog.dismiss();
        Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginFailed(String msg) {
        if (progressDialog != null)
            progressDialog.dismiss();
        Toast.makeText(this, "Login failed!\n" + msg, Toast.LENGTH_LONG).show();
    }
}
