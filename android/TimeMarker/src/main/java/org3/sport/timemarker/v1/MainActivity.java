package org3.sport.timemarker.v1;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import net.nosocial.clock.Clock;
import net.nosocial.clock.HighPrecisionClock;
import net.nosocial.clock.PreciseNow;
import net.nosocial.clock.SynchronizingClock;
import net.nosocial.clock.SystemClock;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ikh
 * @since 1/25/14
 */
public class MainActivity extends Activity {
    static final String TIME_MARKER_TAG = "TimeMarker";

    public static final int TICK_CLOCK_MSG = 42;

    public static final String LIST_ITEM_FLAG = "Flag";
    public static final String LIST_ITEM_DATE = "Date";
    public static final String LIST_ITEM_TIME = "Time";
    public static final String LIST_ITEM_PRECISION = "TPS"; // TODO: not tps anymore
    public static final String LIST_ITEM_MARK_OBJECT = "MarkObject";

    public static final int ALLOWED_TIME_FROM_LAST_SYNC = 60000;

    private int pinSoundEffect;
    private int tickSoundEffect;
    private SoundPool soundPool;
    private Handler handler;
    private HashSet<Integer> readySamples = new HashSet<Integer>(2);

    private final Clock systemClock = new SystemClock();
    private final SynchronizingClock syncClock = new SynchronizingClock(systemClock);
    private final HighPrecisionClock highPrecisionClock = new HighPrecisionClock(syncClock);
    private SharedPreferences preferences;

