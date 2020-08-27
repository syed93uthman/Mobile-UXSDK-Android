package com.ikramatic.dronemonitor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.dji.mapkit.core.maps.DJIMap;
import com.dji.mapkit.core.models.DJIBitmapDescriptorFactory;
import com.dji.mapkit.core.models.DJILatLng;
import com.dji.mapkit.core.models.annotations.DJIMarker;
import com.dji.mapkit.core.models.annotations.DJIMarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.gimbal.Attitude;
import dji.common.gimbal.GimbalState;
import dji.common.model.LocationCoordinate2D;
import dji.common.util.CommonCallbacks;
import dji.common.util.CommonCallbacks.CompletionCallback;
import dji.keysdk.CameraKey;
import dji.keysdk.KeyManager;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.mission.tapfly.TapFlyMissionOperator;
import dji.sdk.products.Aircraft;
import dji.sdk.remotecontroller.RemoteController;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.ux.widget.FPVWidget;
import dji.ux.widget.MapWidget;
import dji.ux.widget.controls.CameraControlsWidget;

import com.ikramatic.dronemonitor.manage.RemoteControllerClass;

public final class CompleteWidgetActivity extends Activity {

    private MapWidget mapWidget;
    private ViewGroup parentView;
    private FPVWidget fpvWidget;
    private FPVWidget secondaryFPVWidget;
    private RelativeLayout primaryVideoView;
    private FrameLayout secondaryVideoView;
    private boolean isMapMini = true;

    private int height;
    private int width;
    private int margin;
    private int deviceWidth;
    private int deviceHeight;

    public LinearLayout menuLayout, menuHDMILayout;
    public Button setHomeButton;
    public Button dualVideoButton;
    public ImageButton extraMenuButton;
    public TextView homeLatTextView;
    public TextView homeLonTextView;
    public TextView gpsStatusTextView;
    public int gpsSignallevel = 0;
    boolean aircraftReady = false;
    boolean setHomeLocation = false;
    boolean homeLocationReady = false;
    boolean dualVideo = false;
    boolean menuVisible = false;
    Context context;

    JsonHandler myJson;
    MqttHandler mqttHandler;
    JSONObject droneJson;

    private FlightController mFlightController = null;
    private RemoteControllerClass mRemotetController;
    private double droneLocationLat = 0, droneLocationLng = 0;
    public double homeLocationLat = 0, homeLocationLng = 0;
    private double prevdroneLocationLat = 0, prevdroneLocationLng = 0;
    private float droneHeading = 0, droneAltitude = 0, cameraElevation = 0;
    private double droneRoll = 0, dronePitch = 0;

