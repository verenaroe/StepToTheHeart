package ch.vrdesign.steptotheheart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

public class ChooseTargetActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "MyPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_target);
    }

    public void goToOverview(View view){

        setSettings();

        Intent intent = new Intent(this, TrainingOverviewActivity.class);
        startActivity(intent);
    }

    private void setSettings(){
        // save index of trainingzone to settings
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        RadioGroup radioButtonGroup = (RadioGroup) findViewById(R.id.radioGroup);

        int radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
        View radioButton = radioButtonGroup.findViewById(radioButtonID);
        int index = radioButtonGroup.indexOfChild(radioButton);

        int bottomLimit = 0;
        int topLimit = 0;
        int maxHr = settings.getInt("hrMax", 220);
        switch (index){
            case 1:
                bottomLimit = maxHr * 50 / 100;
                topLimit = maxHr * 60 / 100;
                editor.putString("trainingZoneString", "50 - 60% HRMax");
                break;
            case 2:
                bottomLimit = maxHr * 60 / 100;
                topLimit = maxHr * 70 / 100;
                editor.putString("trainingZoneString", "60 - 70% HRMax");
                break;
            case 3:
                bottomLimit = maxHr * 70 / 100;
                topLimit = maxHr * 80 / 100;
                editor.putString("trainingZoneString", "70 - 80% HRMax");
                break;

        }
        editor.putInt("bottomLimit", bottomLimit);
        editor.putInt("topLimit", topLimit);
        editor.putString("resultingHeartrateString", String.format("%d - %d", bottomLimit, topLimit));
        editor.commit();
    }
}
