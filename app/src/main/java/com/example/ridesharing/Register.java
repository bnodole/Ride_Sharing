package com.example.ridesharing;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends Fragment {

    EditText name, phone, password, repassword;
    RadioGroup role;
    Button register;

    // Firebase DB reference
    DatabaseReference dbRef;

    public Register() {}

    public static Register newInstance(String param1, String param2) {
        Register fragment = new Register();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbRef = FirebaseDatabase.getInstance().getReference("users");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        register = view.findViewById(R.id.btnRegister);
        name = view.findViewById(R.id.etName);
        phone = view.findViewById(R.id.etPhone);
        password = view.findViewById(R.id.etPassword);
        repassword = view.findViewById(R.id.etConfirmPassword);
        role = view.findViewById(R.id.radioGroupRole);

        register.setOnClickListener(v -> {
            String regName = name.getText().toString().trim();
            String phoneStr = phone.getText().toString().trim();
            String regPassword = password.getText().toString().trim();
            String reRegPassword = repassword.getText().toString().trim();

            if (regName.isEmpty() || phoneStr.isEmpty() || regPassword.isEmpty() || reRegPassword.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!regPassword.equals(reRegPassword)) {
                Toast.makeText(getContext(), "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedRole = role.getCheckedRadioButtonId();
            if (selectedRole == -1) {
                Toast.makeText(getContext(), "Please select Role", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = view.findViewById(selectedRole);
            String regRole = selectedRadio.getText().toString();

            // Check if phone number is valid
            if (!phoneStr.matches("\\d{7,15}")) {
                Toast.makeText(getContext(), "Invalid phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save user to Firebase
            User user = new User(regName, phoneStr, regPassword, regRole);

            dbRef.child(phoneStr).setValue(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Registration Successful!", Toast.LENGTH_SHORT).show();
                        name.setText("");
                        phone.setText("");
                        password.setText("");
                        repassword.setText("");
                        role.clearCheck();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });

        return view;
    }
}
