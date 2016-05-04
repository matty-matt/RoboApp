package roboniania.com.roboniania_android.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import roboniania.com.roboniania_android.R;
import roboniania.com.roboniania_android.adapter.model.Edu;

public class EduActivity extends AppCompatActivity {

    public static final String EDU_EXTRA_KEY = "edu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edu);

        Intent i = getIntent();
        Edu edu = (Edu) i.getExtras().getSerializable(EDU_EXTRA_KEY);
        showEdu(edu);
    }

    private void showEdu(Edu edu) {
        TextView title = (TextView) findViewById(R.id.eduDetailTitle);
        ImageView photo = (ImageView) findViewById(R.id.eduDetailIcon);

        title.setText(edu.getTitle());
        photo.setImageResource(edu.getIconId());
    }
}
