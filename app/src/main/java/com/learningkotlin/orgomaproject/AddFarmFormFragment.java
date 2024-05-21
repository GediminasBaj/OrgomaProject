package com.learningkotlin.orgomaproject;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Collections;
import java.util.List;

public class AddFarmFormFragment extends Fragment {
    // Spinner, RadioGroup
    Spinner olives;
    RadioGroup oliveVarieties;
    String selectedVariety;
    int typeId;
    // Buttons
    Button nextAction;
    // Edit Texts
    EditText farmName;
    // Map
    MainMapFragment mapFragment;
    // Firebase
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseUser currentUser;
    private String userId;
    private boolean isOrganic;
    private boolean sprayed;
    public AddFarmFormFragment() {
        // Required empty public constructor
    }
    public static AddFarmFormFragment newInstance(String userId) {
        AddFarmFormFragment fragment = new AddFarmFormFragment();
        Bundle args = new Bundle();
        args.putString("idOfOwner", userId);
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getting current user
        currentUser = auth.getCurrentUser();
        if (currentUser != null){
            userId = currentUser.getUid();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_add_farm_form, container, false);
        olives = view.findViewById(R.id.varietiesSpinner);
        // Setting up spinner values, so choices could be displayed
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.olives, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        olives.setAdapter(adapter);

        // farmName
        farmName = view.findViewById(R.id.farmName);
        // OliveVarieties
        oliveVarieties = view.findViewById(R.id.registrationGroup);
        // Setting up next button
        nextAction = view.findViewById(R.id.addFarmNextBtn);
        // button action
        nextAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // checking id of checked radio button
                typeId = oliveVarieties.getCheckedRadioButtonId();
                // Making sure that at least one of types are selected
                if (typeId == -1){
                    Toast.makeText(requireContext(),
                            "You must chose farm type",
                            Toast.LENGTH_SHORT).show();
                }

                isOrganic = typeId == R.id.organic;

                // getting spinner item and casting it to string
                selectedVariety = (String) olives.getSelectedItem();
                //  checking if farmer selected variety from spinner
                if (TextUtils.equals(selectedVariety, "Select")){
                    Toast.makeText(requireContext(),
                            "You need to choose olives varieties",
                            Toast.LENGTH_SHORT).show();
                }
                // Checking if he entered farm Name
                String name = farmName.getText().toString().trim();
                if (TextUtils.isEmpty(name)){
                    Toast.makeText(requireContext(),
                            "You need to enter name",
                            Toast.LENGTH_SHORT).show();
                }

                if (!TextUtils.isEmpty(name) && !TextUtils.equals(selectedVariety, "Select")
                    && typeId != -1){
                    Farm farm = new Farm(userId, name, null, Collections.singletonList(selectedVariety), isOrganic, sprayed, null);
                    toMap(name, isOrganic, Collections.singletonList(selectedVariety));
                    Toast.makeText(requireContext(),
                            "Please place Markers on your farm fields",
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
        return view;
    }

    private void toMap(String farmName, Boolean isOrganic, List<String> oliveVarieties){
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();

        // Begin the transaction
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // checking if map is null
        if (mapFragment == null) {
            // initialize map
            mapFragment = new MainMapFragment();
            // Passing info to mapFragment
            mapFragment.setFarmName(farmName);
            // sending boolean value(organic or not)
            mapFragment.setIsOrganic(isOrganic);
            // Sending olive Varieties
            mapFragment.setOliveVarieties(oliveVarieties);
            // setting boolean to true
            mapFragment.setFromForm(true);
            // moving to map Fragment
            fragmentTransaction.replace(R.id.map_for_worker, mapFragment);
            // adding toBackStack, so user could go back
            fragmentTransaction.addToBackStack(null);
            // changing fragments
            fragmentTransaction.commit();
        } else {
            // if map already exist, just setting up boolean to true
            mapFragment.setFromForm(true);
        }

    }

}