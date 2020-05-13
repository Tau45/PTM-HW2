package com.tylkowski.hw2;

    import androidx.fragment.app.FragmentActivity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
    import com.google.gson.reflect.TypeToken;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private GoogleMap mMap;

    private List<MarkerOptions> markerOptionsList = new ArrayList<>();

    private TextView sensorTextView;
    private FloatingActionButton dotFAB;
    private FloatingActionButton crossFAB;
    private FloatingActionButton dummyFAB;
    private Button clrMemButton;

    private boolean buttonsUp;
    private boolean accelerometerOn;

    private SensorManager mSensorManager;
    private Sensor accelerometer;

    private final String MARKERS_JSON_FILE = "markers.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorTextView = findViewById(R.id.sensor_textview);
        dotFAB = findViewById(R.id.dot_button);
        crossFAB = findViewById(R.id.cross_button);
        dummyFAB = findViewById(R.id.dummy_button);
        clrMemButton = findViewById(R.id.clear_memory_button);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        buttonsUp = false;
        accelerometerOn = false;

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        mMap.getUiSettings().setMapToolbarEnabled(false);
        restoreMarkersFromJson();
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        MarkerOptions markerOptions = new MarkerOptions()
                .position(new LatLng(latLng.latitude,latLng.longitude))
                .title(String.format("Position:(%.2f, %.2f)",latLng.latitude,latLng.longitude));
        mMap.addMarker(markerOptions);
        markerOptionsList.add(markerOptions);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        buttonAnimation(true);
        return false;
    }

    private void buttonAnimation(boolean show){
        if(buttonsUp != show){
            ObjectAnimator dotAnimator, dotFadeAnimator;
            ObjectAnimator crossAnimator, crossFadeAnimator;

            float FromAlpha = show ? 0f : 0.7f;
            float ToAlpha = show ? 0.7f : 0f;

            float anim_StartY = show ? dummyFAB.getY() : clrMemButton.getY();
            float anim_EndY = show ? clrMemButton.getY() : dummyFAB.getY();

            dotAnimator = ObjectAnimator.ofFloat(dotFAB, "y", anim_StartY, anim_EndY);
            dotAnimator.setInterpolator(new AccelerateInterpolator());
            dotAnimator.setDuration(400);

            crossAnimator = ObjectAnimator.ofFloat(crossFAB, "y", anim_StartY, anim_EndY);
            crossAnimator.setInterpolator(new AccelerateInterpolator());
            crossAnimator.setDuration(400);

            dotFadeAnimator = ObjectAnimator.ofFloat(dotFAB, "alpha", FromAlpha, ToAlpha);
            dotFadeAnimator.setInterpolator(new AccelerateInterpolator());
            dotFadeAnimator.setDuration(600);

            crossFadeAnimator = ObjectAnimator.ofFloat(crossFAB, "alpha", FromAlpha, ToAlpha);
            crossFadeAnimator.setInterpolator(new AccelerateInterpolator());
            crossFadeAnimator.setDuration(600);

            AnimatorSet animSet = new AnimatorSet();
            animSet.play(dotAnimator).with(crossAnimator).with(dotFadeAnimator).with(crossFadeAnimator);
            animSet.start();
            buttonsUp = true;
        }
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    public void clearMemory(View view) {
        ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
    }

    public void hideButtons(View view) {
        buttonAnimation(false);
        buttonsUp = false;
    }

    public void toggleAccelerometer(View view) {
        if(!accelerometerOn){
            if(accelerometer != null)
                mSensorManager.registerListener(this, accelerometer, 100000);
            sensorTextView.setVisibility(View.VISIBLE);
            accelerometerOn = true;
        }else{
            if(accelerometer != null)
                mSensorManager.unregisterListener(this, accelerometer);
            sensorTextView.setVisibility(View.INVISIBLE);
            accelerometerOn = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        String sensorText = "Acceleration:\n" +
                String.format("x: %.4f y: %.4f", event.values[0], event.values[1]);
        sensorTextView.setText(sensorText);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy(){
        try {
            saveMarkersToJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private void saveMarkersToJson() throws JSONException {
        JSONArray jsonArray = new JSONArray();

        for(int i = 0; i < markerOptionsList.size(); i++){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("latitude", markerOptionsList.get(i).getPosition().latitude);
            jsonObject.put("longitude", markerOptionsList.get(i).getPosition().longitude);
            jsonArray.put(jsonObject);
        }

        FileOutputStream outputStream;
        try{
            outputStream = openFileOutput(MARKERS_JSON_FILE,MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(jsonArray.toString());
            writer.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private static class MarkerCoordinate{
        float latitude;
        float longitude;
    }

    public void restoreMarkersFromJson(){
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try{
            inputStream = openFileInput(MARKERS_JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while((n = reader.read(buf)) >= 0){
                String tmp = String.valueOf(buf);
                String substring = (n<DEFAULT_BUFFER_SIZE) ? tmp.substring(0,n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<MarkerCoordinate>>(){
            }.getType();
            List<MarkerCoordinate> o = gson.fromJson(readJson,collectionType);
            if(o != null){
                markerOptionsList.clear();
                for(MarkerCoordinate markerCoordinate : o){
                    MarkerOptions markerOptions = new MarkerOptions()
                            .position(new LatLng(markerCoordinate.latitude,markerCoordinate.longitude))
                            .title(String.format("Position:(%.2f, %.2f)",markerCoordinate.latitude,markerCoordinate.longitude));
                    mMap.addMarker(markerOptions);
                    markerOptionsList.add(markerOptions);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}
