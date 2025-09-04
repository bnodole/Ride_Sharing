package com.example.ridesharing;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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


public class Login extends Fragment {



    EditText phone,password;
    Button login;

    public Login() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        phone = view.findViewById(R.id.etPhone);
        password = view.findViewById(R.id.etPassword);
        login = view.findViewById(R.id.btnLogin);

        login.setOnClickListener(v -> {
            String phoneStr = phone.getText().toString().trim();
            String logPassword = password.getText().toString().trim();

            if (phoneStr.isEmpty() || logPassword.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields!!!", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users");
            dbRef.child(phoneStr).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String dbPassword = snapshot.child("password").getValue(String.class);
                        String role = snapshot.child("role").getValue(String.class);

                        if (dbPassword != null && dbPassword.equals(logPassword)) {
                            Toast.makeText(getContext(), "Login Success: " + role, Toast.LENGTH_SHORT).show();
                            assert role != null;
                            if (role.equalsIgnoreCase("Client")) {
                                Intent intent = new Intent(getActivity(), DashboardClient.class);
                                intent.putExtra("phone", phoneStr); // Pass phone to dashboard
                                startActivity(intent);
                                assert getActivity() != null;
                                getActivity().finish();
                            } else if (role.equalsIgnoreCase("Driver")) {
                                Intent intent = new Intent(getActivity(), DashboardDriver.class);
                                intent.putExtra("phone", phoneStr); // Pass phone to dashboard
                                startActivity(intent);
                                assert getActivity() != null;
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
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
        return view;
    }
}