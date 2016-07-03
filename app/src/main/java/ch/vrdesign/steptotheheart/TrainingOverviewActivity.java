package ch.vrdesign.steptotheheart;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class TrainingOverviewActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";
    private ImageButton mButtonPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_overview);

        EditText txtHrMax = (EditText) findViewById(R.id.hrMaxField);
        EditText txtTrainingZone = (EditText) findViewById(R.id.trainigZoneField);
        EditText txtResultingHeartRate = (EditText) findViewById(R.id.resultingHeartrateField);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        boolean testRunDone = settings.getBoolean("testRunDone", false);

        txtHrMax.setText(Integer.toString(settings.getInt("hrMax", 0)));
        txtTrainingZone.setText(settings.getString("trainingZoneString", "not set"));
        txtResultingHeartRate.setText(settings.getString("resultingHeartrateString", "not set"));

        if (testRunDone == false) {
            Button theButton = (Button) findViewById(R.id.buttonLetsGo);
            theButton.setVisibility(View.VISIBLE);
            theButton.setBackgroundColor(Color.TRANSPARENT);
        }

        mButtonPlay = (ImageButton) findViewById(R.id.btnPlay);
        mButtonPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startMusic();
            }
        });
    }

    public void goToScanActivity(View view){
        Intent intent = new Intent(this, DeviceScanActivity.class);
        startActivity(intent);
    }

    private void startMusic() {
        Intent intent = new Intent(this, SpotifyActivity.class);
        startActivity(intent);
    }

    public static void verifyInternetPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.INTERNET);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[] {Manifest.permission.INTERNET},
                    1

            );
        }
    }

}

    // je nach trainingszone usrechne wie viell
    // checke ob testlauf scho gsi isch, isch setting ume suscht TESTLAUF
    // wenn setting ume los los los

