package ch.vrdesign.steptotheheart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;

public class ChooseTargetActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_target);
    }

    public void goToOverview(View view) {

        setSettings();

        Intent intent = new Intent(this, TrainingOverviewActivity.class);
        startActivity(intent);
    }

    private void setSettings() {

        // get settings

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        // save trainigzone boundaries to settings

        int maxHr = settings.getInt("hrMax", 220);
        int zone1Bottom = maxHr * 50 / 100;
        int zone1Top = maxHr * 60 / 100;
        int zone2Top = maxHr * 70 / 100;
        int zone3Top = maxHr * 80 / 100;
        editor.putInt("zone1Bottom", zone1Bottom);
        editor.putInt("zone1Top", zone1Top);
        editor.putInt("zone2Top", zone2Top);
        editor.putInt("zone3Top", zone3Top);

        // get choosen trainingszone

        RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.radioGroup);
        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        View radioButton = radioButtonGroup.findViewById(radioButtonID);
        int index = radioButtonGroup.indexOfChild(radioButton);
        switch (index) {
            case 1:
                editor.putString("trainingZoneString", "50 - 60% HRMax");
                editor.putString("resultingHeartrateString", String.format("%d - %d", zone1Bottom, zone1Top));
                editor.putInt("bottomLimit", zone1Bottom);
                editor.putInt("topLimit", zone1Top);
                editor.putInt("choosenTrainingzone", 1);
                editor.putInt("playListInitialTempo", settings.getInt("zone1Tempo", 0));
                break;
            case 2:
                editor.putString("trainingZoneString", "60 - 70% HRMax");
                editor.putString("resultingHeartrateString", String.format("%d - %d", zone1Top, zone2Top));
                editor.putInt("bottomLimit", zone1Top);
                editor.putInt("topLimit", zone2Top);
                editor.putInt("choosenTrainingzone", 2);
                editor.putInt("playListInitialTempo", settings.getInt("zone2Tempo", 0));
                break;
            case 3:
                editor.putString("trainingZoneString", "70 - 80% HRMax");
                editor.putString("resultingHeartrateString", String.format("%d - %d", zone2Top, zone3Top));
                editor.putInt("bottomLimit", zone2Top);
                editor.putInt("topLimit", zone3Top);
                editor.putInt("choosenTrainingzone", 3);
                editor.putInt("playListInitialTempo", settings.getInt("zone3Tempo", 0));
                break;
        }
        editor.commit();
    }
}
