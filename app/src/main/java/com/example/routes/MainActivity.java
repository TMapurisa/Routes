package com.example.routes;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_MAP = 100;

    private Button walkWithMeButton, panicButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        walkWithMeButton = findViewById(R.id.walkWithMeButton);
        walkWithMeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the MapsActivity and wait for the result
                Intent intent = new Intent(MainActivity.this, RoutesMaps.class);
                startActivityForResult(intent, REQUEST_CODE_MAP);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_MAP && resultCode == RESULT_OK) {
            // Handle the result here, if needed
        }
    }
}