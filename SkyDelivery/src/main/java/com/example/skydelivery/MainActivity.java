package com.example.skydelivery;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.List;

import static com.o3dr.android.client.apis.ExperimentalApi.getApi;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, DroneListener, TowerListener, LinkListener {

    private static final String TAG = "";
    NaverMap mNaverMap;
    Animation fab_open, fab_close;
    FloatingActionButton mFabMain, mFabBasic, mFabNavi, mFabSatellite, mFabHybrid, mFabTerrain;
    Boolean openFlag = false;

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();

    private static final int DEFAULT_UDP_PORT = 14550;
    private static final int DEFAULT_USB_BAUD_RATE = 57600;

    private Spinner modeSelector;

    Handler mainHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        boolean isImmersiveModeEnabled = ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("Is on?", "Turning immersive mode mode off. ");
        } else {
            Log.i("Is on?", "Turning immersive mode mode on.");
        }

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

        fab_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.fab_close);
        mFabMain = findViewById(R.id.fabMain);
        mFabBasic = findViewById(R.id.fabBasic);
        mFabNavi = findViewById(R.id.fabNavi);
        mFabSatellite = findViewById(R.id.fabSatellite);
        mFabHybrid = findViewById(R.id.fabHybrid);
        mFabTerrain = findViewById(R.id.fabTerrain);

        mFabBasic.startAnimation(fab_close);
        mFabNavi.startAnimation(fab_close);
        mFabSatellite.startAnimation(fab_close);
        mFabHybrid.startAnimation(fab_close);
        mFabTerrain.startAnimation(fab_close);

        mFabBasic.setClickable(false);
        mFabNavi.setClickable(false);
        mFabSatellite.setClickable(false);
        mFabHybrid.setClickable(false);
        mFabTerrain.setClickable(false);

        mFabMain.setOnClickListener(this);
        mFabBasic.setOnClickListener(this);
        mFabNavi.setOnClickListener(this);
        mFabSatellite.setOnClickListener(this);
        mFabHybrid.setOnClickListener(this);
        mFabTerrain.setOnClickListener(this);

        FloatingActionButton fab = findViewById(R.id.fabMain);

        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient("895cz3v0pt")
        );

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.naverMap);
        if(mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.naverMap, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        final CheckBox cb1 = (CheckBox)findViewById(R.id.checkBox);
        cb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cb1.isChecked() == true) mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL,true);
                else if(cb1.isChecked()==false)mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL,false);
            }
        });

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                ((TextView)parent.getChildAt(0)).setTextColor(Color.WHITE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    @Override
    public void onClick(View view) {

        int id = view.getId();

        switch (id) {
            case R.id.fabMain:
                anim();
                break;
            case R.id.fabBasic:
                anim();
                Toast.makeText(this, "Change MapType 'Basic'", Toast.LENGTH_SHORT).show();
                mNaverMap.setMapType(NaverMap.MapType.Basic);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, true);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BICYCLE, true);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_MOUNTAIN, true);
                break;
            case R.id.fabNavi:
                anim();
                Toast.makeText(this, "Change MapType 'Navi'", Toast.LENGTH_SHORT).show();
                mNaverMap.setMapType(NaverMap.MapType.Navi);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_BUILDING, true);
                mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRAFFIC, true);
                break;
            case R.id.fabSatellite:
                anim();
                Toast.makeText(this, "Change MapType 'Satellite'", Toast.LENGTH_SHORT).show();
                mNaverMap.setMapType(NaverMap.MapType.Satellite);
                break;
            case R.id.fabHybrid:
                anim();
                Toast.makeText(this, "Change MapType 'Hybrid'", Toast.LENGTH_SHORT).show();
                mNaverMap.setMapType(NaverMap.MapType.Hybrid);
                break;
            case R.id.fabTerrain:
                anim();
                Toast.makeText(this, "Change MapType 'Terrain'", Toast.LENGTH_SHORT).show();
                mNaverMap.setMapType(NaverMap.MapType.Terrain);
                break;
        }
    }

    public void anim() {
        if (openFlag) {
            mFabBasic.startAnimation(fab_close);
            mFabNavi.startAnimation(fab_close);
            mFabSatellite.startAnimation(fab_close);
            mFabHybrid.startAnimation(fab_close);
            mFabTerrain.startAnimation(fab_close);

            mFabBasic.setClickable(false);
            mFabNavi.setClickable(false);
            mFabSatellite.setClickable(false);
            mFabHybrid.setClickable(false);
            mFabTerrain.setClickable(false);

            openFlag = false;
        } else {
            mFabBasic.startAnimation(fab_open);
            mFabNavi.startAnimation(fab_open);
            mFabSatellite.startAnimation(fab_open);
            mFabHybrid.startAnimation(fab_open);
            mFabTerrain.startAnimation(fab_open);

            mFabBasic.setClickable(true);
            mFabNavi.setClickable(true);
            mFabSatellite.setClickable(true);
            mFabHybrid.setClickable(true);
            mFabTerrain.setClickable(true);

            openFlag = true;
        }
    }

    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.mNaverMap = naverMap;
        naverMap.setMapType(NaverMap.MapType.Satellite);
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true);
        naverMap.setContentPadding(0, 0, 0, 200);
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);

        Marker marker = new Marker();
        marker.setPosition(new LatLng(35.944303,126.684297));
        marker.setMap(naverMap);
    }

    //Drone Start

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    // DroneKit-Android Listener

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    //Drone Listener

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateVoltage();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.GPS_COUNT:
                updateGps();
                break;

            /*case AttributeEvent.GPS_POSITION:
                updateYaw();
                break;*/

            case AttributeEvent.HOME_UPDATED:
                //updateDistanceFromHome();
                break;

            default:
                // Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    //UI Events

    public void onBtnConnectTap(View view) {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
        } else {
            Spinner connectionSelector = (Spinner) findViewById(R.id.selectConnectionType);
            int selectedConnectionType = connectionSelector.getSelectedItemPosition();

            ConnectionParameter connectionParams = selectedConnectionType == ConnectionType.TYPE_UDP
                    ? ConnectionParameter.newUsbConnection(null)
                    : ConnectionParameter.newUdpConnection(null);

            this.drone.connect(connectionParams);
        }
    }

    public void onFlightModeSelected(View view) {
        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();

        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Vehicle mode change successful.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Vehicle mode change failed: " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("Vehicle mode change timed out.");
            }
        });
    }

    public void onArmButtonTap(View view) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off.");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    //UI Updating

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.btnConnect);
        if (isConnected) {
            connectButton.setText("Disconnect");
        } else {
            connectButton.setText("Connect");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        Button armButton = (Button) findViewById(R.id.btnArmTakeOff);

        if (!this.drone.isConnected()) {
            armButton.setVisibility(View.INVISIBLE);
        } else {
            armButton.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            armButton.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            armButton.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            armButton.setText("ARM");
        }
    }

    protected void updateVoltage() {
        TextView voltageTextView = (TextView) findViewById(R.id.voltageValueTextView);
        Battery droneBattery = this.drone.getAttribute(AttributeType.BATTERY);
        voltageTextView.setText(String.format("%3.1f", droneBattery.getBatteryVoltage()) + "V");
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    protected void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    /*protected void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.YAWValueTextView);
        YawCondition droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        yawTextView.setText(String.format("%f",droneYaw.getAngle()) + "deg");
    }*/

    protected void updateGps() {
        TextView gpsTextView = (TextView) findViewById(R.id.gpsValueTextView);
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        gpsTextView.setText(String.format("%d",droneGps.getSatellitesCount()));
    }

    protected void updateVehicleModesForType(int droneType) {

        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    // Helper methods
    // ==========================================================

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + connectionStatus.getStatusCode());
        }
    }
}
