
package com.example.routes;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.app.NotificationManager;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.graphics.Color;
import android.location.Location;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.os.VibrationEffect;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.routes.databinding.ActivityRoutesMapsBinding;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.EncodedPolyline;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;

public class RoutesMaps extends FragmentActivity implements OnMapReadyCallback {

    private static final double MAX_DEVIATION_DISTANCE_METERS = 100;
    private static final int REQUEST_CODE_LOCATION_PERMISSION = 123;
    private static final int REQUEST_SEND_SMS_PERMISSION = 1;
    private DirectionsResult existingRouteData;
    private List<LatLng> routePoints;
//    private static final int POLYLINE_WIDTH = 10;
//    private static final int POLYLINE_COLOR = Color.BLUE;
    private AsyncTask<Void, Void, DirectionsResult> generateRouteTask;
    //private static final String API_KEY = "@string/apiKey";
    private static final String API_KEY = "AIzaSyAjCZ3-1lFhc87diARhwsoG4Fu4Y5VvXG8";

    private GoogleMap mMap;
    private ActivityRoutesMapsBinding binding;
    private LatLng startPoint;
    private LatLng endPoint;
    private Marker userMarker;
    private Button shareRouteButton;
    private Button startRouteButton;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GeoApiContext geoContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRoutesMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Check for and request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permissions are granted, proceed with map initialization
            initMap();
        } else {
            // Request location permissions from the user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_LOCATION_PERMISSION);
        }

        shareRouteButton = findViewById(R.id.shareRouteButton);
        shareRouteButton.setVisibility(View.INVISIBLE); // Initially, hide the button;
        startRouteButton = findViewById(R.id.startRouteButton);

        //start
        startRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("RoutesMaps", "Start Route Button Clicked");

                if (startPoint != null && endPoint != null) {
                    // Start tracking the user's location
                    trackUserLocation();
                } else {
                    // Show a message to the user indicating that both starting and ending points must be selected
                    Toast.makeText(getApplicationContext(), "Please select both starting and ending points.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //end
        shareRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("RoutesMaps", "Share Route Button Clicked");
                if (startPoint != null && endPoint != null) {
                    // Check if the task is already running, cancel it if needed
                    if (generateRouteTask != null && generateRouteTask.getStatus() == AsyncTask.Status.RUNNING) {
                        generateRouteTask.cancel(true);
                    }

                    // Create and execute a new GenerateRouteTask
                    generateRouteTask = new AsyncTask<Void, Void, DirectionsResult>() {
                        @Override
                        protected DirectionsResult doInBackground(Void... params) {
                            try {
                                // Create a Directions API request with the start and end points
                                DirectionsApiRequest request = new DirectionsApiRequest(geoContext);
                                request.origin(new com.google.maps.model.LatLng(startPoint.latitude, startPoint.longitude));
                                request.destination(new com.google.maps.model.LatLng(endPoint.latitude, endPoint.longitude));

                                // Choose the travel mode based on the user's selection
                                switch (TravelMode.DRIVING) {
                                    case DRIVING:
                                        request.mode(TravelMode.DRIVING);
                                        break;
                                    case WALKING:
                                        request.mode(TravelMode.WALKING);
                                        break;
                                    case BICYCLING:
                                        request.mode(TravelMode.BICYCLING);
                                        break;
                                    case TRANSIT:
                                        request.mode(TravelMode.TRANSIT);
                                        break;
                                }

                                // Send the request and return the result
                                return request.await();
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }

                        @Override
                        protected void onPostExecute(DirectionsResult result) {
                            if (result != null) {
                                existingRouteData = result;
                                // Display the route on the map
                                displayRoute(result);

                                // Share the generated route via SMS
                                shareRoute(result);
                            } else {
                                // Handle the case where there was an error with the network request
                                Toast.makeText(getApplicationContext(), "Error generating route.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    };

                    // Execute the task
                    generateRouteTask.execute();
                } else {
                    Toast.makeText(getApplicationContext(), "Please select both starting and ending points.", Toast.LENGTH_SHORT).show();
                }
            }
        });

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        geoContext = new GeoApiContext.Builder()
                .apiKey(API_KEY)
                .build();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable the My Location layer on the map if permissions are granted.
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            // Get the user's last known location and move the camera.
            fusedLocationProviderClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));

                                // Add a marker at the user's location
                                userMarker = mMap.addMarker(new MarkerOptions().position(userLatLng).title("My Location"));

                                // Set the startPoint to the user's location
                                startPoint = userLatLng;
                            }
                        }
                    });
        } else {
            // Request location permissions or handle the case where the user denied permissions.
            Toast.makeText(this, "Location permissions are required to use this feature.", Toast.LENGTH_SHORT).show();
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Set the destination point
                endPoint = latLng;

                // Remove any previously added markers
                if (userMarker != null) {
                    userMarker.remove();
                }

                // Add a marker at the selected destination
                userMarker = mMap.addMarker(new MarkerOptions().position(endPoint).title("Destination"));

                // Show the startRouteButton
                shareRouteButton.setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted, initialize the map
                initMap();
            } else {
                // Location permission denied, handle it appropriately (e.g., show a message)
                Toast.makeText(this, "Location permissions are required to use this feature.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void generateRoute(LatLng start, LatLng end) {
        // Create a Directions API request with the start and end points
        DirectionsApiRequest request = new DirectionsApiRequest(geoContext);
        request.origin(new com.google.maps.model.LatLng(start.latitude, start.longitude));
        request.destination(new com.google.maps.model.LatLng(end.latitude, end.longitude));

// Choose the travel mode based on the user's selection
        switch (TravelMode.DRIVING) {
            case DRIVING:
                request.mode(TravelMode.DRIVING);
                break;
            case WALKING:
                request.mode(TravelMode.WALKING);
                break;
            case BICYCLING:
                request.mode(TravelMode.BICYCLING);
                break;
            case TRANSIT:
                request.mode(TravelMode.TRANSIT);
                break;
        }

        try {
            // Send the request and handle the response (parsing and displaying the route)
            DirectionsResult result = request.await();

            displayRoute(result);
        } catch (Exception e) {
            // Handle any exceptions or errors here
            e.printStackTrace();
        }



    }

    private void displayRoute(DirectionsResult result) {
        // Clear previous polylines
        mMap.clear();

        // Extract the route's polyline from the result
        DirectionsRoute route = result.routes[0]; // You may need to handle multiple routes
        EncodedPolyline encodedPolyline = route.overviewPolyline;
        List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(encodedPolyline.getEncodedPath());

        // Convert the list of LatLng to LatLng objects
        List<LatLng> path = new ArrayList<>();
        for (com.google.maps.model.LatLng decodedLatLng : decodedPath) {
            path.add(new LatLng(decodedLatLng.lat, decodedLatLng.lng));
        }

        // Create and add a polyline to the map
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(path)
                .color(Color.BLUE)
                .width(10); // You can customize the color and width as needed
        mMap.addPolyline(polylineOptions);
        startRouteButton.setVisibility(View.VISIBLE);
    }

    // Inside your RoutesMaps class
    private void shareRoute(DirectionsResult result) {
        if (result != null && result.routes != null && result.routes.length > 0) {
            DirectionsRoute route = result.routes[0];
            String routeSummary = route.summary;
            String routeDetails = route.legs[0].startAddress + " to " + route.legs[0].endAddress;

            // Construct the route URL dynamically
            String routeUrl = "https://www.google.com/maps/dir/?api=1&origin=" +
                    startPoint.latitude + "," + startPoint.longitude +
                    "&destination=" + endPoint.latitude + "," + endPoint.longitude;

            // Send the route URL via SMS using your existing logic
            sendRouteViaSms(routeUrl); // Call your existing sendRouteViaSms method

            // Optionally, you can also display a confirmation message to the user
            Toast.makeText(getApplicationContext(), "Route shared via SMS.", Toast.LENGTH_SHORT).show();
        } else {
            // Handle the case where there is no route to share
            Toast.makeText(getApplicationContext(), "No route to share.", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendRouteViaSms(String routeUrl) {
        String emergencyContactNumber = "0765652261"; // Update with the correct number
        String liveLocationMessage = "Follow my live location route: " + routeUrl;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, REQUEST_SEND_SMS_PERMISSION);
        } else {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emergencyContactNumber, null, routeUrl, null, null);
            Toast.makeText(this, "Live location shared with contact", Toast.LENGTH_SHORT).show();
        }
    }
//    private void trackUserLocation() {
//        LocationRequest locationRequest = LocationRequest.create()
//                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//                .setInterval(10000) // Update interval in milliseconds (e.g., every 10 seconds)
//                .setFastestInterval(5000); // Fastest update interval in milliseconds
//        if (existingRouteData != null && existingRouteData.routes != null && existingRouteData.routes.length > 0) {
//            DirectionsRoute route = existingRouteData.routes[0];
//
//            // Extract the route's polyline and convert it to LatLng objects (similar to displayRoute)
//            routePoints = convertPolylineToLatLng(route.overviewPolyline);
//
//            // Create a LocationCallback to receive location updates
//            LocationCallback locationCallback = new LocationCallback() {
//                @Override
//                public void onLocationResult(LocationResult locationResult) {
//                    if (locationResult != null) {
//                        Location currentLocation = locationResult.getLastLocation();
//
//                        // Update the user's marker on the map with their current location
//                        if (userMarker != null) {
//                            userMarker.setPosition(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()));
//                        }
//
//                        // Check for deviation from the route
//                        double distanceToRoute = calculateDistanceToRoute(currentLocation, routePoints);
//                        if (distanceToRoute > MAX_DEVIATION_DISTANCE_METERS) {
//                            // Notify the user that they've deviated from the route
//                            showToast("You've deviated from the route!");
//                        }
//                    }
//                }
//            };
//
//            // Request location updates with the specified LocationCallback
//            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//        } else {
//            // Handle the case where the existing route data is not available
//            showToast("Route data is not available. Please generate a route first.");
//        }
//    }
private void trackUserLocation() {
    LocationRequest locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
            .setInterval(10000) // Update interval in milliseconds (e.g., every 10 seconds)
            .setFastestInterval(5000); // Fastest update interval in milliseconds

    if (existingRouteData != null && existingRouteData.routes != null && existingRouteData.routes.length > 0) {
        DirectionsRoute route = existingRouteData.routes[0];

        // Extract the route's polyline and convert it to LatLng objects (similar to displayRoute)
        routePoints = convertPolylineToLatLng(route.overviewPolyline);

        // Create a LocationCallback to receive location updates
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    Location currentLocation = locationResult.getLastLocation();

                    // Update the user's marker on the map with their current location
                    if (userMarker != null) {
                        LatLng userLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                        userMarker.setPosition(userLatLng);
                        // Rotate the marker to align with the user's movement (bearing)
                        if (locationResult.getLastLocation().hasBearing()) {
                            userMarker.setRotation(locationResult.getLastLocation().getBearing());
                        }

                        // Zoom and animate the camera to follow the user's location
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f));

                        // Check for deviation from the route
                        double distanceToRoute = calculateDistanceToRoute(currentLocation, routePoints);
                        if (distanceToRoute > MAX_DEVIATION_DISTANCE_METERS) {
                            // Notify the user that they've deviated from the route
                            showToast("You've deviated from the route!");

                        }
                    }
                }
            }
        };

        // Request location updates with the specified LocationCallback
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    } else {
        // Handle the case where the existing route data is not available
        showToast("Route data is not available. Please generate a route first.");
    }
}

    private double calculateDistanceToRoute(Location userLocation, List<LatLng> routePoints) {
        double minDistance = Double.MAX_VALUE;
        for (LatLng routePoint : routePoints) {
            double distance = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), routePoint.latitude, routePoint.longitude);
            if (distance < minDistance) {
                minDistance = distance;
            }
        }
        return minDistance;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Calculate the distance between two latitude and longitude points (Haversine formula)
        double earthRadius = 6371e3; // Radius of the Earth in meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Calculate the distance in meters
        double distance = earthRadius * c;

        return distance;
    }



        private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private List<LatLng> convertPolylineToLatLng(EncodedPolyline encodedPolyline) {
        List<com.google.maps.model.LatLng> decodedPath = PolylineEncoding.decode(encodedPolyline.getEncodedPath());
        List<LatLng> path = new ArrayList<>();

        for (com.google.maps.model.LatLng decodedLatLng : decodedPath) {
            path.add(new LatLng(decodedLatLng.lat, decodedLatLng.lng));
        }

        return path;
    }





}

