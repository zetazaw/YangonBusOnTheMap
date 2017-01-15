package net.konyan.yangonbusonthemap;

import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.SphericalUtil;
import com.google.maps.android.geojson.GeoJsonFeature;
import com.google.maps.android.geojson.GeoJsonLayer;
import com.google.maps.android.geojson.GeoJsonPointStyle;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String LOG_TAG = MainActivity.class.getCanonicalName();

    private final LatLng chaw = new LatLng(16.850728, 96.157675);
    private ProgressDialog loading;

    private MapView mapView;
    private GoogleMap map;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loading = ProgressDialog.show(this, "Loading", "Please wait");

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        MapsInitializer.initialize(this);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        load(googleMap);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        googleMap.setMyLocationEnabled(true);
        //initBottomSheet();

    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    private void initBottomSheet(List<Feature> featureList, final FeatureCollection stopFeature){

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_bus);
        //recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView.setAdapter(new BusRecyclerAdapter(this, featureList, new BusSelectListener() {
            @Override
            public void onBusClick(Feature feature) {
                Log.d(LOG_TAG, feature.toString());
                route(feature, stopFeature);
            }
        }));
    }

    private GeoJsonLayer line, stops;

    private void route(Feature featureClick, FeatureCollection stopFeature){
        if (line != null) line.removeLayerFromMap();
        if (stops != null) stops.removeLayerFromMap();

        BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.ic_point);
        FeatureCollection routeFeature = new FeatureCollection();

        try{
            String busServiceName = featureClick.getProperties().getString("service_name").trim();
            String busColor = featureClick.getProperties().getString("color").trim();

            for (Feature feature: stopFeature.getFeatures()){

                String busName = feature.getProperties().getString("service_name").trim();

                if (busName.equals(busServiceName)){
                    routeFeature.addFeature(feature);
                }
            }

            line = new GeoJsonLayer(map, featureClick.toJSON());
            line.getDefaultLineStringStyle().setColor(Color.parseColor(busColor));

            stops = new GeoJsonLayer(map, routeFeature.toJSON());
            for (GeoJsonFeature feature : stops.getFeatures()) {  //loop through features
                GeoJsonPointStyle pointStyle = new GeoJsonPointStyle();
                pointStyle.setTitle(feature.getProperty("name_mm"));
                pointStyle.setIcon(icon);
                feature.setPointStyle(pointStyle);
            }


            line.addLayerToMap();
            stops.addLayerToMap();

            //camera
            JSONArray coordinates = featureClick.getGeometry().toJSON().getJSONArray("coordinates");

            JSONArray midCoor = coordinates.getJSONArray(coordinates.length()/2);

            LatLng midLoc = new LatLng(midCoor.getDouble(1), midCoor.getDouble(0));



            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    toBounds(midLoc, 5).getCenter(), 12.0f));




        }catch (JSONException jse){

        }
    }

    private LatLngBounds toBounds(LatLng center, double radius) {
        LatLng southwest = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 225);
        LatLng northeast = SphericalUtil.computeOffset(center, radius * Math.sqrt(2.0), 45);
        return new LatLngBounds(southwest, northeast);
    }

    private void load(final GoogleMap map) {
        new AsyncTask<Void, Void, FeatureCollection[]>() {
            @Override
            protected void onPreExecute() {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(chaw, 11.5f));
                //map.setMinZoomPreference(7.0f);
                //map.setMinZoomPreference(14.0f);
            }

            @Override
            protected FeatureCollection[] doInBackground(Void... voids) {
                return new FeatureCollection[]{
                        (FeatureCollection) readRawJson(R.raw.services_000114),
                        (FeatureCollection) readRawJson(R.raw.bus_stops_000114)};
            }

            @Override
            protected void onPostExecute(FeatureCollection[] featureCollection) {
                loading.dismiss();

                FeatureCollection lineFeature = featureCollection[0];
                FeatureCollection stopFeature = featureCollection[1];

                initBottomSheet(lineFeature.getFeatures(), stopFeature);


            }
        }.execute();
    }

    private GeoJSONObject readRawJson(int rawId) {
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(rawId);
            return GeoJSON.parse(in_s);

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (JSONException jse) {
            jse.printStackTrace();
        }

        return null;
    }
}
