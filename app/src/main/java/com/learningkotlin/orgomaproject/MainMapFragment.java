package com.learningkotlin.orgomaproject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainMapFragment extends Fragment implements GoogleMap.OnMapClickListener, EditFarmFragment.SaveAndDisplay {
    // tracking location
    Location currentLocation;
    private GoogleMap googleMap1;
    FusedLocationProviderClient fusedLocationProviderClient;

    // Firebase Connection
    // accessing Firebase Firestore database, to obtain references
    private final FirebaseFirestore accounts_db = FirebaseFirestore.getInstance();
    // Representing the document in firestore db, provides methods, (w,r,u,d)
    private DocumentReference registeredAccounts = accounts_db.collection("Users")
            .document("Workers");

    // Represent specific collection within fb fs, in this case we referring to Users collection
    private CollectionReference collectionReference = accounts_db.collection("Users");

    // Firebase code, for creating collection Farms
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CollectionReference farmsCollection = db.collection("Farms");

    // Firebase auth
    FirebaseAuth auth = FirebaseAuth.getInstance();
    FirebaseAuth.AuthStateListener authStateListener;
    FirebaseUser currentUser;
    private boolean isOrganic;
    private List<Farm> AllFarmsList = new ArrayList<>();

    // To check if farmer tries to place marker after coming from Form Fragment, and button for farmer to complete farm insertion
    boolean fromForm = false;
    Button complete, editFarm;
    // List for farms
    private List<CustomLatLng> farmList = new ArrayList<>();
    private List<String> oliveVarieties;
    private LatLng farmLocation;
    private String farmName;
    private boolean completeTrue = false;
    private boolean sprayed;
    // Instance of EditFragment
    EditFarmFragment editFarmFragment = new EditFarmFragment();
    private DocumentReference locationOfUser;
    // Instances of LocationListener and Manager
    private LocationListener locationListener;
    private LocationManager locationManager;
    // Time that will be used for updates(how long it'll take to call for updates)
    private final long MIN_TIME = 2000; // 5 minutes (because firebase have limits of 20k writes each day)
    private final long set_to_sprayed = 10; // distance when farm is set as sprayed

    private OnMapReadyCallback callback = new OnMapReadyCallback() {

        @Override
        public void onMapReady(GoogleMap googleMap) {
            if (!isAdded()) {
                return;
            }
            // displaying farms to everyone
            displayFarmsToEveryone();
            // for zooming in map for convience
            LatLng zoomIn = new LatLng(37.7907762, 26.7051974);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomIn, 20));

            googleMap1 = googleMap;
            // for finding place easier(testing purposes)
            // LatLng test = new LatLng(37.7907762, 26.7051974);
            // googleMap.addMarker(new MarkerOptions().position(test).title("Your Location"));
            // googleMap.moveCamera(CameraUpdateFactory.newLatLng(test));

            if (currentLocation != null) {
                // Getting worker info from FireStore
                currentUser = auth.getCurrentUser();
                if (currentUser != null) {
                    // getting current user id
                    String id = currentUser.getUid();
                    // using that id to access firebase info
                    collectionReference.document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                // Getting worker status and type
                                boolean log = documentSnapshot.getBoolean("signedIn");
                                String type = documentSnapshot.getString("type");
                                // Checking worker type and if he's logged in
                                if ("Sprayer".equals(type) && log) {
                                    LatLng isIn = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                                    googleMap.addMarker(new MarkerOptions().position(isIn).title("Your Location"));
                                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(isIn));
                                    // Sprayers shouldn't see these buttons
                                    complete.setVisibility(View.GONE);
                                    editFarm.setVisibility(View.GONE);
                                }
                                // if farmer logged in allowing clicks actions on map
                                if ("Farmer".equals(type) && log) {
                                    // Allowing click on Map and displaying button for ending the farm insertion
                                    if (fromForm) {
                                        complete.setVisibility(View.VISIBLE);
                                        googleMap.setOnMapClickListener(MainMapFragment.this);
                                    } else {
                                        // Not allowing click on Map and not displaying button
                                        googleMap.setOnMapClickListener(null);
                                        complete.setVisibility(View.GONE);
                                    }
                                } else {
                                    // Not allowing click on Map
                                    googleMap.setOnMapClickListener(null);
                                }
                            }
                        }
                    });
                } else {
                    Toast.makeText(requireContext(),
                            "Worker need to login for location display",
                            Toast.LENGTH_SHORT).show();
                }
            }

        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_map, container, false);
        // initialization of auth SDK
        auth = FirebaseAuth.getInstance();
        // Location tracking
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        // Buttons for Farmer actions
        complete = view.findViewById(R.id.farmerDoneBtn);
        editFarm = view.findViewById(R.id.farmerEditBtn);
        // Getting current user
        currentUser = auth.getCurrentUser();
        // To prevent UI bug, that displays done button even it shouldn't
        if (currentUser != null) {
            // Getting current user id
            String id = currentUser.getUid();
            // using that id to access firebase info
            collectionReference.document(id).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot) {
                    if (documentSnapshot.exists()) {
                        boolean log = documentSnapshot.getBoolean("signedIn");
                        String type = documentSnapshot.getString("type");

                        if ("Farmer".equals(type) && log) {
                            // Check if the farmer has farms inserted
                            // Edit Farm button
                            editFarm.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // Edit farms if he have them
                                    String id = currentUser.getUid();
                                    hasFarms(id);
                                }
                            });
                        } else {
                            // Show the buttons for other user types or when not logged in
                            complete.setVisibility(View.GONE);
                            editFarm.setVisibility(View.GONE);
                        }
                    }
                }
            });
        } else {
            // Hide the buttons when the user is not logged in
            complete.setVisibility(View.GONE);
            editFarm.setVisibility(View.GONE);
        }
        // For constant location updates
        if (currentUser != null) {
            // getting user id
            String id = currentUser.getUid();
            // finding user in collection and setting it to locationOfUser variable - will be used for location updates
            locationOfUser = accounts_db.collection("Users").document(id);
            collectionReference.document(id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    // getting task result
                    DocumentSnapshot documentSnapshot = task.getResult();
                    // if user exist
                    if (documentSnapshot.exists()) {
                        // getting user type (Farmer or Sprayer)
                        String type = documentSnapshot.getString("type");
                        // if Sprayer, calling method to update worker location
                        if ("Sprayer".equals(type)) {
                            updateWorkerLocation();
                        }
                    }
                }
            });
        }
        // Obtaining location service, for device location tracking
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);

        if (currentUser != null) {
            // Getting current user id
            String id = currentUser.getUid();
            complete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // creating farm object
                    Farm farm = new Farm(id, farmName, farmList, oliveVarieties, isOrganic, sprayed, null);
                    completeTrue = true;
                    if (completeTrue) {
                        complete.setVisibility(View.GONE);
                        fromForm = false;
                        googleMap1.setOnMapClickListener(null);
                    }
                    // adding it to firestore
                    addFarmToFirestore(farm);
                    // clearing map
                    resetMap();
                    editFarm.setVisibility(View.VISIBLE);
                }
            });
        } else {
            // if user not signed in
            Toast.makeText(requireContext(), "User not signed in", Toast.LENGTH_SHORT).show();
        }
        // checking user state (signed in or not)
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {
                    Log.d("AuthStateChange", "User is logged in");
                    getLocation();
                } else {
                    Log.d("AuthStateChange", "User is logged out");
                }
            }
        };
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    // getting farmers farms info
    private void getFarmInfo(String id) {
        if (currentUser != null) {
            // searching farms with userId
            farmsCollection.whereEqualTo("idOfOwner", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                // if there is any
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    // if task success and there is any farms(not empty)
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // now works only if farmer has one farm.
                        DocumentSnapshot farmDocument = task.getResult().getDocuments().get(0);
                        Farm farm = farmDocument.toObject(Farm.class);
                        // Getting info about farms
                        farmName = farm.getFarmName();
                        isOrganic = farm.isOrganic();
                        farmList = farm.getCoordinates();
                        oliveVarieties = farm.getOliveVarieties();
                        // sending info to edit fragment
                        toEditFarms();
                    } else {
                        Toast.makeText(requireContext(),
                                "Something went wrong",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void getAllFarms() {
        if (currentUser != null) {
            farmsCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot snapshot : task.getResult()) {
                            // converting firebase info to Farm object
                            Farm farm = snapshot.toObject(Farm.class);
                            AllFarmsList.add(farm);
                        }
                    }
                }
            });
        }
    }

    private void hasFarms(String id) {
        farmsCollection.whereEqualTo("idOfOwner", id).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    complete.setVisibility(View.GONE);
                    editFarm.setVisibility(View.VISIBLE);

                    getFarmInfo(id);
                } else {
                    // Farmer doesn't have farms inserted, hide the edit button
                    editFarm.setVisibility(View.GONE);
                }
            }
        });
    }

    // start of Location related code
    // to make sure that notification doesn't repeat(because of location checking time if it set, for 10 min then it wouldn't be needed)
    private boolean notified = false;
    // location updates
    private void updateWorkerLocation() {
       // channel();
        // using fusedLocation Provider
        // Setting new location on map and sending info to firebase
        // move this out( and out on locationChanged) or in for loop if you want to be notified once(made this because my pc hardly can handle emulation)
        notified = false;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext());
        locationListener = new com.google.android.gms.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (locationOfUser != null) {
                    // locationOfUser.set(new GeoPoint(location.getLatitude(), location.getLongitude()));
                    getAllFarms();
                    // sending location to firestore
                    sendLocationToFirestore(location.getLatitude(), location.getLongitude());
                    // displaying location from firestore on map
                    if (currentUser != null) getLocationFromFirestore(currentUser.getUid());
                    // move this in our out for loop if you want constant notifications
                    boolean nearOrganic = false;
                    // going through array of farms
                    for (Farm farm : AllFarmsList) {

                        // for non-organic and organic farms distance calculation
                        float minDistanceFromFarm = Float.MAX_VALUE;
                        String workerId = null;
                        // too avoid null exception and prevent app crashes
                        if (currentUser != null){
                            workerId = currentUser.getUid();

                            float distanceFromFarm = distanceFromFarm(location, farm.getCoordinates());
                            // using Math class method to calculate distance
                            minDistanceFromFarm = Math.min(minDistanceFromFarm, distanceFromFarm);

                            // checking if farm that workers near is organic
                            if (farm.isOrganic()) {
                                // Check if the worker is within 20 meters of this organic farm
                                if (farm.isOrganic() && minDistanceFromFarm <= 20) {
                                    nearOrganic = true;
                                    Log.d("Setting to true", "It's" + nearOrganic);
                                    farm.setWorkerId(workerId);
                                    // using this to update info in firebase firestore
                                    changeStatus(farm);
                                }
                            }
                            // if sprayer 10 metres away from farm, setting sprayed to true and updating firebase info
                            if (minDistanceFromFarm <= set_to_sprayed && farm.getidOfDocument() != null){
                                if (!farm.isOrganic()){
                                    farm.setSprayed(true);
                                    farm.setWorkerId(workerId);
                                    // using this to update info in firebase firestore
                                    changeStatus(farm);
                                }else {
                                    // farm.setWorkerId(workerId);
                                }
                            }
                        }
                }
                    // checking if Sprayer was notified or not, if not notify
                    // (can move out of loop if don't want repeated notification every time when worker location updated)
                    if (nearOrganic && !notified) {
                        notification(getString(R.string.app_name), " You're near organic farm!\n Don't spray!");
                        notified = true;
                    }else if (!nearOrganic){
                        notified = false;
                    }
                }
            }
        };
        // checking if permissions to location access is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // update location
            fusedLocationProviderClient.requestLocationUpdates(locationRequest(), locationListener, null);
        }

    }
    // calculating worker distance from any kind of farms
    private float distanceFromFarm(Location currentLocation, List<CustomLatLng> locationsOfFarms) {
        // for farms
        float maxDistance = Float.MAX_VALUE;

        // getting all farm locations and calculating min distance from organic and non organic farms
        for (CustomLatLng coordinates : locationsOfFarms) {
            Location farm = new Location("Organic");
            farm.setLatitude(coordinates.getLatitude());
            farm.setLongitude(coordinates.getLongitude());

            float distance = currentLocation.distanceTo(farm);
            // calculating min distance for farms
            maxDistance = Math.min(maxDistance, distance);
        }

        return maxDistance;
    }
    // throwing out notification, notification channel, reference https://www.youtube.com/watch?v=SvCV0LKY7Xo
    private void channel(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel notificationChannel = new NotificationChannel("near", appName, NotificationManager.IMPORTANCE_DEFAULT);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
    private void notification(String title, String message) {
        channel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), "near")
                .setSmallIcon(R.drawable.too_near)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat manager = NotificationManagerCompat.from(requireContext());
        // included because of android studio, didn't change anything
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.notify(1, builder.build());
    }
    // used this as reference for constant location tracking https://programmerworld.co/android/create-location-tracking-android-app-by-exchanging-location-information-over-firebase-database/
    // location request
    private LocationRequest locationRequest(){
        // creates location update request and setting time when it should be done, in this case 5 mins
        return new LocationRequest().setInterval(MIN_TIME).setFastestInterval(MIN_TIME).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
    // Permission for location
    private void getLocation() {
        // Permission check
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> locationTask = fusedLocationProviderClient.getLastLocation();
        locationTask.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    Log.d("LocationUpdate", "Location received: " + location.getLatitude() + ", " + location.getLongitude());
                    currentLocation = location;
                    // updating firebase location
                    sendLocationToFirestore(location.getLatitude(), location.getLongitude());
                    displayFarmsToEveryone();
                    // Reference to layout by its id
                    SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
                    // Asynchronously initialize google map
                    mapFragment.getMapAsync(callback);
                }
            }
        });
    }
    // Checking Permissions result
    private ActivityResultLauncher<String> resultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isAllowed -> {
        // Getting permission code
        if (isAllowed) {
            getLocation();
        } else {
            Toast.makeText(requireContext(), "No location permission", Toast.LENGTH_SHORT).show();
        }
    });
    // Sending location info to firestore
    private void sendLocationToFirestore(double latitude, double longitude){
        // if user logged in
        if (currentUser != null){
            String id = currentUser.getUid();
            // upading location in firestore
            collectionReference.document(id).update("latitude", latitude, "longitude", longitude);
        }
    }
    // getting location from firestore
    private void getLocationFromFirestore(String workerId){
        // getting document info by workerId
        registeredAccounts = FirebaseFirestore.getInstance().collection("Users").document(workerId);
        // if it's successful
        registeredAccounts.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                // if document with such worker id exist
                if (documentSnapshot.exists()){
                    double latitude = documentSnapshot.getDouble("latitude");
                    double longitude = documentSnapshot.getDouble("longitude");
                    LatLng newLocation = new LatLng(latitude,longitude);

                    updateWorkerMapLocation(newLocation);
                }
            }
        });
    }
    // upading location on map
    private void updateWorkerMapLocation(LatLng workerLocation){
        // resetting map and displaying farms
        resetMap();
        // displaying new worker position form firestore in map
        googleMap1.addMarker(new MarkerOptions().position(workerLocation).title("Your current location"));
    }
    // end of location related code

    // Array for conversion to ParcelableList so coordinates can be sent to editFarmFragment
    private ArrayList<Parcelable> convertToParcelableList(List<CustomLatLng> customLatLngList) {
        // Parcelable objects Array list
        ArrayList<Parcelable> parcelableList = new ArrayList<>();
        // converting customLatLng objects to Parcelable objects
        for (CustomLatLng farmplaces : customLatLngList) {
            // adding converted objects to list
            parcelableList.add(farmplaces);
        }
        return parcelableList;
    }

    private void toEditFarms(){

        editFarmFragment.setSaveAndDisplay(this);
        // Passing existing info that we got from firebase to editFarmFragment
        Bundle bundle = new Bundle();
        bundle.putString("name", farmName);
        bundle.putBoolean("isOrganic", isOrganic);
        bundle.putStringArrayList("oliveVarieties", (ArrayList<String>) oliveVarieties);
        ArrayList<CustomLatLng> farmCoordinates = new ArrayList<>(farmList);
        // Using parcelable to convert coordinates, so they could be sent, used stackoverflow and google to solve an issue
        bundle.putParcelableArrayList("farmCoordinates", convertToParcelableList(farmCoordinates));
        editFarmFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.map, editFarmFragment);
        transaction.addToBackStack(null);
        transaction.commit();
        editFarm.setVisibility(View.GONE);
    }
    private void addFarmToFirestore(Farm farm) {
        farmsCollection.add(farm)
                .addOnSuccessListener(documentReference -> {
                    String farmId = documentReference.getId();
                    if (farmId != null){
                        documentReference.update("idOfDocument", farmId);
                        Toast.makeText(requireContext(), "Farm fields were added", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Farm fields weren't added", Toast.LENGTH_SHORT).show();
                });
    }
    // used when worker sprays field, to update info in firebase
    private void changeStatus(Farm farm){
        // getting document id(where farm info is stored)
        String farmId = farm.getidOfDocument();
        // updating info in Firestore
        farmsCollection.document(farmId)
                .update("sprayed", farm.isSprayed(), "workerId", farm.getWorkerId());
    }
    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        // adding marker to farm list(from where all coordinates will be sent to firebase)
        // Using customLatLng because LatLng doesn't have empty constructor and firebase throws error
        CustomLatLng customLatLng = new CustomLatLng(latLng.latitude, latLng.longitude);
        farmList.add(customLatLng);
        // Updating map with marker where farmer clicked
        updateMapWithMarker(latLng);
        //   Toast.makeText(requireContext(), "You can", Toast.LENGTH_SHORT).show();
    }

    // Method for all farm display to all users(guests)
    private void displayFarmsToEveryone(){
        farmsCollection.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot farmsDocument :
                            Objects.requireNonNull(task.getResult())){
                        // converting the data from firebase to Farm class object
                        Farm farm = farmsDocument.toObject(Farm.class);
                        // displaying markers on map
                        displayFarmMarkers(farm);
                    }
                }
            }
        });
    }
    // for setting markers on map of farms that already exist
    private BitmapDescriptor setMarkerIcon(boolean isOrganic, boolean sprayed){
        if (sprayed){
            BitmapDescriptor WorkerSprayed = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET);
            return WorkerSprayed;
        }
        else if (isOrganic){
            BitmapDescriptor organic = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
            return organic;
        }else {
            BitmapDescriptor nonOrganic = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE);
            return nonOrganic;
        }
    }
    // sets markers on map with existing farms and their types
    private void displayFarmMarkers(Farm farm){
        List<CustomLatLng> coordinates = farm.getCoordinates();
        String farmName = farm.getFarmName();
        boolean organic = farm.isOrganic();
        boolean sprayed = farm.isSprayed();
        // getting olive varieties
        List<String> oliveList = farm.getOliveVarieties();
        // making list to string, so it could be displayed on marker as snippet
        String olive = String.join(", ", oliveList);

        for (CustomLatLng locations : coordinates){
            // converting to LatLng so google can display it
            LatLng farmLatLng = locations.toLatLng();
            googleMap1.addMarker(new MarkerOptions().position(farmLatLng).title(farmName).snippet(olive).icon(setMarkerIcon(organic, sprayed)));
        }
    }
    public void onStart() {
        super.onStart();
        auth.addAuthStateListener(authStateListener);
    }
    @Override
    public void onStop() {
        super.onStop();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }
    }
    // updating map with markers
    private void updateMapWithMarker(LatLng latLng) {
        // Markers object
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(farmName);

        // setting marker on map
        googleMap1.addMarker(markerOptions);
        farmLocation = latLng;
    }
    // setter to see if farmer comes from AddFarmFormFragment
    public void setFromForm(boolean FromForm) {
        this.fromForm = FromForm;
    }
    // Setter to get Farm name
    public void setFarmName(String farmName) {
        this.farmName = farmName;
    }
    // Setter to get type of farm (organic or not)
    public void setIsOrganic(boolean isOrganic) {
        this.isOrganic = isOrganic;
    }
    // setter to get varieties
    public void setOliveVarieties(List<String> oliveVarieties) {
        this.oliveVarieties = oliveVarieties;
    }
    // making sure that edit farm button once again visible
    @Override
    public void saveClicked() {
        if (editFarm != null){
            editFarm.setVisibility(View.VISIBLE);
        }
    }
    // for resetting map markers after edit or deletion of farms
    @Override
    public void resetMap() {
        googleMap1.clear();
        displayFarmsToEveryone();
    }
}