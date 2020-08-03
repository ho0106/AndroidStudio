package com.example.skydelivery;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;

import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, DroneListener, TowerListener {

    private static final String TAG = "";
    NaverMap mNaverMap;
    Animation fab_open, fab_close;
    FloatingActionButton mFabMain, mFabBasic, mFabNavi, mFabSatellite, mFabHybrid, mFabTerrain;
    Boolean openFlag = false;

    private RecyclerView recyclerView;
    public RecyclerView.LayoutManager layoutManager;
    private RecyclerView.Adapter adapter;

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private Marker marker = new Marker();
    private List<LatLng> poly = new ArrayList<>();
    private PolylineOverlay polylineOverlay = new PolylineOverlay();

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
        fab_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close);
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
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.naverMap);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.naverMap, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        final CheckBox cb1 = (CheckBox) findViewById(R.id.checkBox);
        cb1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cb1.isChecked() == true)
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                else if (cb1.isChecked() == false)
                    mNaverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
            }
        });

        this.modeSelector = (Spinner) findViewById(R.id.modeSelect);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
                ((TextView) parent.getChildAt(0)).setTextColor(Color.WHITE);
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
        UiSettings uiSettings = naverMap.getUiSettings();
        naverMap.setMapType(NaverMap.MapType.Basic);
        naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_TRANSIT, true);
        naverMap.setContentPadding(0, 0, 0, 200);
        naverMap.setMinZoom(5.0);
        naverMap.setMaxZoom(18.0);
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setScaleBarEnabled(false);
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
                updateMapMoveButton();
                updateClearButton();
                updateArmButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateMapMoveButton();
                updateClearButton();
                updateArmButton();
                clearValue();
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

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.HOME_UPDATED:
                //updateDistanceFromHome();
                break;

            case AttributeEvent.GPS_POSITION:
                updateDroneLocation();
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

    public void onBtnTakeOffAltitudeTap(View view) {
        Button upAltitudeButton = (Button) findViewById(R.id.btnUpAltitude);
        Button downAltitudeButton = (Button) findViewById(R.id.btnDownAltitude);

        if (upAltitudeButton.getVisibility() == view.GONE) {
            upAltitudeButton.setVisibility(View.VISIBLE);
            downAltitudeButton.setVisibility(View.VISIBLE);
        } else {
            upAltitudeButton.setVisibility(View.GONE);
            downAltitudeButton.setVisibility(View.GONE);
        }
    }

    public void onBtnMissionTap(View view) {
        Button ABMissionButton = (Button) findViewById(R.id.btnABMission);
        Button polygonMissionButton = (Button) findViewById(R.id.btnPolygonMission);
        Button missionCancelButton = (Button) findViewById(R.id.btnMissionCancel);

        if (ABMissionButton.getVisibility() == view.GONE) {
            ABMissionButton.setVisibility(View.VISIBLE);
            polygonMissionButton.setVisibility(View.VISIBLE);
            missionCancelButton.setVisibility(View.VISIBLE);
        } else {
            ABMissionButton.setVisibility(View.GONE);
            polygonMissionButton.setVisibility(View.GONE);
            missionCancelButton.setVisibility(View.GONE);
        }
    }

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
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();

        if (vehicleState.isFlying()) {
            onArmButtonFunction();
        } else if (vehicleState.isArmed()) {
            TextView title = dialogView.findViewById(R.id.title);
            title.setText("지정한 이륙고도까지 기체가 상승합니다.");
            TextView message = dialogView.findViewById(R.id.message);
            message.setText("안전거리를 유지하세요");
            Button btnPositive = dialogView.findViewById(R.id.btnPositive);
            btnPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onArmButtonFunction();
                    alertDialog.dismiss();
                }
            });
            Button btnNegative = dialogView.findViewById(R.id.btnNegative);
            btnNegative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        } else if (!vehicleState.isConnected()) {
            alertUser("Connect to a drone first");
        } else {
            TextView title = dialogView.findViewById(R.id.title);
            title.setText("모터를 가동합니다.");
            TextView message = dialogView.findViewById(R.id.message);
            message.setText("모터가 고속으로 회전합니다.");
            Button btnPositive = dialogView.findViewById(R.id.btnPositive);
            btnPositive.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onArmButtonFunction();
                    alertDialog.dismiss();
                }
            });
            Button btnNegative = dialogView.findViewById(R.id.btnNegative);
            btnNegative.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    public void onArmButtonFunction() {
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
            ControlApi.getApi(this.drone).takeoff(5.0, new AbstractCommandListener() {

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

    public void onClearButtonTap(View view) {
        marker.setMap(null);
        polylineOverlay.setMap(null);
        Toast.makeText(getApplicationContext(),"All Clear", Toast.LENGTH_SHORT).show();
    }

    public void onMapMoveButtonTap(View view) {
        Button mapMoveButton = (Button) findViewById(R.id.btnMapMove);
    }

    //UI Updating

    protected void updateTakeOffAltitudeButton() {
        Button takeOffAltitudeButton = (Button) findViewById(R.id.btnTakeOffAltitude);

        if (!this.drone.isConnected()) {
            takeOffAltitudeButton.setVisibility(View.INVISIBLE);
        } else {
            takeOffAltitudeButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateMissionButton() {
        Button missionButton = (Button) findViewById(R.id.btnMission);

        if (!this.drone.isConnected()) {
            missionButton.setVisibility(View.INVISIBLE);
        } else {
            missionButton.setVisibility(View.VISIBLE);
        }
    }


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

    protected void updateClearButton() {
        Button clearButton = (Button) findViewById(R.id.btnClear);

        if (!this.drone.isConnected()) {
            clearButton.setVisibility(View.INVISIBLE);
        } else {
            clearButton.setVisibility(View.VISIBLE);
        }
    }

    protected void updateMapMoveButton() {
        Button mapMoveButton = (Button) findViewById(R.id.btnMapMove);

        if (!this.drone.isConnected()) {
            mapMoveButton.setVisibility(View.INVISIBLE);
        } else {
            mapMoveButton.setVisibility(View.VISIBLE);
        }
    }

    public void updateDroneLocation() {
        Gps droneInitLocation = this.drone.getAttribute(AttributeType.GPS);
        Attitude droneHead = this.drone.getAttribute(AttributeType.ATTITUDE);
        CameraUpdate cameraUpdate = CameraUpdate.scrollTo(new LatLng(droneInitLocation.getPosition().getLatitude(), droneInitLocation.getPosition().getLongitude())).animate(CameraAnimation.Linear);


        marker.setPosition(new LatLng(droneInitLocation.getPosition().getLatitude(), droneInitLocation.getPosition().getLongitude()));
        marker.setIcon(OverlayImage.fromResource(R.drawable.location_overlay_icon));
        marker.setWidth(100);
        marker.setHeight(400);
        marker.setMap(mNaverMap);
        marker.setAngle((float) droneHead.getYaw());
        mNaverMap.moveCamera(cameraUpdate);

        Collections.addAll(poly, new LatLng(droneInitLocation.getPosition().getLatitude(), droneInitLocation.getPosition().getLongitude()));
        polylineOverlay.setCoords(poly);

        poly.set(0, new LatLng(droneInitLocation.getPosition().getLatitude(), droneInitLocation.getPosition().getLongitude()));
        polylineOverlay.setCoords(poly);
        polylineOverlay.setWidth(4);
        polylineOverlay.setCapType(PolylineOverlay.LineCap.Round);
        polylineOverlay.setJoinType(PolylineOverlay.LineJoin.Round);
        polylineOverlay.setColor(Color.RED);
        polylineOverlay.setMap(mNaverMap);
    }

    protected  void clearValue() {
        TextView voltageTextView = (TextView) findViewById(R.id.voltageValueTextView);
        voltageTextView.setText(String.format("0V"));
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        altitudeTextView.setText(String.format("0m"));
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        speedTextView.setText(String.format("0m/s"));
        TextView yawTextView = (TextView) findViewById(R.id.YAWValueTextView);
        yawTextView.setText(String.format("0deg"));
        TextView gpsTextView = (TextView) findViewById(R.id.gpsValueTextView);
        gpsTextView.setText(String.format("0"));
        marker.setMap(null);
        polylineOverlay.setMap(null);
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

    protected void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.YAWValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        yawTextView.setText(String.format("%3.0f", droneYaw.getYaw()) + "deg");
    }

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
}
