package com.example.skydelivery;
import android.util.Log;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.mission.Mission;
import com.o3dr.services.android.lib.drone.mission.MissionItemType;
import com.o3dr.services.android.lib.drone.mission.item.MissionItem;
import com.o3dr.services.android.lib.drone.mission.item.command.SetServo;
import com.o3dr.services.android.lib.drone.mission.item.spatial.Waypoint;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PolygonSpray {
//    private static final String TAG = "PolygonSpray";
//    public ArrayList<LatLong> polygonPointList = new ArrayList<>();
//    public ArrayList<LatLong> sprayPointList = new ArrayList<>();
//    protected double sprayAngle;
//    private MainActivity mainActivity;
//    private ManageOverlaysInterface manageOverlays;
//    private ManageMission manageMission;
//    protected PolygonSprayState polygonSprayState;
//
//    // AB방제 관련 변수
//    private LatLong pointA = null;
//    private LatLong pointB = null;
//    private double sprayDistance = 5.5f;
//    private int maxSprayDistance = 50;
//    private int capacity = 0;
//
//    public static enum PolygonSprayState {
//        NONE,
//        STARTED,
//        STORED_A,
//        MAKED_SPRAYPOINT,
//        UPLOADED_MISSION,
//        PLAYING_MISSION,
//        PAUSE_MISSION,
//        FINISH_MISSION
//    }
//
//    public void setCapacity(int capacity) {
//        this.capacity = capacity;
//    }
//
//    PolygonSpray(MainActivity activity) {
//        this.mainActivity = activity;
//        manageOverlays = mainActivity.myDrone.getManageOverlays();
//    }
//
//    public void addPolygonPoint(LatLong latLong) {
//        double angle1 = 0, angle2 = 0;
//        int direction = 1;
//
//        if (polygonSprayState == PolygonSprayState.NONE) {
//            polygonSprayState = PolygonSprayState.STARTED;
//        }
//        polygonPointList.add(latLong);
//        manageOverlays.setPPosition(latLong);
//
//        if (mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_RIGHT || mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_LEFT) {
//
//            if (mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_RIGHT) {
//                direction = 1;
//            } else if (mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_LEFT) {
//                direction = -1;
//            }
//
//            if (polygonPointList.size() == 1) {
//                polygonSprayState = PolygonSprayState.STORED_A;
//            }
//
//            if (polygonPointList.size() == 2) {
//                angle1 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));
//                LatLong newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(1), angle1 + (90 * direction), 100);
//                addPolygonPoint(newPoint);
//
//                angle2 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(1), polygonPointList.get(0));
//                newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(0), angle2 - (90 * direction), 100);
//                addPolygonPoint(newPoint);
//                polygonSprayState = PolygonSprayState.MAKED_SPRAYPOINT;
//            }
//        }
//
//        if (polygonPointList.size() > 2) {
//            manageOverlays.drawPolygon();
//            if (mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_RIGHT || mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_LEFT) {
//                sprayAngle = angle1;
//            } else {
//                sprayAngle = makeSprayAngle();
//            }
//
//            try {
//                makeGrid();
//                polygonSprayState = PolygonSprayState.MAKED_SPRAYPOINT;
//            } catch(Exception e) {
//                Log.d("myCheck","예외처리 : " + e.getMessage());
//            }
//        }
//    }
//
//
//    // 다각형 방제에서 방제영역을 수정하면 호출되는 메소드로 방제영역과 경로를 다시 그려 준다.
//    public void modifyPolygonPoint() {
//        if (polygonPointList.size() > 2) {
//            manageOverlays.drawPolygon();
//            sprayAngle = makeSprayAngle();
//            try {
//                makeGrid();
//                polygonSprayState = PolygonSprayState.MAKED_SPRAYPOINT;
//            } catch(Exception e) {
//                Log.d("myCheck","예외처리 : " + e.getMessage());
//            }
//        }
//    }
//
//    /**
//     * Set 1 for rotateAmount for now, Increase if want more rotate.
//     * @param rotateAmount : ClockWise+, CounterClock-
//     */
//    public void rotatePath(double rotateAmount) {
//        if(this.polygonPointList.size() < 3) return;
//        sprayAngle += rotateAmount;
//        if(sprayAngle > 360) sprayAngle -= 360;
//        else if(sprayAngle < 1) sprayAngle += 360;
//        try {
//            makeGrid();
//        } catch(Exception e) {
//            Log.e("MRLogger","rotatePath error : " + e.getMessage());
//        }
//    }
//
//    public void makeGrid() throws Exception {
//        if(mainActivity == null) throw new Exception("PolygonSpray retreiving MapActivity returns null");
//
//        List<LatLong> polygonPoints = new ArrayList<>();
//        for(LatLong latLong : polygonPointList) {
//            polygonPoints.add(latLong);
//        }
//
//        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoints, this.sprayAngle, sprayDistance).getGrid();
//        List<LineLatLong> trimedGrid = new Trimmer(circumscribedGrid, makePoly().getLines()).getTrimmedGrid();
//
//        for (int i = 0; i < trimedGrid.size(); i++) {
//            LineLatLong line = trimedGrid.get(i);
//            if(line.getStart().getLatitude() > line.getEnd().getLatitude()) {
//                LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
//                trimedGrid.set(i, line1);
//            }
//        }
//
//        LatLong dronePosition = mainActivity.myDrone.getManageDroneState().getDronePosition();
//        double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0).getStart(), trimedGrid.get(0).getEnd(), dronePosition);
//        double dist2 = MathUtils.pointToLineDistance(trimedGrid.get(trimedGrid.size()-1).getStart(), trimedGrid.get(trimedGrid.size()-1).getEnd(), dronePosition);
//
//        if (dist2 < dist1) {
//            Collections.reverse(trimedGrid);
//            double distStart = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getStart());
//            double distEnd = MathUtils.getDistance2D(dronePosition, trimedGrid.get(trimedGrid.size()-1).getEnd());
//            if (distStart > distEnd) {
//                for (int i = 0; i < trimedGrid.size(); i++) {
//                    LineLatLong line = trimedGrid.get(i);
//                    LineLatLong line1 = new LineLatLong(line.getEnd(),line.getStart());
//                    trimedGrid.set(i, line1);
//                }
//            }
//        }
//
//        for (int i = 0; i < trimedGrid.size(); i++) {
//            LineLatLong line = trimedGrid.get(i);
//            if (i % 2 != 0) {
//                line = new LineLatLong(line.getEnd(), line.getStart());
//                trimedGrid.set(i,line);
//            }
//        }
//
//        sprayPointList.clear();
//        for(LineLatLong lineLatLong : trimedGrid) {
//            sprayPointList.add(lineLatLong.getStart());
//            sprayPointList.add(lineLatLong.getEnd());
//        }
//
//        manageOverlays.drawSprayPoint();
//        makeMission();
//    }
//
//    private Polygon makePoly() {
//        Polygon poly = new Polygon();
//        List<LatLong> latLongList = new ArrayList<>();
//        for(LatLong latLong : polygonPointList) {
//            latLongList.add(latLong);
//        }
//        poly.addPoints(latLongList);
//        return poly;
//    }
//
//    protected double makeSprayAngle() {
//        Polygon poly = makePoly();
//        double angle = 0;
//        double maxDistance = 0;
//        List<LineLatLong> lineLatLongList = poly.getLines();
//        for (LineLatLong lineLatLong : lineLatLongList) {
//            double lineDistance = MathUtils.getDistance2D(lineLatLong.getStart(), lineLatLong.getEnd());
//            if(maxDistance < lineDistance) {
//                maxDistance = lineDistance;
//                angle = lineLatLong.getHeading();
//            }
//        }
//        return angle;
//    }
//
//    private void makeMission() {
//
//        if(mainActivity == null || mainActivity.isFinishing()) {
//            Log.e(TAG, "makeMision retreiving MapActivity returns null");
//            return;
//        }
//
//        // 유저가 미리 설정한 분사량
//        int pwmOnMin = SharedPrefHelper.getInt(mainActivity, SharedPrefHelper.KEY_SLT_SPRAYAMOUNT, ServoHelper.CH6_STATE2);
//        int pwmOnMax = SharedPrefHelper.getInt(mainActivity, SharedPrefHelper.KEY_SLT_SPRAYAMOUNT, ServoHelper.CH6_STATE3);
//
//
//        int pwmOn = (int) Math.round(pwmOnMin + ((pwmOnMax - pwmOnMin) * (capacity * 0.01)));
//
//        if(mainActivity.myDrone == null) {
//            Log.e("MRLogger", "VandiDrone is null");
//            return;
//        }
//        double altitude;
//        int altitudeType;
//        manageMission = mainActivity.myDrone.getManageMission();
//        manageMission.clear();
//
//        for (int i = 1; i <= sprayPointList.size(); i++) {
//            Waypoint waypoint = new Waypoint();
//            SetServo setServo = new SetServo();
//
//            if(mainActivity.myDrone.isRadarActivated()) {
//                altitude = mainActivity.myDrone.getTerrainAlt();
//                altitudeType = MAV_FRAME.MAV_FRAME_GLOBAL_TERRAIN_ALT;
//            } else {
//                altitude = mainActivity.myDrone.getManageDroneState().getDroneAltitude();
//                altitudeType = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
//            }
//
//            LatLongAlt latLongAlt = new LatLongAlt(sprayPointList.get(i-1).getLatitude(), sprayPointList.get(i-1).getLongitude(), altitude);
//            //LatLongAlt latLongAlt = new LatLongAlt(sprayPointList.get(i-1).latitude, sprayPointList.get(i-1).longitude, 8);
//            waypoint.setCoordinate(latLongAlt);
//            waypoint.setFrame(altitudeType);
//            waypoint.setDelay(0);
//            setServo.setChannel(9);
//            if (i % 2 != 0) {
//                setServo.setPwm(pwmOn);
//            } else {
//                setServo.setPwm(ServoHelper.PWM_OFF);
//            }
//            manageMission.addMission(waypoint);
//            manageMission.addMission(setServo);
//            //if(!mainActivity.isCropSprayMode()) MissionDataManager.getInstance().addMissionItem(setServo);
//        }
//    }
//
//    public void resetPolygonSpray() {
//        polygonPointList.clear();
//        sprayPointList.clear();
//        polygonSprayState = PolygonSprayState.NONE;
//        pointA = null;
//        pointB = null;
//    }
//
//    protected void pauseProcess() {
//        polygonPointList.clear();
//        //sprayPointList.clear();
//        polygonSprayState = PolygonSprayState.PAUSE_MISSION;
//        pointA = null;
//        pointB = null;
//    }
//
//    public int getSprayPointCnt() {
//        return sprayPointList.size();
//    }
//
//    public PolygonSprayState getPolygonSprayState() {
//        return polygonSprayState;
//    }
//
//    public void setPolygonSprayState(PolygonSprayState polygonSprayState) {
//        this.polygonSprayState = polygonSprayState;
//    }
//
//    // AB방제 관련 메소드
//    public void setPointA(LatLong latLong) {
//        pointA = new LatLong(latLong.getLatitude(), latLong.getLongitude());
//        manageOverlays.setAPosition(latLong);
//    }
//
//    public void setPointB(LatLong latLong) {
//        pointB = new LatLong(latLong.getLatitude(), latLong.getLongitude());
//        manageOverlays.setBPosition(latLong);
//    }
//
//    public void makeWaypoint() {
//        int pointCnt = (int) Math.round(maxSprayDistance / sprayDistance);
//        int angleDirect = 1;
//
//        sprayPointList.clear();
//        sprayPointList.add(pointA);
//        sprayPointList.add(pointB);
//        double angle = MathUtils.getHeadingFromCoordinates(pointA, pointB);
//
//        if (mainActivity.abPestConButtonGroup.getMode() == ABPestConButtonGroup.eABPestMode.AB_RIGHT) {
//            angleDirect = 1;
//        } else {
//            angleDirect = -1;
//        }
//
//        for (int i = 1; i <= pointCnt; i++) {
//            if (i % 2 == 1) {
//                sprayPointList.add(MathUtils.newCoordFromBearingAndDistance(pointB, angle + 90 * angleDirect, sprayDistance * i));
//                sprayPointList.add(MathUtils.newCoordFromBearingAndDistance(pointA, angle + 90 * angleDirect, sprayDistance * i));
//            } else {
//                sprayPointList.add(MathUtils.newCoordFromBearingAndDistance(pointA, angle + 90 * angleDirect, sprayDistance * i));
//                sprayPointList.add(MathUtils.newCoordFromBearingAndDistance(pointB, angle + 90 * angleDirect, sprayDistance * i));
//            }
//        }
//        manageOverlays.drawSprayPoint();
//        makeMission();
//    }
//
//    public void setSprayDistance(double sprayDistance) {
//        this.sprayDistance = sprayDistance;
//    }
//
//    protected void processPrepareRetryMission() {
//        ManageMission manageMission = mainActivity.myDrone.getManageMission();
//        Mission mission = manageMission.getMission();
//        for (int i = 0; i < manageMission.currentIndex; i++) {
//            mission.removeMissionItem(i);
//        }
//        Waypoint waypoint = new Waypoint();
//        SetServo setServo = new SetServo();
//        LatLong dronePosition = mainActivity.myDrone.getManageDroneState().getDronePosition();
//        double droneAltitude = mainActivity.myDrone.getManageDroneState().getDroneAltitude();
//
//        waypoint.setCoordinate(new LatLongAlt(dronePosition.getLatitude(), dronePosition.getLongitude(), droneAltitude));
//        waypoint.setDelay(0);
//        setServo.setChannel(9);
//        setServo.setPwm(SharedPrefHelper.getInt(mainActivity, SharedPrefHelper.KEY_SLT_SPRAYAMOUNT, ServoHelper.CH6_STATE2));
//        mission.addMissionItem(0, setServo);
//        mission.addMissionItem(0, waypoint);
//        mainActivity.myDrone.getManageMission().sendMission();
//        polygonSprayState = PolygonSprayState.PAUSE_MISSION;
//
//        sprayPointList.clear();
//        for(MissionItem item : manageMission.getMission().getMissionItems()) {
//            if (item.getType() == MissionItemType.WAYPOINT) {
//                LatLongAlt latLongAlt = ((Waypoint)item).getCoordinate();
//                LatLong latLong = new LatLong(latLongAlt.getLatitude(), latLongAlt.getLongitude());
//                sprayPointList.add(latLong);
//            }
//        }
//
//        Log.d("mycheck","자동비행을 중단했을 때 sprayPointList의 개수 : " + sprayPointList.size());
//    }
}
