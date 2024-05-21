package com.learningkotlin.orgomaproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class RegistrationActivity extends AppCompatActivity {

    // Buttons
    Button registrationBtn, backToMain;
    // Fragments
    Registration registrationFragment;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        backToMain = findViewById(R.id.backToMain);

        backToMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveBackToMain();
            }
        });

        // initialize fragment
        registrationFragment = new Registration();
        // Adding registration fragment, to display immediately
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.registrationFrame, registrationFragment).commit();

    }

    // back to main method
    private void moveBackToMain(){
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        startActivity(intent);
    }


    private void moveToRegistration(){
        // Starting transaction
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();

        fragmentTransaction.replace(R.id.registrationFrame, registrationFragment);

        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

}