package com.learningkotlin.orgomaproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class WorkerLoggedIn extends AppCompatActivity {

    // google map and login/log out/ location
    MainMapFragment mapFragment;
    GoogleSignInOptions signInOptions;
    GoogleSignInClient signInClient;
    // Buttons
    Button logout, addFarm;
    // Firebase
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
    FirebaseUser currentUser = firebaseAuth.getCurrentUser();
    // Firebase Connection
    // accessing Firebase Firestore database, to obtain references
    private FirebaseFirestore accounts_db = FirebaseFirestore.getInstance();
    // Represent specific collection within fb fs, in this case we referring to Users collection
    private CollectionReference collectionReference = accounts_db.collection("Users");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_logged_in);

        // Google sign in options
        signInOptions =  new GoogleSignInOptions.
                Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        signInClient = GoogleSignIn.getClient(this,signInOptions);

        // instance of map fragment
        mapFragment = new MainMapFragment();

        // Displaying Fragment in Framelayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.map_for_worker, mapFragment)
                .commit();

        // logout
        logout = findViewById(R.id.worker_sign_out);

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workerSignOut();
            }
        });

        // Adding Farm code
        addFarm = findViewById(R.id.addFarm);
        addFarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddFarmFragment();
            }
        });
    }
    // Sign out method
    private void workerSignOut(){
        // checking if user logged in
        if (currentUser != null){
            // getting user id
            String id = currentUser.getUid();

            // changing boolean value if false
            collectionReference.document(id).update("signedIn", false).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    // Sign out with firebase method
                    firebaseAuth.signOut();
                    // google sign out
                    signInClient.signOut().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            backToMain();
                        }
                    });
                }
            });
        }
    }
    // when logging out directing to main app activity
    private void backToMain(){
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }
    private void displayAddFarmFragment(){
        if (currentUser != null){
            currentUser = firebaseAuth.getCurrentUser();
            // getting current user id
            String id = currentUser.getUid();
            collectionReference.document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    // Getting worker status and type
                    boolean log = documentSnapshot.getBoolean("signedIn");
                    String type = documentSnapshot.getString("type");
                    // Checking user type and if he's logged in
                    if ("Farmer".equals(type) && log){
                        // changing map fragment with addFarm fragment
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.map_for_worker, new AddFarmFormFragment())
                                .addToBackStack(null)
                                .commit();
                    }else {
                        Toast.makeText(WorkerLoggedIn.this,
                                "Only Farmers Can Add Farms",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
}