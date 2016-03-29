package roboniania.com.roboniania_android;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    private static final int LOGIN_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (isAuthenticated()) {
            startHomeActivity();
        } else {
            startLoginActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == LOGIN_REQUEST) {
            if (resultCode == RESULT_OK) {
                startHomeActivity();
            }
            if (resultCode == RESULT_CANCELED) {
                Log.e("ERR", "Error during logging in.");
            }
        }
    }

    private boolean isAuthenticated() {
        return false;
    }

    private void startHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void startLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivityForResult(intent, LOGIN_REQUEST);
    }
}
