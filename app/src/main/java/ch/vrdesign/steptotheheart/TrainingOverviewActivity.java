package ch.vrdesign.steptotheheart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

public class TrainingOverviewActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_overview);

        EditText txtHrMax = (EditText) findViewById(R.id.hrMaxField);
        EditText txtTrainingZone = (EditText) findViewById(R.id.trainigZoneField);
        EditText txtResultingHeartRate = (EditText) findViewById(R.id.resultingHeartrateField);
        EditText txtTestRunDone = (EditText) findViewById(R.id.testRunDone);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean testRunDone = settings.getBoolean("testRunDone", false);

        if (testRunDone)
            txtTestRunDone.setText("Yes");
        else
            txtTestRunDone.setText("No");

        txtHrMax.setText(Integer.toString(settings.getInt("hrMax", 0)));
        txtTrainingZone.setText(settings.getString("trainingZoneString", "not set"));
        txtResultingHeartRate.setText(settings.getString("resultingHeartrateString", "not set"));
    }

    public void goToScanActivity(View view) {
        if (view.getId() == R.id.buttonTestRun)
            DeviceControlActivity.TestRun = true;
        else if (view.getId() == R.id.buttonLetsGo)
            DeviceControlActivity.TestRun = false;
        Intent intent = new Intent(this, DeviceScanActivity.class);
        startActivity(intent);
    }

    public void goToPlaylistActivity(View view) {
        Intent intent = new Intent(this, PlaylistListActivity.class);
        startActivity(intent);
    }
}