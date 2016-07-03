package ch.vrdesign.steptotheheart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        EditText txtHrMax = (EditText) findViewById(R.id.hrMaxField);

        int hrMax = settings.getInt("hrMax", 220);
        if (hrMax < 220)
        {
            txtHrMax.setText(Integer.toString(hrMax));
            return;
        }
    }

    public void calculateHRMax(View view){
        EditText txtage = (EditText)findViewById(R.id.ageField);

        // check if age is valid an send error message if not

        // Restore preferences

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        int age = Integer.valueOf(txtage.getText().toString());
        int calcHrMax = 220 - age;

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("hrMax", calcHrMax);

        editor.commit();
        EditText txtHrMax = (EditText) findViewById(R.id.hrMaxField);
        txtHrMax.setText(Integer.toString(calcHrMax));

    }

    public void goToChooseTarget(View view){
        Intent intent = new Intent(this, ChooseTargetActivity.class);
        startActivity(intent);
    }
}
