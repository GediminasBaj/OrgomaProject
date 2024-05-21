package com.learningkotlin.orgomaproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class Registration extends Fragment {
    // Firebase and google auth
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private GoogleSignInClient signInClient;
    private GoogleSignInOptions signInOptions;

    // Firebase email auth
    EditText worker_email_reg, worker_pas_reg;
    Button email_registerBtn;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    // Firebase Connection
    // accessing Firebase Firestore database, to obtain references
    private FirebaseFirestore accounts_db = FirebaseFirestore.getInstance();
    // Representing the document in firestore db, provides methods, (w,r,u,d)
    private DocumentReference registeredAccounts = accounts_db.collection("Users")
            .document("Workers");
    // Represent specific collection within fb fs, in this case we referring to Users collection
    private CollectionReference collectionReference = accounts_db.collection("Users");

    // Buttons, checkbox
    Button googleBtn;
    RadioGroup group;
    int typeId;
    // Worker type
    private String userType = "";

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public Registration() {
        // Required empty public constructor
    }
    public static Registration newInstance(String param1, String param2) {
        Registration fragment = new Registration();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_worker_registration, container, false);

        // User type radio group
        group = view.findViewById(R.id.registrationGroup);

        // Google signIn
        googleBtn = view.findViewById(R.id.register_with_google);

        googleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting radio group element id
                typeId = group.getCheckedRadioButtonId();
                // First check if worker type chosen
                if(typeId == R.id.worker){
                    userType = "Sprayer";
                    signIn.launch(signInClient.getSignInIntent());
                }else if(typeId == R.id.farmer){
                    userType = "Farmer";
                    signIn.launch(signInClient.getSignInIntent());
                }else {
                    Toast.makeText(requireContext(),
                            "You must chose user type",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build();
        signInClient = GoogleSignIn.getClient(requireActivity(), signInOptions);
        // Google signIn ends here

        // Firebase Registration
        worker_email_reg = view.findViewById(R.id.Worker_Enter_Email);
        worker_pas_reg = view.findViewById(R.id.Worker_Enter_Password);
        // Firebase Auth
        auth = FirebaseAuth.getInstance();
        email_registerBtn = view.findViewById(R.id.Register_with_Email);
        // Obtaining current user info

        email_registerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting radio group element id
                typeId = group.getCheckedRadioButtonId();

                // First check if worker type chosen
                if (typeId == R.id.worker){
                    userType = "Sprayer";
                    register();
                } else if (typeId == R.id.farmer) {
                    userType = "Farmer";
                    register();
                } else {
                    Toast.makeText(requireContext(),
                            "You must chose user type",
                            Toast.LENGTH_SHORT).show();
                }

            }
                // Encapsulated my logic, so I can call it when needed
                // to avoid unexpected, unneeded account creation
                private void register(){
                    // making sure that all fields not empty
                    if (!TextUtils.isEmpty(worker_email_reg.getText().toString())
                            && !TextUtils.isEmpty(worker_pas_reg.getText().toString())){
                        // getting info from edit text
                        String email = worker_email_reg.getText().toString().trim();
                        String pass = worker_pas_reg.getText().toString().trim();

                        RegisterWorkerWithEmail(email, pass);
                    }else {
                        Toast.makeText(requireContext(),
                                "You Must Fill in All Fields!",
                                Toast.LENGTH_SHORT).show();
                    }
                }

        });

        return view;
    }
    // Registration with email
    private void RegisterWorkerWithEmail(String email, String pass){
       // checking if all fields are filled, so no empty account enters db
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)){
            auth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    // check if insertion in db was success
                    if(task.isSuccessful()){
                        // The worker account is created
                        Toast.makeText(
                                requireContext(),
                                "Account creation is success",
                                Toast.LENGTH_SHORT).show();

                        // Worker object with email, userId, and type
                        // getting objects - that setting that it can't be null(empty),
                        // then from currentUser(FirebaseUser object) we get it's user id with prebuilt getUid method
                        String workerId = Objects.requireNonNull(auth.getCurrentUser().getUid());
                        String type = userType;

                        User user = new User(true,email, workerId, type, 0 ,0);

                        // Saving object in Firebase Firestore
                        collectionReference.document(workerId).set(user).addOnSuccessListener(
                                new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        IfRegistrationSuccessMoveToWorkerLoggedIn();
                                    }
                                }
                        );
                    }
                }
            });
        }
    }
    // Tried to use another method for google sign in, because on activity result is deprecated
    // regIn -> lambda expression that represents callback, when there is available result
    // contains info about the started activity result
    private final ActivityResultLauncher<Intent> signIn = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), regIn -> {
                        int code = regIn.getResultCode();
                        Intent data = regIn.getData();
                        signInResult(code, data);
    });

    // if registration success move to workerLoggedIn Activity
    private void IfRegistrationSuccessMoveToWorkerLoggedIn() {
        currentUser = auth.getCurrentUser();

        if (currentUser != null){
            // Getting values
            String email = currentUser.getEmail();
            String workerId = currentUser.getUid();
            String type = userType;
            // Creating worker object with the values
            User user = new User(true,email, workerId, type, 0, 0);

            collectionReference.document(workerId).set(user).addOnSuccessListener(
                    new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Intent intent = new Intent(requireContext(), WorkerLoggedIn.class);
                            startActivity(intent);
                        }
                    }
            );
        }
    }

    private void signInResult(int code, Intent data){
        // checking result
        if (code == android.app.Activity.RESULT_OK){
            // getting signed-in account from result intent
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // if user signed in, trying to retrieve account, and if successful display Toast, else another toast
                GoogleSignInAccount signInAccount = task.getResult(ApiException.class);
                // Getting google auth Credentials
                AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
                // Signing with the google credentials
                auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(
                                    requireContext(),
                                    "Your registration was success",
                                    Toast.LENGTH_LONG).show();
                            IfRegistrationSuccessMoveToWorkerLoggedIn();
                        }
                    }
                });
            } catch (ApiException e) {
                Toast.makeText(
                        requireContext(),
                        "Something went wrong: " + e.getStatusCode(),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // deprecated sign in method onActivityForResult

    // signIn
    //    private void signIn(){
    //        Intent signInIntent = signInClient.getSignInIntent();
    //        startActivityForResult(signInIntent, 1000);
    //    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        // checking result
//        if (requestCode == 1000){
//            // getting singed-in account from result intent
//            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
//            try {
//                // if user signed in, trying to retrieve account, and if successful display Toast, else another toast
//                GoogleSignInAccount signInAccount = task.getResult(ApiException.class);
//                // Getting google auth Credentials
//                AuthCredential authCredential = GoogleAuthProvider.getCredential(signInAccount.getIdToken(), null);
//                // Signing with the google credentials
//                auth.signInWithCredential(authCredential).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
//                    @Override
//                    public void onComplete(@NonNull Task<AuthResult> task) {
//                        if (task.isSuccessful()){
//                            Toast.makeText(
//                                    requireContext(),
//                                    "Your registration was success",
//                                    Toast.LENGTH_LONG).show();
//                            IfRegistrationSuccessMoveToWorkerLoggedIn();
//                        }
//                    }
//                });
//            } catch (ApiException e) {
//                Toast.makeText(
//                        requireContext(),
//                        "Something went wrong: " + e.getStatusCode(),
//                        Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
}