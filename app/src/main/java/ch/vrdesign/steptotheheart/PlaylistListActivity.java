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

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.SavedTrack;

import static android.widget.AdapterView.*;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class PlaylistListActivity extends AppCompatActivity {

    ArrayList<String> playListNames = new ArrayList<>();
    private static final int REQUEST_CODE = 1337;
    String myAccessToken = "myAccessToken";
    String choosenPlaylistFromList = "";
    private String playListid80 = "";
    private String playListid90 = "";
    private String playListid100 = "";
    private String playListid110 = "";
    private String playListid120 = "";
    private String playListid130 = "";
    private String playListid140 = "";
    private String playListid150 = "";
    private String playListuri80 = "";
    private String playListuri90 = "";
    private String playListuri100 = "";
    private String playListuri110 = "";
    private String playListuri120 = "";
    private String playListuri130 = "";
    private String playListuri140 = "";
    private String playListuri150 = "";

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    IntentFilter filter = new IntentFilter();
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ListView listView;
    private boolean polarH7Detected = false;
    private boolean polarRunDetected = false;
    private BluetoothDevice polarRun = null;
    private BluetoothDevice polarH7 = null;

    /* // Device scan callback.
     private BluetoothAdapter.LeScanCallback mLeScanCallback =
             new BluetoothAdapter.LeScanCallback() {

                 @Override
                 public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                     runOnUiThread(new Runnable() {
                         @Override
                         public void run() {
                             mLeDeviceListAdapter.addDevice(device);
                             mLeDeviceListAdapter.notifyDataSetChanged();
                         }
                     });
                 }
             };
 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_list);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        listView = (ListView) findViewById(R.id.list);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                new AddPlaylistTracksToBpmPlaylists().execute(mLeDeviceListAdapter.getDevice(position));
            }
        });


        getSupportActionBar().setTitle("List of spotify Playlists");
        mHandler = new Handler();
final Activity spotActivity = this;

        SpotifyAuthenticationActivity spot = new SpotifyAuthenticationActivity(spotActivity);


        //registerReceiver(mReceiver, filter);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                break;
            case R.id.menu_stop:
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.

        // Initializes list view adapter.
        //mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_CODE) {

            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                myAccessToken = response.getAccessToken();

                try {
                    List<String> list = new SetPlaylistIds().execute().get();
                    for (String name: list
                         ) {
                        mLeDeviceListAdapter.addDevice(name);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                mLeDeviceListAdapter.notifyDataSetChanged();

            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLeDeviceListAdapter.clear();
    }

    @Override
    public void onDestroy() {
        //unregisterReceiver(mReceiver);
        super.onDestroy();
    }

   /* private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                //discovery starts, we can show progress dialog or perform other tasks
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //discovery finishes, dismis progress dialog
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //bluetooth device found
                BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                Log.i("AG", "Found device " + device.getName());
                if (device.getName() == null) return;
                if (device.getName().contains("Polar H7")) {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    polarH7Detected = true;
                    polarH7 = device;
                } else if (device.getName().contains("Polar RUN")) {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    polarRunDetected = true;
                    polarRun = device;
                }

                if (polarH7Detected && polarRunDetected) {
                    if (polarH7 == null || polarRun == null) return;
                    final Intent controlIntent = new Intent(context, DeviceControlActivity.class);
                    controlIntent.putExtra(DeviceControlActivity.EXTRAS_RUN_NAME, polarRun.getName());
                    controlIntent.putExtra(DeviceControlActivity.EXTRAS_RUN_ADDRESS, polarRun.getAddress());
                    controlIntent.putExtra(DeviceControlActivity.EXTRAS_H7_NAME, polarH7.getName());
                    controlIntent.putExtra(DeviceControlActivity.EXTRAS_H7_ADDRESS, polarH7.getAddress());
                    if (mScanning) {
                        mBluetoothAdapter.cancelDiscovery();
                        mScanning = false;
                    }
                    startActivity(controlIntent);

                }
            }
        }
    };*/

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<String> playLists;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            playLists = new ArrayList<>();
            mInflator = PlaylistListActivity.this.getLayoutInflater();
        }

        public void addDevice(String name) {
            if (!playLists.contains(name)) {
                playLists.add(name);
                //mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        public String getDevice(int position) {
            return playLists.get(position);
        }

        public void clear() {
            playLists.clear();
        }

        @Override
        public int getCount() {
            return playLists.size();
        }

        @Override
        public Object getItem(int i) {
            return playLists.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                //view = listView;
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            String device = playLists.get(i);
            //final String deviceName = device.getName();
            //if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(device);
            //else
            //    viewHolder.deviceName.setText(R.string.unknown_device);
            //viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }

    }


    class AddPlaylistTracksToBpmPlaylists extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {

            SpotifyApi api = new SpotifyApi();

            // Most (but not all) of the Spotify Web API endpoints require authorisation.
            // If you know you'll only use the ones that don't require authorisation you can skip this step
            api.setAccessToken(myAccessToken);

            SpotifyService spotify = api.getService();

            Map<String, Object> trackUris = new ArrayMap<>();
            Map<String, Object> body = new ArrayMap<>();
            Map<String, Object> queryMap = new ArrayMap<>();

            String playlistId = getPlaylistIdForPlaylist(spotify.getMyPlaylists(), params[0]);

            Pager<PlaylistTrack> tracks = spotify.getPlaylistTracks(spotify.getMe().id, playlistId);


            for (int i = 0; i < tracks.total; i += 50) {
                queryMap.clear();
                queryMap.put("limit", 50);
                queryMap.put("offset", i);
                tracks = spotify.getPlaylistTracks(spotify.getMe().id, playlistId, queryMap);

                for (PlaylistTrack track : tracks.items) {
                    AudioFeaturesTrack features = spotify.getTrackAudioFeatures(track.track.id);
                    int tempoOfCurrentTrack = Math.round(features.tempo);
                    int tempoRoundToTen = Math.round((tempoOfCurrentTrack / 10) * 10);
                    trackUris.put("uris", track.track.uri);
                    if (tempoRoundToTen >= 80 && tempoRoundToTen <= 90)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid80, trackUris, trackUris);
                    if (tempoRoundToTen >= 91 && tempoRoundToTen <= 100)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid90, trackUris, trackUris);
                    if (tempoRoundToTen >= 101 && tempoRoundToTen <= 110)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid100, trackUris, trackUris);
                    if (tempoRoundToTen >= 111 && tempoRoundToTen <= 120)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid110, trackUris, body);
                    if (tempoRoundToTen >= 121 && tempoRoundToTen <= 130)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid120, trackUris, trackUris);
                    if (tempoRoundToTen >= 131 && tempoRoundToTen <= 140)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid130, trackUris, trackUris);
                    if (tempoRoundToTen >= 141 && tempoRoundToTen <= 150)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid140, trackUris, trackUris);
                    if (tempoRoundToTen >= 151 && tempoRoundToTen <= 160)
                        spotify.addTracksToPlaylist(spotify.getMe().id, playListid150, trackUris, trackUris);
                }
            }
            return null;
        }

        private String getPlaylistIdForPlaylist(Pager<PlaylistSimple> playlists, String name) {
            for (PlaylistSimple playlist : playlists.items
                    ) {
                if (!playlist.name.equals(name))
                    continue;

                return playlist.id;
            }
            return "not found";
        }
    }
    // get spotify playlists and show in listview
    // on click on list item save to specific playlist

    class SetPlaylistIds extends AsyncTask<Void, Void, List<String>> {

        @Override
        protected List<String> doInBackground(Void... params) {

            SpotifyApi api = new SpotifyApi();

            // Most (but not all) of the Spotify Web API endpoints require authorisation.
            // If you know you'll only use the ones that don't require authorisation you can skip this step
            api.setAccessToken(myAccessToken);

            SpotifyService spotify = api.getService();
            Pager<PlaylistSimple> playlists = spotify.getMyPlaylists();
List<String> list = new ArrayList<>();

            for (PlaylistSimple playlist : playlists.items
                    ) {
                list.add(playlist.name);
               // mLeDeviceListAdapter.notifyDataSetChanged();

                if (playlist.name.equals("80-90")) {
                    playListid80 = playlist.id;
                    playListuri80 = playlist.uri;
                } else if (playlist.name.equals("91-100")) {
                    playListid90 = playlist.id;
                    playListuri90 = playlist.uri;
                } else if (playlist.name.equals("101-110")) {
                    playListid100 = playlist.id;
                    playListuri100 = playlist.uri;
                } else if (playlist.name.equals("111-120")) {
                    playListid110 = playlist.id;
                    playListuri110 = playlist.uri;
                } else if (playlist.name.equals("121-130")) {
                    playListid120 = playlist.id;
                    playListuri120 = playlist.uri;
                } else if (playlist.name.equals("131-140")) {
                    playListid130 = playlist.id;
                    playListuri130 = playlist.uri;
                } else if (playlist.name.equals("141-150")) {
                    playListid140 = playlist.id;
                    playListuri140 = playlist.uri;
                } else if (playlist.name.equals("151-160")) {
                    playListid150 = playlist.id;
                    playListuri150 = playlist.uri;
                }
            }
            return list;
        }
    }
}