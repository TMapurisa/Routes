package com.example.routes;

import com.google.maps.GeoApiContext;

public class GeoContextManager {
    private static GeoApiContext geoContext;

    public static GeoApiContext getGeoContext() {
        if (geoContext == null) {
            geoContext = new GeoApiContext.Builder()
                    .apiKey("AIzaSyAjCZ3-1lFhc87diARhwsoG4Fu4Y5VvXG8")
                    .build();
        }
        return geoContext;
    }
}