    private LocationListener locationUpdatesListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            // Location[mProvider=gps,mTime=1390740202000,mLatitude=***,mLongitude=***,mHasAltitude=true,mAltitude=***,mHasSpeed=true,mSpeed=0.0,mHasBearing=false,mBearing=0.0,mHasAccuracy=true,mAccuracy=15.0,mExtras=Bundle[mParcelledData.dataSize=4]], time = 1390740202000
            highPrecisionClock.sync(location.getTime());
            Log.d(TIME_MARKER_TAG, "GPS time sync: " + syncClock.toString());
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // see http://stackoverflow.com/questions/2021176/how-can-i-check-the-current-status-of-the-gps-receiver
            // "onStatusChanged() doesn't get called on Eclair and Froyo. It does get called on 1.6 though."
            Log.d(TIME_MARKER_TAG, "GPS status changed: " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TIME_MARKER_TAG, "GPS provider enabled");

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TIME_MARKER_TAG, "GPS provider disabled");
        }
    };
    private GpsStatus.Listener gpsStatusListener = new GpsStatus.Listener() {
        GpsStatus status;

        @Override
        public void onGpsStatusChanged(int event) {
            status = locationManager.getGpsStatus(status);
            int satellitesCount = 0;
            Iterable<GpsSatellite> satellites = status.getSatellites();
            for (GpsSatellite ignored : satellites) {
                satellitesCount++;
            }

            // TODO: in case of GpsStatus.GPS_EVENT_SATELLITE_STATUS -
            // check last location change time - it will outdate in 3-5 seconds (means that gps signal is lost)

            // GPS status event: 4, max satellites: 255, satellites: 4, ttff: 9
            // GPS status event: 3, max satellites: 255, satellites: 8, ttff: 336218 - first fix

            Log.d(TIME_MARKER_TAG, String.format(
                    "GPS status event: %d, max satellites: %d, satellites: %s, ttff: %d",
                    event,
                    status.getMaxSatellites(),
                    satellitesCount,
                    status.getTimeToFirstFix()));
        }
    };
    private LocationManager locationManager;

    private final AtomicBoolean ntpSyncEnabled = new AtomicBoolean();
    private final AtomicBoolean gpsSyncEnabled = new AtomicBoolean();
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TIME_MARKER_TAG, "Shared preference change: " + key);

            if (key.equals("clock_sync")) {
                String clockSyncSetting = clockSyncSetting();
                Log.d(TIME_MARKER_TAG, "Clock sync setting change to " + clockSyncSetting);

                ntpSyncEnabled.set(isNtpSyncSettingEnabled(clockSyncSetting));

                if (gpsSyncEnabled.get() && !isGpsSyncSettingEnabled(clockSyncSetting)) {
                    disableGPSSync();
                } else if (!gpsSyncEnabled.get() && isGpsSyncSettingEnabled(clockSyncSetting)) {
                    enableGPSSync();
                }
            }
        }
    };

    // TODO: INVERTED??
    static List<Map<String, Object>> markListItems ;
    static SimpleAdapter markListItemsAdapter;
    static ItemsStore store;
    static ImageButton shareButton;

    private Thread ntpSyncThread;
    private volatile boolean forceStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.i(TIME_MARKER_TAG, "onCreate");

        forceStop = false;

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        startSoundPool();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // TODO: move out SQL from UI thread
        store = new ItemsStore(this);

        markListItems = new LinkedList<Map<String, Object>>();

        List<Marker> storedMarkers = store.queryAll();

        for (Marker storedMarker : storedMarkers) {
            markListItems.add(0, createListItem(storedMarker));
        }

        markListItemsAdapter = new SimpleAdapter(
                this, markListItems, R.layout.list_row,
                new String[]{LIST_ITEM_FLAG, LIST_ITEM_DATE, LIST_ITEM_TIME, LIST_ITEM_PRECISION},
                new int[]{
                        R.id.list_item_flag,
                        R.id.list_item_date,
                        R.id.list_item_time,
                        R.id.list_item_tps});

        setContentView(R.layout.activity_main);
    }

    private void startSoundPool() {
        soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);

        pinSoundEffect = soundPool.load(this, R.raw.pin, 1);
        tickSoundEffect = soundPool.load(this, R.raw.tick, 1);

        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                if (status == 0) {
                    readySamples.add(sampleId);
                } else {
                    Log.v(TIME_MARKER_TAG, "Wrong status of sample " + sampleId + ":  " + status);
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        shareButton = (ImageButton) findViewById(R.id.share_button);
        assert shareButton != null;
        shareButton.setVisibility(markListItems.isEmpty() ? View.INVISIBLE : View.VISIBLE);

        final ListView listView = (ListView) findViewById(R.id.timestamp_list);
        listView.setAdapter(markListItemsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TIME_MARKER_TAG,
                        String.format("export item id %d at position %d", id, position));

                Map<String, Object> item = markListItems.get(position);
                Marker itemMarker = (Marker) item.get(LIST_ITEM_MARK_OBJECT);

                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("message/rfc822");
                i.putExtra(Intent.EXTRA_SUBJECT, "[3sport Time Marker] This marker only");
                i.putExtra(Intent.EXTRA_TEXT, _formatMarkerForEmail(itemMarker));

                String menuTitle = String.format("Share this marker only");

                sendIntent(i, menuTitle);
            }
        });

        registerForContextMenu(listView);

        resetPrecision();

        String clockSyncSetting = clockSyncSetting();

        ntpSyncEnabled.set(isNtpSyncSettingEnabled(clockSyncSetting));
        Log.i(TIME_MARKER_TAG, "NTP sync: " + ntpSyncEnabled.get());

        startNTPSyncThread();

        LocationProvider locationProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
        Log.d(TIME_MARKER_TAG, "GPS provider: " + locationProvider); // TODO: check null?

        if (isGpsSyncSettingEnabled(clockSyncSetting)) {
            enableGPSSync();
        } else {
            Log.i(TIME_MARKER_TAG, "GPS sync disabled");
        }

        preferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);


        ImageView pinImage = (ImageView) findViewById(R.id.pin);
        pinImage.setBackgroundResource(R.drawable.pin);
        final AnimationDrawable pinAnimation = (AnimationDrawable) pinImage.getBackground();
        assert pinAnimation != null;

        final TextView ticksPerSecondView = (TextView) findViewById(R.id.tps_text);

        final TextView dow = (TextView) findViewById(R.id.day_of_week);
        final TextView hhMM = (TextView) findViewById(R.id.time_hh_mm);
        final TextView ssSSS = (TextView) findViewById(R.id.time_ss_SSS);
        final TextView ddMM = (TextView) findViewById(R.id.dd_mmm);

        handler = new Handler() {
            private String lastPinAnimationSeconds = null;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == TICK_CLOCK_MSG) {
                    PreciseNow preciseNow = highPrecisionClock.preciseNow();
                    Date wallClockDate = new Date(preciseNow.getTime());

                    dow.setText(DateFormats.clockDow.format(wallClockDate));
                    String hoursAndMinutes = DateFormats.clockHhMm.format(wallClockDate);
                    String seconds = DateFormats.clockS.format(wallClockDate);
                    hhMM.setText(hoursAndMinutes);
                    ssSSS.setText(" " + seconds + "."
                            + DateFormats.clockMs.format(wallClockDate));
                    ddMM.setText(DateFormats.clockMonthDate.format(wallClockDate));

                    if (lastPinAnimationSeconds == null
                            || !lastPinAnimationSeconds.equals(seconds)) {
                        lastPinAnimationSeconds = seconds;

                        if (readySamples.contains(tickSoundEffect)
                                && preferences.getBoolean("tick_sound", false)) {
                            soundPool.play(tickSoundEffect, 0.25f, 0.25f, 1, 0, 1); // TODO: priority?
                        }

                        if (pinAnimation.isRunning()) {
                            pinAnimation.stop();
                        }
                        pinAnimation.start();
                    }

                    scheduleHandlerTick();

                    ticksPerSecondView.setText(Marker.formatListItemPrecision(
                            isPrecisionAccurate() ? preciseNow.getPrecision() : 0));

                    highPrecisionClock.tickTock();
                }
            }
        };

        scheduleHandlerTick();

        final View clock = findViewById(R.id.view_clock);
        final Button markButton = (Button) findViewById(R.id.button_mark_timestamp);

        clock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mark();
                    clock.setBackgroundResource(R.drawable.gradient_pressed);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    clock.setBackgroundResource(R.drawable.gradient);
                }
                return true;
            }
        });
        markButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mark();
                }
                return false;
            }
        });

    }

    private void startNTPSyncThread() {
        Log.i(TIME_MARKER_TAG, "Starting NTP sync thread");
        ntpSyncThread = new Thread(new Runnable() {
            @Override
            public void run() {

                final List<String> pool = Arrays.asList(
                        "0.pool.ntp.org",
                        "1.pool.ntp.org",
                        "2.pool.ntp.org",
                        "3.pool.ntp.org");

                while (!forceStop) {
                    int nextSync = 30000;
                    if (ntpSyncEnabled.get()) {
                        SntpClient sntpClient = new SntpClient();

                        Collections.shuffle(pool);

                        if (sntpClient.requestTime(pool.get(0), 5000)
                                || sntpClient.requestTime(pool.get(1), 5000)
                                || sntpClient.requestTime(pool.get(2), 5000)
                                || sntpClient.requestTime(pool.get(3), 5000)) {

                            // FIXME: NPE possible (not initialized?)
                            highPrecisionClock.sync(sntpClient.getNtpTime()
                                    + android.os.SystemClock.elapsedRealtime()
                                    - sntpClient.getNtpTimeReference());
                            highPrecisionClock.addSyncDelay((int) sntpClient.getRoundTripTime());

                            Log.d(TIME_MARKER_TAG,
                                    String.format("NTP time: %d, %d, %d (%s)",
                                            sntpClient.getNtpTime(),
                                            sntpClient.getNtpTimeReference(),
                                            sntpClient.getRoundTripTime(),
                                            syncClock));
                        } else {
                            Log.d(TIME_MARKER_TAG, "NTP request failed from all servers");
                            /**
                             * Ensure that at least 2 failures of all servers happen before
                             * {@link ALLOWED_TIME_FROM_LAST_SYNC} .
                             */
                            nextSync = 10000;
                        }
                    } else {
                        Log.i(TIME_MARKER_TAG, "NTP sync disabled");
                    }
                    try {
                        Thread.sleep(nextSync);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                if (forceStop) {
                    Log.i(TIME_MARKER_TAG, "NTP sync thread stopped");
                } else {
                    Log.w(TIME_MARKER_TAG, "NTP sync thread interrupted");
                }

            }
        });
        ntpSyncThread.start();
    }

    private void resetPrecision() {
        highPrecisionClock.tickTock();
        highPrecisionClock.reset();
        highPrecisionClock.tickTock();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();

        menu.setHeaderTitle("Flag this marker");

        inflater.inflate(R.menu.context_menu, menu);

        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;

        Map<String, Object> listItem = markListItems.get(info.position);
        Marker marker = (Marker) listItem.get(LIST_ITEM_MARK_OBJECT);
        Flag flag = marker.getFlag();

        menu.getItem(flag.ordinal()).setChecked(true);

        new MenuFlagsCustomizer(menu, getResources()).customize();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        assert info != null;

        Map<String, Object> listItem = markListItems.get(info.position);
        Marker marker = (Marker) listItem.get(LIST_ITEM_MARK_OBJECT);

        Flag flag = Flag.byContextMenu(item.getItemId());

        marker.setFlag(flag);
        store.updateFlag(marker, flag);
        listItem.put(LIST_ITEM_FLAG, flag.getDrawableId());
        markListItemsAdapter.notifyDataSetChanged();

        return true;
    }

    private boolean isPrecisionAccurate() {
        return highPrecisionClock.isPrecisionAccumulated()
                && (isSyncOff()
                || highPrecisionClock.timeFromLastSync() <= ALLOWED_TIME_FROM_LAST_SYNC);
    }

    private boolean isSyncOff() {
        return clockSyncSetting().equals("off");
    }

    private boolean isNtpSyncSettingEnabled(String clockSyncSetting) {
        return clockSyncSetting.equals("ntp") || clockSyncSetting.equals("gps+ntp");
    }

    private boolean isGpsSyncSettingEnabled(String clockSyncSetting) {
        return clockSyncSetting.equals("gps") || clockSyncSetting.equals("gps+ntp");
    }

    private void enableGPSSync() {
        Log.i(TIME_MARKER_TAG, "Enable GPS sync");

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("GPS is disabled");
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage("Show location settings?");
            builder.setInverseBackgroundForced(true);
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int which) {
                            Log.i(MainActivity.TIME_MARKER_TAG, "Go to GPS setting dialog");

                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);

                            dialog.dismiss();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }

        gpsSyncEnabled.set(true);
        locationManager.addGpsStatusListener(gpsStatusListener);
        // TODO: see http://developer.android.com/guide/topics/location/strategies.html
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationUpdatesListener);
    }

    private void disableGPSSync() {
        Log.i(TIME_MARKER_TAG, "Disable GPS sync");
        gpsSyncEnabled.set(false);
        locationManager.removeGpsStatusListener(gpsStatusListener);
        locationManager.removeUpdates(locationUpdatesListener);
    }

    private String clockSyncSetting() {
        return preferences.getString("clock_sync", "ntp");
    }

    private boolean volumeAsMarkSetting() {
        return preferences.getBoolean("volume_mark", false);
    }

    private void scheduleHandlerTick() {
        handler.sendEmptyMessageDelayed(TICK_CLOCK_MSG, 5);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TIME_MARKER_TAG, "onPause");
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Log.d(TIME_MARKER_TAG, "onWindowFocusChanged: " + hasFocus);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TIME_MARKER_TAG, "onResume");

        resetPrecision();
        scheduleHandlerTick();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TIME_MARKER_TAG, "onStop");

    }

    private void stopNTPSyncThread() {
        Log.i(TIME_MARKER_TAG, "Stopping NTP sync thread");
        ntpSyncThread.interrupt();
        try {
            ntpSyncThread.join();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        ntpSyncThread = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TIME_MARKER_TAG, "onStart");

        // TODO: move GPS enable dialog here
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TIME_MARKER_TAG, "onDestroy");

        forceStop = true;
        stopSoundPool();
        stopNTPSyncThread();
    }

    private void stopSoundPool() {
        soundPool.release();
        readySamples.clear();
        soundPool = null;
    }

    private void mark() {
        highPrecisionClock.tickTock(); // just to be sure
        PreciseNow preciseNow = highPrecisionClock.preciseNow();

        Marker markerItem = new Marker(preciseNow.getTime(),
                isPrecisionAccurate() ? preciseNow.getPrecision() : 0);

        store.insert(markerItem);

        HashMap<String, Object> item = createListItem(markerItem);
        markListItems.add(0, item);
        shareButton.setVisibility(View.VISIBLE);
        markListItemsAdapter.notifyDataSetChanged();

        if (readySamples.contains(pinSoundEffect) && preferences.getBoolean("mark_sound", true)) {
            soundPool.play(pinSoundEffect, 1, 1, 1, 0, 1); // TODO: priority?
        }
    }

    private HashMap<String, Object> createListItem(Marker markerItem) {
        Date wallClockDate = markerItem.getDate();

        HashMap<String, Object> item = new HashMap<String, Object>();
        item.put(LIST_ITEM_FLAG, markerItem.getFlagDrawableId());
        item.put(LIST_ITEM_DATE, DateFormats.listDate.format(wallClockDate));
        item.put(LIST_ITEM_TIME, DateFormats.listTime.format(wallClockDate));
        item.put(LIST_ITEM_PRECISION, markerItem.getFormattedPrecision());
        item.put(LIST_ITEM_MARK_OBJECT, markerItem);
        return item;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        goToSettings();
        return false;
    }

    public void onClickSettings(View view) {
        goToSettings();
    }

    private void goToSettings() {
        Log.d(TIME_MARKER_TAG, "Settings click");
        Intent settings = new Intent(this, SettingsActivity.class);
        startActivity(settings);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            boolean volumeAsMark = volumeAsMarkSetting();
            if (volumeAsMark) {
                mark();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (volumeAsMarkSetting()) {
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    public void onClickShare(View view) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");
        i.putExtra(Intent.EXTRA_SUBJECT, String.format("[3sport Time Marker] All markers"));
        StringBuilder text = new StringBuilder();
        _markListItems_exportAsText(text);
        i.putExtra(Intent.EXTRA_TEXT, text.toString());

        int itemsCount = markListItems.size();
        String menuTitle = String.format("Share all markers (%d)", itemsCount);

        sendIntent(i, menuTitle);
    }

    private void _markListItems_exportAsText(StringBuilder text) {
        for (Map<String, Object> markListItem : markListItems) {
            Marker itemMarker = (Marker) markListItem.get(LIST_ITEM_MARK_OBJECT);
            text.append(_formatMarkerForEmail(itemMarker));
        }
        text.append("\n");
        int itemsCount = markListItems.size();
        text.append(String.format("Total: %d", itemsCount));
    }

    private String _formatMarkerForEmail(Marker marker) {
        StringBuilder stringBuilder = new StringBuilder(String.format("%s    (%s)",
                DateFormats.emailDateTime.format(marker.getDate()),
                marker.getPrecision()));
        if (marker.hasFlag()) {
            stringBuilder.append(" ").append(marker.getEmailFlag());
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private void sendIntent(Intent i, String menuTitle) {
        try {
            startActivity(Intent.createChooser(i,
                    menuTitle));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(MainActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
        }
    }
}
