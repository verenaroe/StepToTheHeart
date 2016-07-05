/*
 * Copyright (C) 2013 The Android Open Source Project
 * This software is based on Apache-licensed code from the above.
 *
 * Copyright (C) 2013 APUS
 *
 *     This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.

 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.vrdesign.steptotheheart;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.AsyncTask;
import android.os.Bundle;


import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session.AccessType;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.TrackSimple;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends AppCompatActivity implements PlayerNotificationCallback, ConnectionStateCallback {
    private final static String TAG = DeviceControlActivity.class
            .getSimpleName();

    // BLE stuff
    public static final String EXTRAS_H7_NAME = "DEVICE_NAMEH7";
    public static final String EXTRAS_H7_ADDRESS = "DEVICE_ADDRESSH7";
    public static final String EXTRAS_RUN_NAME = "DEVICE_NAMERUN";
    public static final String EXTRAS_RUN_ADDRESS = "DEVICE_ADDRESSRUN";
    private BluetoothLeService mBluetoothLeServiceH7;
    private BluetoothLeService mBluetoothLeServiceRUN;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristicRUN;
    private BluetoothGattCharacteristic mNotifyCharacteristicH7;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Database
    private EventsDataSource datasource;

    // Dropbox
    private DropboxAPI<AndroidAuthSession> mDBApi;
    private AccessTokenPair dropboxTokens = null;
    final static private String APP_KEY = "tjyi4o6psg0dm0r";
    final static private String APP_SECRET = "jp6054yixb9t9e0";
    final static private AccessType ACCESS_TYPE = AccessType.APP_FOLDER;
    final static private String ACCOUNT_PREFS_NAME = "prefs";
    final static private String ACCESS_KEY_NAME = "ACCESS_KEY";
    final static private String ACCESS_SECRET_NAME = "ACCESS_SECRET";
    private boolean uploadFileRequested = false;

    // Various UI stuff
    public static boolean currentlyVisible;
    private boolean logging = false;
    private TextView mDataFieldHeartRate;
    private TextView mDataFieldCadence;
    private String mH7Name;
    private String mH7Address;
    private String mRUNName;
    private String mRUNAddress;
    private ImageButton mButtonStart;
    private ImageButton mButtonStop;
    private ImageButton mButtonSend;

    // Chart stuff
    private GraphicalView mChart;
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();
    private XYSeries mSeriesHeartRate;
    private XYSeries mSeriesCadence;
    private XYSeriesRenderer mRendererHeartRate;
    private XYSeriesRenderer mRendererCadence;


    private void initChart() {

        Log.i(TAG, "initChart");
        if (mSeriesHeartRate == null) {
            mSeriesHeartRate = new XYSeries("Heart Rate");
            mDataset.addSeries(mSeriesHeartRate);
        }
        if (mSeriesCadence == null) {
            mSeriesCadence = new XYSeries("Steps per Minute");
            mDataset.addSeries(mSeriesCadence);
            Log.i(TAG, "initChart mCurrentSeries == null");
        }

        if (mRendererHeartRate == null) {
            mRendererHeartRate = new XYSeriesRenderer();
            mRendererHeartRate.setLineWidth(4);

            mRendererHeartRate.setPointStyle(PointStyle.CIRCLE);
            mRendererHeartRate.setFillPoints(true);
            mRendererHeartRate.setColor(Color.GREEN);
            Log.i(TAG, "initChart mRendererHeartRate == null");
            if (mRendererCadence == null) {
                mRendererCadence = new XYSeriesRenderer();
                mRendererCadence.setLineWidth(4);

                mRendererCadence.setPointStyle(PointStyle.CIRCLE);
                mRendererCadence.setFillPoints(true);
                mRendererCadence.setColor(Color.GREEN);
                Log.i(TAG, "initChart mRendererCadence == null");
            }

            mRenderer.setAxisTitleTextSize(70);
            mRenderer.setPointSize(5);
            mRenderer.setYTitle("Time");
            mRenderer.setYTitle("Heart rate & Cadence * 2");
            mRenderer.setPanEnabled(true);
            mRenderer.setLabelsTextSize(50);
            mRenderer.setLegendTextSize(50);

            mRenderer.setYAxisMin(0);
            mRenderer.setYAxisMax(120);
            mRenderer.setXAxisMin(0);
            mRenderer.setXAxisMax(100);

            mRenderer.setShowLegend(false);

            mRenderer.setApplyBackgroundColor(true);
            mRenderer.setBackgroundColor(Color.BLACK);
            mRenderer.setMarginsColor(Color.BLACK);

            mRenderer.setShowGridY(true);
            mRenderer.setShowGridX(true);
            mRenderer.setGridColor(Color.WHITE);
            // mRenderer.setShowCustomTextGrid(true);

            mRenderer.setAntialiasing(true);
            mRenderer.setPanEnabled(true, false);
            mRenderer.setZoomEnabled(true, false);
            mRenderer.setZoomButtonsVisible(false);
            mRenderer.setXLabelsColor(Color.WHITE);
            mRenderer.setYLabelsColor(0, Color.WHITE);
            mRenderer.setXLabelsAlign(Align.CENTER);
            mRenderer.setXLabelsPadding(10);
            mRenderer.setXLabelsAngle(-30.0f);
            mRenderer.setYLabelsAlign(Align.RIGHT);
            mRenderer.setPointSize(3);
            mRenderer.setInScroll(true);
            // mRenderer.setShowLegend(false);
            mRenderer.setMargins(new int[]{50, 150, 10, 50});

            mRenderer.addSeriesRenderer(mRendererHeartRate);
            mRenderer.addSeriesRenderer(mRendererCadence);

        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeServiceRUN = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            mBluetoothLeServiceH7 = ((BluetoothLeService.LocalBinder) service)
                    .getService();
            if (!mBluetoothLeServiceH7.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth Service H7");
                finish();
            }
            if (!mBluetoothLeServiceRUN.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth Service RUN");
                finish();
            }
            // Automatically connects to the device upon successful start-up
            // initialization.

            mBluetoothLeServiceH7.connect(mH7Address);
            mBluetoothLeServiceRUN.connect(mRUNAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeServiceH7 = null;
            mBluetoothLeServiceRUN = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiverRUN = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(true);
               // new GetServicesAxync().execute();
                displayGattServices(mBluetoothLeServiceRUN
                        .getSupportedGattServices("RUN"));
                displayGattServices(mBluetoothLeServiceH7
                        .getSupportedGattServices("H7"));
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                mConnected = false;
                updateConnectionState(false);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(mBluetoothLeServiceH7
                        .getSupportedGattServices("H7"));
                displayGattServices(mBluetoothLeServiceRUN
                        .getSupportedGattServices("RUN"));
                // mButtonStop.setVisibility(View.VISIBLE);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayCadence(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_CADENCE));
            }
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device. This can be a
    // result of read
    // or notification operations.
    private final BroadcastReceiver mGattUpdateReceiverH7 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(true);
                displayGattServices(mBluetoothLeServiceH7
                        .getSupportedGattServices("H7"));
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
                    .equals(action)) {
                mConnected = false;
                updateConnectionState(false);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                // Show all the supported services and characteristics on the
                // user interface.
                displayGattServices(mBluetoothLeServiceH7
                        .getSupportedGattServices("H7"));

                // mButtonStop.setVisibility(View.VISIBLE);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayHeartRate(intent.getStringExtra(BluetoothLeService.EXTRA_DATA_HEARTRATE));
            }
        }
    };

    private void clearUI() {
        // mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataFieldHeartRate.setText("---");
        mDataFieldCadence.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Activity spotActivity = this;

        Log.i(TAG, "onCreate");

        setContentView(R.layout.heartrate);

        // getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

        final Intent intent = getIntent();
        mH7Address = intent.getStringExtra(EXTRAS_H7_ADDRESS);
        mH7Name = intent.getStringExtra(EXTRAS_H7_NAME);
        mRUNAddress = intent.getStringExtra(EXTRAS_RUN_ADDRESS);
        mRUNName = intent.getStringExtra(EXTRAS_RUN_NAME);

        // Set up database connection
        datasource = new EventsDataSource(this);
        datasource.open();

        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);

        mDataFieldHeartRate = (TextView) findViewById(R.id.data_value_heartrate);
        mDataFieldCadence = (TextView) findViewById(R.id.data_value_cadence);

        mButtonSend = (ImageButton) findViewById(R.id.btnSend);
        mButtonSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // emailLog();
                if (dropboxTokens == null) {
                    mDBApi.getSession().startAuthentication(
                            DeviceControlActivity.this);
                    uploadFileRequested = true;
                } else {
                    File file = new File(Environment
                            .getExternalStorageDirectory().getPath()
                            + "/hrmlog.csv");
                    UploadFile upload = new UploadFile(
                            DeviceControlActivity.this, mDBApi, "/", file);
                    upload.execute();
                }
            }
        });

        mButtonStart = (ImageButton) findViewById(R.id.btnStart);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startLogging();
                SpotifyActivity spot = new SpotifyActivity(spotActivity);
            }
        });

        mButtonStop = (ImageButton) findViewById(R.id.btnStop);
        mButtonStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopLogging();
            }
        });


        getSupportActionBar().setTitle("Do this");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        // TODO: Lars added this
        this.startService(gattServiceIntent);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        LinearLayout layout = (LinearLayout) findViewById(R.id.chart);
        if (mChart == null) {
            initChart();
            mChart = ChartFactory.getTimeChartView(this, mDataset, mRenderer,
                    "hh:mm");
            layout.addView(mChart);
        } else {
            mChart.repaint();
        }

        verifyStoragePermissions(this);
    }

    private static final int REQUEST_CODE = 1337;
    String myAccessToken = "myAccessToken";
    private Player mPlayer;
    private Pager<PlaylistSimple> playlists;
    private Pager<TrackSimple> tracks;

    private static final String CLIENT_ID = "fe5fc16f32a742deb6ba3241d93852aa";
    private static final String REDIRECT_URI = "ch.vrdesign.steptotheheart://callback";

    private static final List<String> TEST_ALBUM_TRACKS = Arrays.asList(
            "spotify:track:2To3PTOTGJUtRsK3nQemP4",
            "spotify:track:0tDoBMgyAzGgLhs73KPrJL",
            "spotify:track:5YkSQuB8i7J4TTyj0xw6ol",
            "spotify:track:3WpLfCkrlQxj8SISLzhs06",
            "spotify:track:2lGNTC3NKCG1d4lR8x3611",
            "spotify:track:0kdSj5REwpHjTBaBsm1wv8",
            "spotify:track:3BgnZiGnnRlXfeGR8ryKzT",
            "spotify:track:00cVWQIFyQnIdsgoVy7qAG",
            "spotify:track:6eEEoowHpnaD3q83ZhYmhZ",
            "spotify:track:1HFBn8S30ndZ7lLb9HbENU",
            "spotify:track:1I9VibKgJTqGfrh8fEK3sL",
            "spotify:track:6rXSPMgGIyOYiMhsj3eSAi",
            "spotify:track:2xwuXthwdNGbPyEqifPQNW",
            "spotify:track:5vRuWI48vKn4TV7efrYtJL",
            "spotify:track:4SEDYSBDd4Ota125LjHa2w",
            "spotify:track:2bVTnSTjLWAizyj4XcU5bf",
            "spotify:track:4gQzqlFuqv6l4Ka633Ue7T",
            "spotify:track:0SLVmM7IrrtkPNa1Fi3IKT"
    );

    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                myAccessToken = response.getAccessToken();

                new RetrieveFeedTask().execute();

                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                mPlayer = Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                    @Override
                    public void onInitialized(Player player) {
                        mPlayer.addConnectionStateCallback(DeviceControlActivity.this);
                        mPlayer.addPlayerNotificationCallback(DeviceControlActivity.this);
                        mPlayer.play(TEST_ALBUM_TRACKS);

                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Checks if the app has permission to write to device storage
     * <p/>
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        currentlyVisible = true;

        registerReceiver(mGattUpdateReceiverH7, makeGattUpdateIntentFilter());
        registerReceiver(mGattUpdateReceiverRUN, makeGattUpdateIntentFilter());


        if (mBluetoothLeServiceH7 != null) {

            final boolean resultH7 = mBluetoothLeServiceH7.connect(mH7Address);

            Log.d(TAG, "Connect request resultH7=" + resultH7);

        }
        if (mBluetoothLeServiceRUN != null) {

            final boolean resultRUN = mBluetoothLeServiceRUN.connect(mRUNAddress);

            Log.d(TAG, "Connect request resultRUN=" + resultRUN);

        }

        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Mandatory call to complete the auth
                mDBApi.getSession().finishAuthentication();

                // Store it locally in our app for later use
                dropboxTokens = mDBApi.getSession().getAccessTokenPair();
                storeKeys(dropboxTokens.key, dropboxTokens.secret);

                if (uploadFileRequested) {
                    dropboxUpload();
                }

            } catch (IllegalStateException e) {
                // showToast("Couldn't authenticate with Dropbox:" +
                // e.getLocalizedMessage());
                Log.i(TAG, "Error authenticating", e);
            }
        }
    }

    // this is called when the screen rotates.
    // (onCreate is no longer called when screen rotates due to manifest, see:
    // android:configChanges)
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // setContentView(R.layout.heartrate);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.i(TAG, "ORIENTATION_LANDSCAPE");
        } else {
            Log.i(TAG, "ORIENTATION_PORTRAIT");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        currentlyVisible = false;
        // unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentlyVisible = false;
        unbindService(mServiceConnection);
        unregisterReceiver(mGattUpdateReceiverH7);
        unregisterReceiver(mGattUpdateReceiverRUN);
        // mBluetoothLeServiceH7 = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
            if (logging) {
                menu.findItem(R.id.menu_start_logging).setVisible(false);
                menu.findItem(R.id.menu_stop_logging).setVisible(true);
                menu.findItem(R.id.menu_dropbox).setVisible(true);
                mButtonStart.setVisibility(View.GONE);
                mButtonStop.setVisibility(View.VISIBLE);
            } else {
                menu.findItem(R.id.menu_start_logging).setVisible(true);
                menu.findItem(R.id.menu_stop_logging).setVisible(false);
                mButtonStart.setVisibility(View.VISIBLE);
                mButtonStop.setVisibility(View.GONE);
            }
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
            menu.findItem(R.id.menu_start_logging).setVisible(false);
            menu.findItem(R.id.menu_stop_logging).setVisible(false);
            menu.findItem(R.id.menu_dropbox).setVisible(false);
            mButtonStart.setVisibility(View.GONE);
            mButtonStop.setVisibility(View.GONE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeServiceRUN.connect(mRUNAddress);
                mBluetoothLeServiceH7.connect(mH7Address);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeServiceRUN.disconnect();
                mBluetoothLeServiceH7.disconnect();
                return true;
            case R.id.menu_dropbox:
                dropboxUpload();
                return true;
            case R.id.menu_start_logging:
                startLogging();
                return true;
            case R.id.menu_stop_logging:
                stopLogging();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connected) {
                    mButtonStart.setVisibility(View.VISIBLE);
                    mButtonStop.setVisibility(View.GONE);
                } else {
                    mButtonStart.setVisibility(View.GONE);
                    mButtonStop.setVisibility(View.GONE);
                }
            }
        });
    }

    int x = 0;

    private void displayHeartRate(String heartRate) {
        try {
            if (heartRate != null) {

                long time = (new Date()).getTime();

                int dataElementHR = Integer.parseInt(heartRate);

                mSeriesHeartRate.add(time, dataElementHR);

                appendLog(("Heartrate: " + new Date()).toString() + "," + heartRate);

                while (mSeriesHeartRate.getItemCount() > 60 * 10) {
                    mSeriesHeartRate.remove(0);
                }


                if (currentlyVisible) {
                    mDataFieldHeartRate.setText("Pulse: " + heartRate);


                    mRenderer.setYAxisMin(0);
                    mRenderer.setYAxisMax(mSeriesHeartRate.getMaxY() + 20);

                    double minx = mSeriesHeartRate.getMinX();
                    double maxx = mSeriesHeartRate.getMaxX();

                    if ((maxx - minx) < 5 * 60 * 1000) {
                        mRenderer.setXAxisMin(minx);
                        mRenderer.setXAxisMax(minx + (5 * 60 * 1000));
                    } else {
                        mRenderer.setXAxisMin(maxx - (5 * 60 * 1000));
                        mRenderer.setXAxisMax(maxx);
                    }

                    mChart.repaint();
                    mChart.zoomReset();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing heartrate: " + heartRate);
        }
    }

    private void displayCadence(String cadence) {
        try {
            if (cadence != null) {

                long time = (new Date()).getTime();

                int dataElementCadence = Integer.parseInt(cadence);
                mSeriesCadence.add(time, dataElementCadence);
                appendLog(("Cadence: " + new Date()).toString() + "," + cadence);
                //datasource.createEvent(1, time, dataElement);
                // Storing last 600 only - should average...

                while (mSeriesCadence.getItemCount() > 60 * 10) {
                    mSeriesCadence.remove(0);
                }

                if (currentlyVisible) {
                    mDataFieldCadence.setText("Cadence: " + cadence);


                    mRenderer.setYAxisMin(0);
                    mRenderer.setYAxisMax(mSeriesCadence.getMaxY() + 20);

                    double minx = mSeriesCadence.getMinX();
                    double maxx = mSeriesCadence.getMaxX();

                    if ((maxx - minx) < 5 * 60 * 1000) {
                        mRenderer.setXAxisMin(minx);
                        mRenderer.setXAxisMax(minx + (5 * 60 * 1000));
                    } else {
                        mRenderer.setXAxisMin(maxx - (5 * 60 * 1000));
                        mRenderer.setXAxisMax(maxx);
                    }

                    mChart.repaint();
                    mChart.zoomReset();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception while parsing cadence: " + cadence);
        }
    }


    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        String unknownServiceString = getResources().getString(
                R.string.unknown_service);
        String unknownCharaString = getResources().getString(
                R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME,
                    SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService
                    .getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {

                if (UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT)
                        .equals(gattCharacteristic.getUuid())) {
                    Log.d(TAG, "Found heart rate");
                    mNotifyCharacteristicH7 = gattCharacteristic;
                } else if (UUID.fromString(SampleGattAttributes.RUNNINGSPEEDANDCADENCE_MEASUREMENT).equals(gattCharacteristic.getUuid())) {
                    Log.d(TAG, "Found Running Speed and Cadence");
                    mNotifyCharacteristicRUN = gattCharacteristic;
                }

                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME,
                        SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter
                .addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private String[] getKeys() {
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key != null && secret != null) {
            String[] ret = new String[2];
            ret[0] = key;
            ret[1] = secret;
            return ret;
        } else {
            return null;
        }
    }

    private void storeKeys(String key, String secret) {
        // Save the access key for later
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        Editor edit = prefs.edit();
        edit.putString(ACCESS_KEY_NAME, key);
        edit.putString(ACCESS_SECRET_NAME, secret);
        edit.commit();
    }

    private AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session;

        String[] stored = getKeys();
        if (stored != null) {
            AccessTokenPair accessToken = new AccessTokenPair(stored[0],
                    stored[1]);
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE,
                    accessToken);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }

    private void dropboxUpload() {
        File file = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/hrmlog.csv");
        UploadFile upload = new UploadFile(DeviceControlActivity.this, mDBApi,
                "/", file);
        upload.execute();
        uploadFileRequested = false;
    }

    public void appendLog(String text) {
        File logFile = new File(Environment.getExternalStorageDirectory()
                .getPath() + "/hrmlog.csv");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                Log.e(TAG, "Error while creating file. ", e);
                e.printStackTrace();
            }
        }
        try {
            // BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile,
                    true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startLogging() {
        mButtonSend.setVisibility(View.VISIBLE);
        mButtonStop.setVisibility(View.VISIBLE);
        mButtonStart.setVisibility(View.GONE);
        new AsyncReadOut().execute();
        mBluetoothLeServiceRUN.setCharacteristicNotification(
                mNotifyCharacteristicRUN, true);
        invalidateOptionsMenu();
        logging = true;
    }

    private void stopLogging() {
        mButtonStop.setVisibility(View.GONE);
        mButtonStart.setVisibility(View.VISIBLE);
        mBluetoothLeServiceH7.setCharacteristicNotification(
                mNotifyCharacteristicH7, false);
        mBluetoothLeServiceRUN.setCharacteristicNotification(
                mNotifyCharacteristicRUN, false);
        invalidateOptionsMenu();
        logging = false;
    }

    @Override
    public void onLoggedIn() {
        Log.d("MainActivity", "User logged in");
    }

    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }

    @Override
    public void onLoginFailed(Throwable error) {
        Log.d("MainActivity", "Login failed");
        Log.d("MainActivity", error.getMessage());
    }

    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }

    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {
        Log.d("MainActivity", "Playback event received: " + eventType.name());
        switch (eventType) {
            // Handle event type as necessary
            default:
                break;
        }
    }

    @Override
    public void onPlaybackError(ErrorType errorType, String errorDetails) {
        Log.d("MainActivity", "Playback error received: " + errorType.name());
        switch (errorType) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    class AsyncReadOut extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            mBluetoothLeServiceH7.setCharacteristicNotification(
                    mNotifyCharacteristicH7, true);
            return null;
        }
    }

    class RetrieveFeedTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                SpotifyApi api = new SpotifyApi();

// Most (but not all) of the Spotify Web API endpoints require authorisation.
// If you know you'll only use the ones that don't require authorisation you can skip this step
                api.setAccessToken(myAccessToken);

                SpotifyService spotify = api.getService();
                playlists = spotify.getMyPlaylists();
                for (PlaylistSimple playlist : playlists.items
                        ) {
                    Log.i("main", playlist.name);
                    if (playlist.name.equals("run today"))
                        mPlayer.play(playlist.uri);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}