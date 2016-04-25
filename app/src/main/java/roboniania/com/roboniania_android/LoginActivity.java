package roboniania.com.roboniania_android;

import android.app.Activity;
import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import roboniania.com.roboniania_android.api.RoboService;
import roboniania.com.roboniania_android.api.model.OAuthToken;
import roboniania.com.roboniania_android.storage.SharedPreferenceStorage;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REGISTER_REQUEST = 1;
    private final String url = "localhost";
    private SharedPreferenceStorage userLocalStorage;

    Button loginBtn;
    TextView signupLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Initialize components
        loginBtn = (Button) findViewById(R.id.login_button);
        loginBtn.setOnClickListener(this);

        signupLink = (TextView) findViewById(R.id.signup_link);
        signupLink.setOnClickListener(this);

        userLocalStorage = new SharedPreferenceStorage(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.login_button:
                login();
                sendResultForMainActivity();
                break;
            case R.id.signup_link:
                startRegisterActivity();
                break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REGISTER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, R.string.account_created, Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                Log.e("ERR", "User went back to login form.");
            }
        }
    }

    private void sendResultForMainActivity() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void startRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivityForResult(intent, REGISTER_REQUEST);
    }

    private void login() {
        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();


        RoboService roboService = retrofit.create(RoboService.class);



        Call<OAuthToken> call = roboService.getToken("janusz", "1234");

        call.enqueue(new Callback<OAuthToken>() {
            @Override
            public void onResponse(Call<OAuthToken> call, Response<OAuthToken> response) {
                if (response.isSuccessful()) {
                    int statusCode = response.code();
                    OAuthToken accessToken = response.body();

                    // Save token to shared prefernces
                    userLocalStorage.storeAccessToken(accessToken.getAccess_token());
                    userLocalStorage.setUserLoggedIn(true);

                    Intent returnIntent = new Intent();
                    setResult(Activity.RESULT_OK, returnIntent);

                    System.out.println(statusCode);
                    System.out.println(accessToken.getAccess_token());

//                    finish(); // Finish Activity

                } else {
//                    Snackbar snackbar = Snackbar
//                            .make(coordinatorLayout, "Blad logowania!", Snackbar.LENGTH_LONG);
//                    snackbar.show();
                    System.out.println("HIUSTON MAMY PROBLEM");
                    //TODO catch code error
                }
            }

            @Override
            public void onFailure(Call<OAuthToken> call, Throwable t) {
//                Snackbar snackbar = Snackbar
//                        .make(coordinatorLayout, "Problem z nawiazaniem polaczenia.", Snackbar.LENGTH_LONG);
//                snackbar.show();
                t.printStackTrace();
            }

        });
    }



}
