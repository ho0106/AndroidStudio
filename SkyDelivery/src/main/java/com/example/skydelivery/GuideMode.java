package com.example.skydelivery;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;

public class GuideMode extends AppCompatActivity {
    LatLong mGuidedPoint;
    Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker();
    OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.location_overlay_icon);
    private MainActivity mMainActivity;

    public GuideMode(MainActivity mainActivity) {
        this.mMainActivity = mainActivity;
    }

    public void DialogSimple(final Drone drone, final LatLong point) {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
        builder.setView(R.id.dialogLayout);

        final AlertDialog alertDialog = builder.create();

        TextView title = dialogView.findViewById(R.id.title);
        title.setText("현재고도를 유지하며");
        TextView message = dialogView.findViewById(R.id.message);
        message.setText("목표지점까지 기체가 이동합니다.");
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED,
                        new AbstractCommandListener() {
                            @Override
                            public void onSuccess() {
                                ControlApi.getApi(drone).goTo(point, true,null);
                            }

                            @Override
                            public void onError(int executionError) {

                            }

                            @Override
                            public void onTimeout() {

                            }
                        });

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

    public static boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }
}