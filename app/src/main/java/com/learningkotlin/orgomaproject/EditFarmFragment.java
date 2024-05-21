package com.learningkotlin.orgomaproject;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
// Class for editing already existing farms
// Farmer can only edit his own farm
public class EditFarmFragment extends Fragment {
    Spinner olives;
    Button save, delete;
    TextView farmNameEdit;
    RadioButton organicBtn, nonOrganicBtn;
    // Firebase
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference farmsCollection = db.collection("Farms");
    FirebaseUser currentUser;
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FragmentManager fragmentManager;
    private List<CustomLatLng> farmList = new ArrayList<>();
    String selectedVariety;
    RadioGroup oliveVarieties;
    int typeId;
   //  private boolean sprayed;
    private SaveAndDisplay saveAndDisplay;
    public EditFarmFragment() {
        // Required empty public constructor
    }

    public static EditFarmFragment newInstance(String param1, String param2) {
        EditFarmFragment fragment = new EditFarmFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_edit_farm, container, false);
        olives = view.findViewById(R.id.varietiesSpinner);
        // Setting up spinner values, so choices could be displayed
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.olives, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        olives.setAdapter(adapter);
        // Buttons
        save = view.findViewById(R.id.editFarmSave);
        delete = view.findViewById(R.id.editDelete);
        // olives
        oliveVarieties = view.findViewById(R.id.editRegistrationGroup);

        // When farmer decides to save changes
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Values that will be changing data in firestore
                String newName = farmNameEdit.getText().toString().trim();
                boolean newOrganicStatus = organicBtn.isChecked();

                // Getting current user(farmer) status
                currentUser = auth.getCurrentUser();
                if (currentUser != null){
                    // getting user id
                    String id = currentUser.getUid();

                    // checking id of checked radio button
                    typeId = oliveVarieties.getCheckedRadioButtonId();
                    // Making sure that at least one of types are selected
                    if (typeId == -1){
                        Toast.makeText(requireContext(),
                                "You must chose farm type",
                                Toast.LENGTH_SHORT).show();
                    }

                    // getting spinner item and casting it to string
                    selectedVariety = (String) olives.getSelectedItem();
                    //  checking if farmer selected variety from spinner
                    if (TextUtils.equals(selectedVariety, "Select")){
                        Toast.makeText(requireContext(),
                                "You need to choose olives varieties",
                                Toast.LENGTH_SHORT).show();
                    }

                    if (TextUtils.isEmpty(newName)){
                        Toast.makeText(requireContext(),
                                "You need to enter name",
                                Toast.LENGTH_SHORT).show();
                    }

                    if (!TextUtils.isEmpty(newName) && !TextUtils.equals(selectedVariety, "Select")
                            && typeId != -1){

                        farmsCollection.whereEqualTo("idOfOwner", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                DocumentSnapshot farmDocument = task.getResult().getDocuments().get(0);
                                String idOfFarm = farmDocument.getId();
                                // updating farm without changing workerId, deleting documentNumber and resetting sprayed value
                                farmsCollection.document(idOfFarm).update("farmName", newName, "oliveVarieties", Collections.singletonList(selectedVariety),
                                        "isOrganic", newOrganicStatus);

                                // Notifying that save button is clicked
                                if (saveAndDisplay != null){
                                    saveAndDisplay.saveClicked();
                                }

                                saveAndDisplay.resetMap();
                                // after updating info send farmer back to Map
                                backToMainMap();
                            }
                        });
                    }
                }
            }
        });
        // When farmer decides to delete farm
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Getting current user
                    currentUser = auth.getCurrentUser();
                    if (currentUser != null){
                        // Getting user id
                        String id = currentUser.getUid();

                        farmsCollection.whereEqualTo("idOfOwner", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                // getting document
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                // getting farm id
                                String farmId = document.getId();

                                farmsCollection.document(farmId).delete();
                                saveAndDisplay.resetMap();
                                backToMainMap();
                            }
                        });
                    }
            }
        });

        Bundle info = getArguments();
        // The data from MainMapFragment
        if (info != null) {
            // Getting info from MainMapFragment(Firebase)
            String farmName = info.getString("name", "");
            boolean isOrganic = info.getBoolean("isOrganic", false);

            ArrayList<? extends Parcelable> coordinatesFromFirestore = getArguments().getParcelableArrayList("farmCoordinates");
            // Creating array list of CustomLatLang class object(where latitude and longitude stored),
            // array list for storing customCoordinates(needed custom class for coordinates because firebase threw an error)
            ArrayList<CustomLatLng> customCoordinates = new ArrayList<>();
            // if coordinates list is not empty(shouldn't be, only way to be so, when inserting through firestore,
            // but just to be safe, still checking)
            if (coordinatesFromFirestore != null) {
                // Converting Parcelable objects back to CustomLatLng objects
                for (Parcelable iterate : coordinatesFromFirestore) {
                    // checking if it's instance from CustomLatLng
                    // if it is cast to CustomLatLang and add it to the list
                    if (iterate instanceof CustomLatLng) {
                        customCoordinates.add((CustomLatLng) iterate);
                    }
                }
            }

            farmList.clear();
            farmList.addAll(customCoordinates);

            ArrayList<String> varietiesFromFirestore = getArguments().getStringArrayList("oliveVarieties");


            // Filling in fields with data
            farmNameEdit = view.findViewById(R.id.editFarmName);
            farmNameEdit.setText(farmName);

            organicBtn = view.findViewById(R.id.organic);
            nonOrganicBtn = view.findViewById(R.id.nonOrganic);

            if (isOrganic){
                organicBtn.setChecked(true);
            }else {
                nonOrganicBtn.setChecked(true);
            }
        }

        return view;
    }

    // interface to communicate between map and edit fragments
    public interface SaveAndDisplay {
        void saveClicked();
        void resetMap();
    }
    // Method for callback
    public void setSaveAndDisplay(SaveAndDisplay saveAndDisplay){
        this.saveAndDisplay = saveAndDisplay;
    }
    private void backToMainMap(){
        fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.popBackStack();
    }
}