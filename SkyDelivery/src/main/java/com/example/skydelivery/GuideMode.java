package com.example.skydelivery;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.NaverMap;
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

import java.util.ArrayList;

public class GuideMode extends AppCompatActivity {
    LatLng mGuidedPoint; // 가이드모드 목적지 저장.
    Marker mMarkerGuide = new com.naver.maps.map.overlay.Marker();
    //OverlayImage guideIcon = OverlayImage.fromResource(R.drawable.location_overlay_icon);
    private MainActivity mMainActivity;

    public GuideMode() {

    }

    public GuideMode(MainActivity mainActivity) {
        this.mMainActivity = mainActivity;
    }

    public void DialogSimple(final Drone drone, final LatLong point) {
        AlertDialog.Builder alt_bld = new AlertDialog.Builder(mMainActivity);
        alt_bld.setMessage("확인하시면 가이드모드로 전환후 기체가 이동합니다.").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            // Action for 'Yes' Button
            VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                        @Override
                        public void onSuccess() {
                            ControlApi.getApi(drone).goTo(point, true, null);
                            mMainActivity.mData.add("현재고도를 유지하며 이동합니다.");
                            mMainActivity.mRecyclerView.smoothScrollToPosition(mMainActivity.mData.size()-1);
                            mMainActivity.mDroneLog.notifyDataSetChanged();
                        }
                        @Override
                        public void onError(int i) {
                            mMainActivity.mData.add("기체를 이동할 수 없습니다.");
                            mMainActivity.mRecyclerView.smoothScrollToPosition(mMainActivity.mData.size()-1);
                            mMainActivity.mDroneLog.notifyDataSetChanged();
                        }
                        @Override
                        public void onTimeout() {
                            mMainActivity.mData.add("가이드모드 시간초과.");
                            mMainActivity.mRecyclerView.smoothScrollToPosition(mMainActivity.mData.size()-1);
                            mMainActivity.mDroneLog.notifyDataSetChanged();
                        }
                    });
        }
        // Action for 'No' Button
        }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        AlertDialog alert = alt_bld.create();
        alert.show();
    }
    public static boolean CheckGoal(final Drone drone, LatLng recentLatLng) {
        GuidedState guidedState = drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(), guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    // Custom Dialog
    /*public void DialogDimpleCustom(final Drone drone, final LatLong point) {
        View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity);
        builder.setView(dialogView);

        final AlertDialog alertDialog = builder.create();

        TextView title = dialogView.findViewById(R.id.title);
        title.setText("현재 고도를 유지하며");
        TextView message = dialogView.findViewById(R.id.message);
        message.setText("목표지점까지 기체가 이동합니다.");
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        btnPositive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
                    @Override
                    public void onSuccess() {
                        ControlApi.getApi(drone).goTo(point, true, null);
                    }
                    @Override
                    public void onError(int i) {
                        mMainActivity.alertUser("Error");
                    }
                    @Override
                    public void onTimeout() {
                        mMainActivity.alertUser("Time out");
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
    }*/
}

