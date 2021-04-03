package com.example.projecttest;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback{
    GoogleMap map;
    //SearchView searchView;

    // added to save the latlng for current location and last searched location
    double startLongitude, startLatitude, endLongitude, endLatitude;
    SupportMapFragment mapFragment;
    Location currentLocation;
    FusedLocationProviderClient client; // to track current location
    private static final int REQUEST_CODE = 101;
    float result[] = new float[10];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        client = LocationServices.getFusedLocationProviderClient(this);
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        getMyLocation();
        getDesieredLocation();

        if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O){
            NotificationChannel channel= new NotificationChannel("arrived",
                    "arrived to Destination", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

    }

    private void getDesieredLocation() {
        final SearchView searchView = findViewById(R.id.location);

        // write it in a thread
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {


                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        // map.clear(); //clear the previous markers
                        String location = searchView.getQuery().toString();
                        List<Address> addressList = null;
                        if (location != null || !location.equals("")) {
                            //geocoder class helps adding a new marker on a new location
                            Geocoder geocoder = new Geocoder(MainActivity.this);
                            try {
                                addressList = geocoder.getFromLocationName(location, 1);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Address address = addressList.get(0);
                            endLatitude = address.getLatitude();
                            endLongitude = address.getLongitude();
                            //adding the new marker
                            LatLng latLng = new LatLng(endLatitude, endLongitude);
                            map.addMarker(new MarkerOptions().position(latLng).title(location));
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10));

//                            Toast.makeText(getApplicationContext(),endLatitude+ "---"
//
//                                    + endLongitude, Toast.LENGTH_SHORT).show();


                        }
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        return false;
                    }
                });
                //I wrote new OnMapReadyCallback() to not get confused with the other method below
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap googleMap) {
                        map = googleMap;
                    }
                });
            }
        });

    }

    public void getMyLocation() {
        // this if statement checks if permission to access location is accepted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String []{
                    Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_CODE);
            return;
        }
        Task<Location> task = client.getLastLocation(); // get the last location the device was in (current location)
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(final Location location) {
                if (location != null){
                    currentLocation = location;

                    //show the latitude and longitude for the current location and display it as toast
                    Toast.makeText(getApplicationContext(),currentLocation.getLatitude()
                            + "---" + currentLocation.getLongitude(), Toast.LENGTH_SHORT).show();
//
                    // get the map when everything is ready
                    mapFragment.getMapAsync(MainActivity.this);

                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        startLatitude = currentLocation.getLatitude();
        startLongitude = currentLocation.getLongitude();

        LatLng latLng = new LatLng(startLatitude, startLongitude); // new latlng with the current location
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("You are here");
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,10));
        googleMap.addMarker(markerOptions); //add the marker
        //map = googleMap;
    }


    // this method checks if the permission is granted then call getmylocation method
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_CODE:
                if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    getMyLocation();
                }
                break;
        }
    }


    public void CalculateDistance(View view) {

//        Toast.makeText(getApplicationContext(),endLatitude+ "---"
//
//                + endLongitude, Toast.LENGTH_SHORT).show();



        Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, result);

        Toast.makeText(getApplicationContext(), "Distance: " + result[0], Toast.LENGTH_SHORT).show();

        if(result[0] <= 500){
            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    MainActivity.this, "arrived");
            builder.setContentTitle("Almost there");
            builder.setContentText("Keep going you are almost there");
            builder.setSmallIcon(R.drawable.ic_launcher_background);
            builder.setAutoCancel(true);

            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(MainActivity.this);
            managerCompat.notify(1, builder.build());
        }


    }
}
