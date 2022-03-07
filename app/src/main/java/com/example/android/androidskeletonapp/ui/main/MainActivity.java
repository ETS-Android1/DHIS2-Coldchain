package com.example.android.androidskeletonapp.ui.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.androidskeletonapp.R;
import com.example.android.androidskeletonapp.data.Sdk;
import com.example.android.androidskeletonapp.data.service.ActivityStarter;
import com.example.android.androidskeletonapp.data.service.SyncStatusHelper;
import com.example.android.androidskeletonapp.ui.code_executor.CodeExecutorActivity;
import com.example.android.androidskeletonapp.ui.cold_chain.ColdChain;
import com.example.android.androidskeletonapp.ui.d2_errors.D2ErrorActivity;
import com.example.android.androidskeletonapp.ui.data_sets.DataSetsActivity;
import com.example.android.androidskeletonapp.ui.data_sets.instances.DataSetInstancesActivity;
import com.example.android.androidskeletonapp.ui.foreign_key_violations.ForeignKeyViolationsActivity;
import com.example.android.androidskeletonapp.ui.login.LoginActivity;
import com.example.android.androidskeletonapp.ui.programs.ProgramsActivity;
import com.example.android.androidskeletonapp.ui.tracked_entity_instances.TrackedEntityInstancesActivity;
import com.example.android.androidskeletonapp.ui.tracked_entity_instances.search.TrackedEntityInstanceSearchActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;

