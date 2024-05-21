package com.learningkotlin.orgomaproject;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {
    // google map
    MainMapFragment mapFragment;
    // Buttons
    Button registrationBtn, loginBtn;
    // Firebase
    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    // accessing Firebase Firestore database, to obtain references
    private FirebaseFirestore accounts_db = FirebaseFirestore.getInstance();
    // Represent specific collection within fb fs, in this case we referring to Users collection
    private CollectionReference collectionReference = accounts_db.collection("Users");

    // Notification
    // Define activity result launcher for permission
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(
            // asking for permission
            new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean o) {
                    // if permission granted
                    if (o){
                        Toast.makeText(MainActivity.this,
                                "Notification permission granted",
                                Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(MainActivity.this,
                                "Permission for Notifications not granted",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Creating new instance of MainMapFragment
        mapFragment = new MainMapFragment();

        // Adding Fragment to fragment container
        getSupportFragmentManager().beginTransaction()
                .add(R.id.LocalMap, mapFragment)
                .commit();

        // Registration Button action
        registrationBtn = findViewById(R.id.RegisterBtn);

        if (currentUser != null){
            String id = currentUser.getUid();
            collectionReference.document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()){
                        // getting type from Firestore
                        String type = documentSnapshot.getString("type");
                        // If type == sprayer
                        if ("Sprayer".equals(type)){
                            Intent intent = new Intent(MainActivity.this, WorkerLoggedIn.class);
                            startActivity(intent);
                        }else {
                            Toast.makeText(MainActivity.this,
                                    "Welcome Guest!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }

        registrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                directToRegistrationActivity();
            }
        });
        // Login Button action
        loginBtn = findViewById(R.id.LoginBtn);
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.LocalMap, LoginFragment.newInstance(null, null))
                        .addToBackStack(null)  // to allow user go back
                        .commit();

            }
        });

        // asking for permission to send notifications
        activityResultLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);

    }

    private void directToRegistrationActivity(){
        finish();
        Intent intent = new Intent(MainActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }


}