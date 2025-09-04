package com.example.ridesharing;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Login#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Login extends Fragment {


    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";


    private String mParam1;
    private String mParam2;

    EditText phone,password;
    Button login;

    public Login() {
        // Required empty public constructor
    }

    public static Login newInstance(String param1, String param2) {
        Login fragment = new Login();
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
        phone = view.findViewById(R.id.etPhone);
        password = view.findViewById(R.id.etPassword);
        login = view.findViewById(R.id.btnLogin);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneStr = phone.getText().toString().trim();
                String logPassword = password.getText().toString().trim();

                if (phoneStr.isEmpty() || logPassword.isEmpty()) {
                    Toast.makeText(getContext(), "Please fill all fields!!!", Toast.LENGTH_SHORT).show();
                    return;
                }

                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");
                dbRef.child(phoneStr).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String dbPassword = snapshot.child("password").getValue(String.class);
                            String role = snapshot.child("role").getValue(String.class);

                            if (dbPassword != null && dbPassword.equals(logPassword)) {
                                Toast.makeText(getContext(), "Login Success: " + role, Toast.LENGTH_SHORT).show();
                                if (role.equalsIgnoreCase("Client")) {
                                    Intent intent = new Intent(getActivity(), DashboardClient.class);
                                    intent.putExtra("phone", phoneStr); // Pass phone to dashboard
                                    startActivity(intent);
                                    getActivity().finish();
                                } else if (role.equalsIgnoreCase("Driver")) {
                                    Intent intent = new Intent(getActivity(), DashboardDriver.class);
                                    intent.putExtra("phone", phoneStr); // Pass phone to dashboard
                                    startActivity(intent);
                                    getActivity().finish();
                                }

                            } else {
                                Toast.makeText(getContext(), "Incorrect password!", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "User not found!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        return view;
    }
}