    private DJIMarker droneMarker = null;
    private DJIMap myMap;
    private DJIMarkerOptions markerOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_widgets);
        myJson = new JsonHandler();
        try {
            droneJson = myJson.createJson(this.getAssets().open("droneData.json"));
            droneJson.put("id", "1");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        markerOptions = new DJIMarkerOptions();

        mqttHandler = (MqttHandler) getIntent().getSerializableExtra(MainActivity.EXTRA_OBJ_MQTT);
        mqttHandler.connect(this);
        context = getApplicationContext();
        Toast.makeText(this, mqttHandler.brokerIp, Toast.LENGTH_LONG).show();

        height = DensityUtil.dip2px(this, 100);
        width = DensityUtil.dip2px(this, 150);
        margin = DensityUtil.dip2px(this, 12);

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        deviceHeight = displayMetrics.heightPixels;
        deviceWidth = displayMetrics.widthPixels;

        menuLayout = findViewById(R.id.menuLayout);
        menuHDMILayout = findViewById(R.id.menuHDMILayout);

        setHomeButton = findViewById(R.id.setHomeButton);
        homeLatTextView = findViewById(R.id.homeLatTextView);
        homeLonTextView = findViewById(R.id.homeLonTextView);
        gpsStatusTextView = findViewById(R.id.gpsStatusTextView);
        dualVideoButton = findViewById(R.id.dualVideoButton);
        extraMenuButton = findViewById(R.id.extraMenuButton);



        mapWidget = findViewById(R.id.map_widget);
        mapWidget.initGoogleMap(new MapWidget.OnMapReadyListener() {
            @Override
            public void onMapReady(@NonNull DJIMap map) {
                map.setOnMapClickListener(new DJIMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(DJILatLng latLng) {
                        onViewClick(mapWidget);
                    }
                });
                myMap = map;
            }
        });
        mapWidget.onCreate(savedInstanceState);
        initMapView();

        parentView = (ViewGroup) findViewById(R.id.root_view);

        fpvWidget = findViewById(R.id.fpv_widget);
        fpvWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onViewClick(fpvWidget);
            }
        });
        primaryVideoView = (RelativeLayout) findViewById(R.id.fpv_container);
        secondaryVideoView = (FrameLayout) findViewById(R.id.secondary_video_view);
        secondaryFPVWidget = findViewById(R.id.secondary_fpv_widget);

        setHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (aircraftReady) {
                    setHomeLocation = true;
                }
            }
        });

        dualVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dualVideoModeUpdate(!mRemotetController.getDualVideoStatus());
            }
        });

        extraMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (menuVisible == false) {
                    menuLayout.setVisibility(View.VISIBLE);
                    menuHDMILayout.setVisibility(View.VISIBLE);
                    Toast.makeText(CompleteWidgetActivity.this,"true",Toast.LENGTH_LONG).show();
                    menuVisible = true;
                } else {
                    menuLayout.setVisibility(View.INVISIBLE);
                    menuHDMILayout.setVisibility(View.INVISIBLE);

                    Toast.makeText(CompleteWidgetActivity.this,"false",Toast.LENGTH_LONG).show();
                    menuVisible = false;
                }

            }
        });

        secondaryFPVWidget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                swapVideoSource();
            }
        });
        updateSecondaryVideoVisibility();

        new Thread((Runnable) () -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    updateData();
                    if (aircraftReady) {
                        gpsStatusTextView.setText("Ready");
                    } else {
                        //gpsStatusTextView.setText(gpsSignallevel + "");
                    }
                    if (homeLocationReady && setHomeLocation) {
                        homeLatTextView.setText("Done ");
                        homeLonTextView.setText("Done ");
                        setHomeLocation = false;
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        initFlightController();
        menuLayout.setVisibility(View.INVISIBLE);
        menuHDMILayout.setVisibility(View.INVISIBLE);
    }

    private void onViewClick(View view) {
        if (view == fpvWidget && !isMapMini) {
            resizeFPVWidget(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, deviceWidth, deviceHeight, width, height, margin);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = true;
        } else if (view == mapWidget && isMapMini) {
            hidePanels();
            resizeFPVWidget(width, height, margin, 12);
            reorderCameraCapturePanel();
            ResizeAnimation mapViewAnimation = new ResizeAnimation(mapWidget, width, height, deviceWidth, deviceHeight, 0);
            mapWidget.startAnimation(mapViewAnimation);
            isMapMini = false;
        }
    }

    private void resizeFPVWidget(int width, int height, int margin, int fpvInsertPosition) {
        RelativeLayout.LayoutParams fpvParams = (RelativeLayout.LayoutParams) primaryVideoView.getLayoutParams();
        fpvParams.height = height;
        fpvParams.width = width;
        fpvParams.rightMargin = margin;
        fpvParams.bottomMargin = margin;
        if (isMapMini) {
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        } else {
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            fpvParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
            fpvParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        }
        primaryVideoView.setLayoutParams(fpvParams);

        parentView.removeView(primaryVideoView);
        parentView.addView(primaryVideoView, fpvInsertPosition);
    }

    private void reorderCameraCapturePanel() {
        View cameraCapturePanel = findViewById(R.id.CameraCapturePanel);
        parentView.removeView(cameraCapturePanel);
        parentView.addView(cameraCapturePanel, isMapMini ? 9 : 13);
    }

    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == FPVWidget.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(FPVWidget.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(FPVWidget.VideoSource.SECONDARY);
        }
    }

    private void updateSecondaryVideoVisibility() {
        if (secondaryFPVWidget.getVideoSource() == null) {
            secondaryVideoView.setVisibility(View.GONE);
        } else {
            secondaryVideoView.setVisibility(View.VISIBLE);
        }
    }

    private void hidePanels() {
        //These panels appear based on keys from the drone itself.
        if (KeyManager.getInstance() != null) {
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.HISTOGRAM_ENABLED), false, null);
            KeyManager.getInstance().setValue(CameraKey.create(CameraKey.COLOR_WAVEFORM_ENABLED), false, null);
        }

        //These panels have buttons that toggle them, so call the methods to make sure the button state is correct.
        CameraControlsWidget controlsWidget = findViewById(R.id.CameraCapturePanel);
        controlsWidget.setAdvancedPanelVisibility(false);
        controlsWidget.setExposurePanelVisibility(false);

        //These panels don't have a button state, so we can just hide them.
        findViewById(R.id.pre_flight_check_list).setVisibility(View.GONE);
        findViewById(R.id.rtk_panel).setVisibility(View.GONE);
        findViewById(R.id.spotlight_panel).setVisibility(View.GONE);
        findViewById(R.id.speaker_panel).setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Hide both the navigation bar and the status bar.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        mapWidget.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        mapWidget.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();
    }

    private class ResizeAnimation extends Animation {

        private View mView;
        private int mToHeight;
        private int mFromHeight;

        private int mToWidth;
        private int mFromWidth;
        private int mMargin;

        private ResizeAnimation(View v, int fromWidth, int fromHeight, int toWidth, int toHeight, int margin) {
            mToHeight = toHeight;
            mToWidth = toWidth;
            mFromHeight = fromHeight;
            mFromWidth = fromWidth;
            mView = v;
            mMargin = margin;
            setDuration(300);
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight;
            float width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth;
            RelativeLayout.LayoutParams p = (RelativeLayout.LayoutParams) mView.getLayoutParams();
            p.height = (int) height;
            p.width = (int) width;
            p.rightMargin = mMargin;
            p.bottomMargin = mMargin;
            mView.requestLayout();
        }
    }

    private void initFlightController() {
        if (isFlightControllerSupported()) {
            mFlightController = ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController();
            mRemotetController.setController(((Aircraft) DJISDKManager.getInstance().getProduct()).getRemoteController());
            dualVideoModeInit();
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState
                                             djiFlightControllerCurrentState) {

                    droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();  //Lat Double
                    droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude(); //Long Double
                    droneAltitude = djiFlightControllerCurrentState.getAircraftLocation().getAltitude(); //Float
                    droneHeading = mFlightController.getCompass().getHeading(); //Float
                    droneRoll = djiFlightControllerCurrentState.getAttitude().roll;
                    dronePitch = djiFlightControllerCurrentState.getAttitude().pitch;
                    if (!aircraftReady) {

                        if (djiFlightControllerCurrentState.getGPSSignalLevel() != null) {
                            gpsSignallevel = djiFlightControllerCurrentState.getGPSSignalLevel().value();
                            if (gpsSignallevel > 3) {
                                aircraftReady = true;
                            }
                        }
                    }
                    if (setHomeLocation && aircraftReady) {

                        mFlightController.setHomeLocationUsingAircraftCurrentLocation(new CompletionCallback() {
                            @Override
                            public void onResult(DJIError djiError) {
                                if (djiError != null) {
                                    if (context != null)
                                        Toast.makeText(context, djiError.getDescription(), Toast.LENGTH_LONG);
                                } else {
                                    if (context != null)
                                        Toast.makeText(context, "Home Location Success", Toast.LENGTH_LONG);
                                }
                            }
                        });


                        mFlightController.getHomeLocation(new CommonCallbacks.CompletionCallbackWith<LocationCoordinate2D>() {
                            @Override
                            public void onSuccess(LocationCoordinate2D locationCoordinate2D) {
                                homeLocationLat = locationCoordinate2D.getLatitude();
                                homeLocationLng = locationCoordinate2D.getLongitude();
                                homeLocationReady = true;
                            }

                            @Override
                            public void onFailure(DJIError djiError) {

                            }
                        });
                    }
                }
            });
            Gimbal gimbal = DJISDKManager.getInstance().getProduct().getGimbal();
            gimbal.setStateCallback(new GimbalState.Callback() {
                @Override
                public void onUpdate(GimbalState gimbalState) {
                    Attitude gimbalAttitude = gimbalState.getAttitudeInDegrees();
                    cameraElevation = gimbalAttitude.getPitch(); //Float
                }
            });
        }
    }

    public void updateData() {
        try {
            droneJson.put("lat", String.valueOf(droneLocationLat));
            droneJson.put("long", String.valueOf(droneLocationLng));
            droneJson.put("altitude", String.valueOf(droneAltitude));
            droneJson.put("elevationCam", String.valueOf(cameraElevation));
            droneJson.put("heading", String.valueOf(droneHeading));
            droneJson.put("roll", String.valueOf(droneRoll));
            droneJson.put("pitch", String.valueOf(dronePitch));
            droneJson.put("update", true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    private boolean isFlightControllerSupported() {
        return DJISDKManager.getInstance().getProduct() != null &&
                DJISDKManager.getInstance().getProduct() instanceof Aircraft &&
                ((Aircraft) DJISDKManager.getInstance().getProduct()).getFlightController() != null;
    }

    private void updateDroneLocation() {

        DJILatLng pos = new DJILatLng(3.0811006794504965, 101.56146941738935);
        //Create MarkerOptions object
        markerOptions.position(pos);
        markerOptions.icon(DJIBitmapDescriptorFactory.fromResource(R.drawable.drone));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordinates(droneLocationLat, droneLocationLng)) {
                    droneMarker = myMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void initMapView() {

        if (myMap == null) {
            myMap = mapWidget.getMap();
        }

        DJILatLng ikramatic = new DJILatLng(3.0811006794504965, 101.56146941738935);
        markerOptions.position(ikramatic);
    }

    public boolean checkGpsCoordinates(double v1, double v2) {
        boolean result = false;
        if (prevdroneLocationLat != v1 || prevdroneLocationLng != v2) {
            prevdroneLocationLat = v1;
            prevdroneLocationLng = v2;
            result = true;
        }
        return result;
    }

    public void dualVideoModeInit(){
        if(mRemotetController.checkLiveVideoStatus()){

        }
        else{
            Toast.makeText(this,"No product",Toast.LENGTH_LONG).show();
        }
    }

    public void dualVideoModeUpdate(Boolean aBoolean){
        if(mRemotetController.setLiveVideoStatus(aBoolean)){
            if(mRemotetController.getDualVideoStatus()){
                dualVideoButton.setBackgroundResource(R.drawable.bgselectedbutton);
                dualVideoButton.setTextColor(Color.parseColor("#000000"));
            }
            else{
                dualVideoButton.setBackgroundResource(R.drawable.bgbutton);
                dualVideoButton.setTextColor(Color.parseColor("#FFFFFF"));
            }
        }
        else {
            Toast.makeText(this,"No product",Toast.LENGTH_LONG).show();
        }
    }
}