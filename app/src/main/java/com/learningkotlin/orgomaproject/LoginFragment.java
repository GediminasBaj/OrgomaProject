package com.learningkotlin.orgomaproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
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
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class LoginFragment extends Fragment {
    // Buttons
    Button loginBtn, googleLoginBtn;
    // Google sign in
    GoogleSignInOptions googleSignInOptions;
    GoogleSignInClient googleSignInClient;
    // EditTexts for Login
    EditText email, passLog;
    // Firebase
    // Firebase Connection
    // accessing Firebase Firestore database, to obtain references
    private FirebaseFirestore accounts_db = FirebaseFirestore.getInstance();
    // Represent specific collection within fb fs, in this case we referring to Users collection
    private CollectionReference collectionReference = accounts_db.collection("Users");
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private String mParam1;
    private String mParam2;

    public LoginFragment() {
        // Required empty public constructor
    }

    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
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
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        loginBtn = view.findViewById(R.id.login_button);
        googleLoginBtn = view.findViewById(R.id.login_google);
        email = view.findViewById(R.id.login_email);

        passLog = view.findViewById(R.id.login_password);
        // Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginEmailPassUser(
                    email.getText().toString().trim(),
                    passLog.getText().toString().trim()
                );
            }
        });

        // Google sign in configuration
           googleSignInOptions = new GoogleSignInOptions
                   .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                   .requestIdToken(getString(R.string.client_id))
                   .requestEmail().build();

           googleSignInClient = GoogleSignIn.getClient(requireActivity(), googleSignInOptions);
        // Google button click listener
        googleLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                googleSignIn();
            }
        });

        return view;
    }

    private void loginEmailPassUser(String email, String pass){
        // Checking for empty edit texts
        if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(pass)){
            // if email and pass are not empty
            // checking if account exist
            firebaseAuth.signInWithEmailAndPassword(
                    email, pass
            ).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                @Override
                public void onSuccess(AuthResult authResult) {
                    // assigning the firebase user.
                    currentUser = firebaseAuth.getCurrentUser();
                    userLoggedIn(true);
                    Intent intent = new Intent(requireContext(), WorkerLoggedIn.class);
                    startActivity(intent);
                }
            });
        }
    }

    // Google signIn
    private static final int google_sign_in = 1002;
    private void googleSignIn(){
        Intent intent = googleSignInClient.getSignInIntent();
        startActivityForResult(intent, google_sign_in);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == google_sign_in){
            // getting singed-in account from result intent
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                // if user signed in, trying to retrieve account, and if successful display Toast, else another toast
                GoogleSignInAccount signInAccount = task.getResult(ApiException.class);
                // calling method to check if user registered
                checkFirebase(signInAccount.getIdToken());
            }catch (ApiException e){

            }
        }
    }

    private void userLoggedIn(boolean login){
        if(currentUser != null){
            // getting current user id from firebase
            String id = currentUser.getUid();
            // using that id access firebase firestore db;
            collectionReference.document(id).update("signedIn", login);
        }

    }

    private void checkFirebase(String id){
        AuthCredential credential = GoogleAuthProvider.getCredential(id, null);
        firebaseAuth.signInWithCredential(credential).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
            @Override
            public void onSuccess(AuthResult authResult) {
                // successfully signed in
                currentUser = firebaseAuth.getCurrentUser();
                // Getting email from firebase
                String email = currentUser.getEmail();
                // checking firebase with email if there is a match
                collectionReference.whereEqualTo("email", email).get().addOnSuccessListener(
                        new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        // if there is no such email registered throw an Toast
                        if (queryDocumentSnapshots.isEmpty()){
                            Toast.makeText(
                                    requireContext(),
                                    "First you need to register",
                                    Toast.LENGTH_SHORT).show();
                        }else {
                            // there is registered account, so direct to next activity
                            userLoggedIn(true);
                            Intent intent = new Intent(requireContext(), WorkerLoggedIn.class);
                            startActivity(intent);
                        }
                    }
                });
            }

        });
    }
}