import org.hisp.dhis.android.core.D2Manager;
import org.hisp.dhis.android.core.arch.call.D2Progress;
import org.hisp.dhis.android.core.event.EventCreateProjection;
import org.hisp.dhis.android.core.event.EventStatus;
import org.hisp.dhis.android.core.maintenance.D2Error;
import org.hisp.dhis.android.core.program.ProgramStageDataElement;
import org.hisp.dhis.android.core.user.User;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static com.example.android.androidskeletonapp.data.service.LogOutService.logOut;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private CompositeDisposable compositeDisposable;
    MyDatabaseHelper myDB;
    private FloatingActionButton syncMetadataButton;
    private FloatingActionButton syncDataButton;
    private FloatingActionButton uploadDataButton;

    private Button connectToNetworkBtn;

    private ConstraintLayout fab_labels;

    private TextView syncStatusText;
    private ProgressBar progressBar;

    private boolean isSyncing = false;

    RecyclerView recyclerView;
    private MyRecyclerViewAdapter.RecyclerViewClickListener listener;

    //BLE variables
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mLeDevices;
    private boolean mScanning;
    private Handler mHandler;
    private boolean loggedIn;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ACCESS_FINE_LOCATION_PERMISSION = 85;
    // Stops scanning after 6 seconds.
    private static final long SCAN_PERIOD = 4000;

    private Boolean connectedToNetwork = false;

    private String MozTeiDirre ="pebsEKg891S";

    private String mozEnrollmentID = "eVwQptiLbqK";
    private String mozProgramID ="J3mQgSxGakP";
    private String mozProgramStage ="wHK19rrcVBI";
    private String mozOrgUnitId = "azEkexvj0fC";

    private String norEnrollemntId = "g5oklCs7xIg";
    private String norProgram = "SDuMzcGLh8i";
    private String norProgramStage ="aecqgkE5quA";
    private String norOrgUnitId ="iMDPax84iAN";
    public static Intent getMainActivityIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);
        mHandler = new Handler();

        compositeDisposable = new CompositeDisposable();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION);
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mLeDevices = new ArrayList<>();

        // set up the RecyclerView
        recyclerView = findViewById(R.id.rvDevices);
        myDB = new MyDatabaseHelper(MainActivity.this);

        //inflateMainListView();
        inflateMainView();

        checkNetworkConnection();
        if(connectedToNetwork){
            User user = getUser();
            TextView greeting = findViewById(R.id.greeting);
            greeting.setText(String.format("Hi %s!", user.displayName()));
            loggedIn = true;
        }else{
            TextView greeting = findViewById(R.id.greeting);
            greeting.setText("Cold Chain Monitoring");
            loggedIn = false;
        }

        if(connectedToNetwork){
            User user = getUser();
            createNavigationView(user);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        checkNetworkConnection();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Initializes list view adapter.
        scanLeDevice(true);
        if(connectedToNetwork){
            updateSyncDataAndButtons();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
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
                mLeDevices.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    private void setAdapter(){
        setOnClickListener();
        MyRecyclerViewAdapter adapter = new MyRecyclerViewAdapter(mLeDevices, listener);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    private void setOnClickListener(){
        listener = new MyRecyclerViewAdapter.RecyclerViewClickListener() {

            @Override
            public void onClick(View v, int position) {
                final BluetoothDevice device = mLeDevices.get(position);
                // Stop scan onclick

                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                if (device == null) return;

                final Intent intent = new Intent(getApplicationContext(), DeviceControlActivity.class);
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivity(intent);

            }
        };
    }

    private User getUser() {
        return Sdk.d2().userModule().user().blockingGet();
    }

    private User getUserFromCursor() {
        try (Cursor cursor = Sdk.d2().databaseAdapter().query("SELECT * FROM user;")) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                return User.create(cursor);
            } else {
                return null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (compositeDisposable != null) {
            compositeDisposable.clear();
        }
    }

    private void inflateMainView() {
       syncMetadataButton = findViewById(R.id.syncMetadataButton);
       syncDataButton = findViewById(R.id.syncDataButton);
        uploadDataButton = findViewById(R.id.uploadDataButton);

        syncStatusText = findViewById(R.id.notificator);
        progressBar = findViewById(R.id.syncProgressBar);

        syncMetadataButton.setOnClickListener(view -> {
            setSyncing();
            Snackbar.make(view, "Syncing metadata", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            syncStatusText.setText(R.string.syncing_metadata);
            syncMetadata();
        });

        syncDataButton.setOnClickListener(view -> {
            setSyncing();
            Snackbar.make(view, "Syncing data", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            syncStatusText.setText(R.string.syncing_data);
            downloadData();
        });

        uploadDataButton.setOnClickListener(view -> {
            setSyncing();
            Snackbar.make(view, "Uploading data", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            syncStatusText.setText(R.string.uploading_data);
            uploadData();
        });
    }
    @SuppressLint("RestrictedApi")
    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        connectToNetworkBtn = findViewById(R.id.connect_button);
        fab_labels = findViewById(R.id.fab_labels);

        if(networkInfo == null){
            connectedToNetwork = false;
            syncMetadataButton.setVisibility(View.INVISIBLE);
            syncDataButton.setVisibility(View.INVISIBLE);
            uploadDataButton.setVisibility(View.INVISIBLE);

            fab_labels.setVisibility(View.INVISIBLE);
            connectToNetworkBtn.setVisibility(View.VISIBLE);
            connectToNetworkBtn.setOnClickListener(view -> {
                if(loggedIn){
                    Toast.makeText(this, "Connect to WIFI or Mobile data", Toast.LENGTH_SHORT).show();
                }else{
                    ActivityStarter.startActivity(this, LoginActivity.getLoginActivityIntent(this),true);
                }
            });
        }
        else if(networkInfo.isConnected()){
            connectedToNetwork = true;
            fab_labels.setVisibility(View.VISIBLE);
            syncMetadataButton.setVisibility(View.VISIBLE);
            syncDataButton.setVisibility(View.VISIBLE);
            uploadDataButton.setVisibility(View.VISIBLE);
            connectToNetworkBtn.setVisibility(View.INVISIBLE);
            ArrayList<String> offList = myDB.returnAllOfflineData();
            if(!offList.isEmpty()){
                //Makes events from the offline DB
                addEventsFromOfflineDB(mozEnrollmentID,mozProgramID,mozProgramStage, mozOrgUnitId);
                //After events are made sanitizes the offline db
                myDB.deleteAllOfflineData();
            }
        }
    }

    private void addEventsFromOfflineDB(String enrollmentID, String programUid , String programStageId, String ouUid){
            String defaultOptionCombo = Sdk.d2().categoryModule().categoryOptionCombos()
                    .byDisplayName().eq("default").one().blockingGet().uid();
            try {
                ArrayList<String> dbResult = getDbResult();

                //makes a new event and returns it eventUID
                String eventUid = Sdk.d2().eventModule().events().blockingAdd(
                        EventCreateProjection.builder()
                                .enrollment(enrollmentID)
                                .program(programUid)
                                .programStage(programStageId)
                                .organisationUnit(ouUid)
                                .attributeOptionCombo(defaultOptionCombo)
                                .build()
                );

                //sets the created-, enrollment- and event-date to this date.
                System.out.println("Ny Event UID :::::  " + eventUid);

                //Super important to have correct date format or it wont upload!!

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                String dateString = format.format(new Date());
                Date date = format.parse (dateString);

                Sdk.d2().eventModule().events().uid(eventUid).setEventDate(date);
                //ENROLLMENT ---  EnrollmentID
                Sdk.d2().enrollmentModule().enrollments().uid(mozEnrollmentID).setEnrollmentDate(date);
                Sdk.d2().eventModule().events().uid(eventUid).setCompletedDate(date);


                //Get's the event's dataElement UID and set's a value to the event
                List<ProgramStageDataElement> programDataElem = Sdk.d2().programModule().programStageDataElements().blockingGet();
                for (ProgramStageDataElement elem: programDataElem
                ) {
                    for (int i = 0; i < dbResult.size() ; i++) {
                        Sdk.d2().trackedEntityModule().trackedEntityDataValues().value(eventUid, elem.dataElement().uid()).blockingSet(dbResult.get(i).substring(0,4));
                        System.out.println(Sdk.d2().trackedEntityModule().trackedEntityDataValues().value(eventUid, elem.dataElement().uid()).blockingGet());
                    }
                }

                //set eventStatus and upload
                Sdk.d2().eventModule().events().uid(eventUid).setStatus(EventStatus.COMPLETED);
                Sdk.d2().eventModule().events().upload();

            } catch (D2Error | ParseException d2Error) {
                d2Error.printStackTrace();
            }

    }

    private void setSyncing() {
        isSyncing = true;
        progressBar.setVisibility(View.VISIBLE);
        syncStatusText.setVisibility(View.VISIBLE);
        updateSyncDataAndButtons();
    }

    private void setSyncingFinished() {
        isSyncing = false;
        progressBar.setVisibility(View.GONE);
        syncStatusText.setVisibility(View.GONE);
        updateSyncDataAndButtons();
    }

    private void disableAllButtons() {
        setEnabledButton(syncMetadataButton, false);
        setEnabledButton(syncDataButton, false);
        setEnabledButton(uploadDataButton, false);
    }

    private void enablePossibleButtons(boolean metadataSynced) {
        if (!isSyncing) {
            setEnabledButton(syncMetadataButton, true);
            if (metadataSynced) {
                 setEnabledButton(syncDataButton, true);
                if (SyncStatusHelper.isThereDataToUpload()) {
                    setEnabledButton(uploadDataButton, true);
                }
            }
        }
    }

    private void setEnabledButton(FloatingActionButton floatingActionButton, boolean enabled) {
        floatingActionButton.setEnabled(enabled);
        floatingActionButton.setAlpha(enabled ? 1.0f : 0.3f);
    }

    private void updateSyncDataAndButtons() {
        disableAllButtons();

        int programCount = SyncStatusHelper.programCount();
        int dataSetCount = SyncStatusHelper.dataSetCount();

        enablePossibleButtons(programCount + dataSetCount > 0);

    }

    private void createNavigationView(User user) {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navView);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        View headerView = navigationView.getHeaderView(0);

        TextView firstName = headerView.findViewById(R.id.firstName);
        TextView email = headerView.findViewById(R.id.email);
        firstName.setText(user.firstName());
        email.setText(user.email());
    }

    private void syncMetadata() {
        compositeDisposable.add(downloadMetadata()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(Throwable::printStackTrace)
                .doOnComplete(() -> {
                    setSyncingFinished();
                })
                .subscribe());
    }

    private Observable<D2Progress> downloadMetadata() {
        return Sdk.d2().metadataModule().download();
    }

    private void downloadData() {
        compositeDisposable.add(
                Observable.merge(
                        downloadTrackedEntityInstances(),
                        downloadSingleEvents(),
                        downloadAggregatedData()
                )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(() -> {
                            setSyncingFinished();
                        })
                        .doOnError(Throwable::printStackTrace)
                        .subscribe());
    }

    private Observable<D2Progress> downloadTrackedEntityInstances() {
        return Sdk.d2().trackedEntityModule().trackedEntityInstanceDownloader()
                .limit(10).limitByOrgunit(false).limitByProgram(false).download();
    }

    private Observable<D2Progress> downloadSingleEvents() {
        return Sdk.d2().eventModule().eventDownloader()
                .limit(10).limitByOrgunit(false).limitByProgram(false).download();
    }

    private Observable<D2Progress> downloadAggregatedData() {
        return Sdk.d2().aggregatedModule().data().download();
    }

    private void uploadData() {
        if(Sdk.d2().eventModule() == null){
          System.out.println("There are no Events to upload");
        }
        else{
        compositeDisposable.add(
                Sdk.d2().fileResourceModule().fileResources().upload()
                        .concatWith(Sdk.d2().trackedEntityModule().trackedEntityInstances().upload())
                        .concatWith(Sdk.d2().dataValueModule().dataValues().upload())
                        .concatWith(Sdk.d2().eventModule().events().upload())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnComplete(this::setSyncingFinished)
                        .doOnError(Throwable::printStackTrace)
                        .subscribe());
        }
    }

    private void wipeData() {
        compositeDisposable.add(
                Observable.fromCallable(() -> Sdk.d2().wipeModule().wipeData())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnError(Throwable::printStackTrace)
                        .doOnComplete(this::setSyncingFinished)
                        .subscribe());
    }

    private ArrayList<String> getDbResult(){
        return myDB.returnAvgTemps_offline();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.navColdChain) {
            ActivityStarter.startActivity(this, ColdChain.getIntent(this), false);
        } else if (id == R.id.navWipeData) {
            syncStatusText.setText(R.string.wiping_data);
            wipeData();
        } else if (id == R.id.navExit) {
            compositeDisposable.add(logOut(this));
        }
        DrawerLayout drawer = findViewById(R.id.drawerLayout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!mLeDevices.contains(device)) {
                                // Filters out other devices
                                // TO-DO make it generic for BM devices
                                if (device.getAddress().startsWith("CC:98:A3") || device.getAddress().startsWith("C2:1C:80") || device.getAddress().startsWith("E3:8C:93:AA") || device.getAddress().startsWith("D6:70:D8")|| device.getAddress().startsWith("F1:78:D6") || device.getAddress().startsWith("D0:5A:3A")|| device.getAddress().startsWith("F5:2C:3F") || device.getAddress().startsWith("DD:94:DF")|| device.getAddress().startsWith("FC:50:2F")|| device.getAddress().startsWith("DA:3A:71")|| device.getAddress().startsWith("DA:33:CE")|| device.getAddress().startsWith("F8:DD:CC")|| device.getAddress().startsWith("CA:54:66")|| device.getAddress().startsWith("F4:C0:DD")|| device.getAddress().startsWith("DF:6B:BE")){
                                    mLeDevices.add(device);
                                }
                            }
                        }
                    });
                    setAdapter();
                }
            };

}