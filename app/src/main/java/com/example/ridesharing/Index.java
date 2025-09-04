package com.example.ridesharing;

import android.content.Context;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.Objects;

public class Index extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Context ctx = getApplicationContext();
        setContentView(R.layout.activity_index);
        Objects.requireNonNull(getSupportActionBar()).hide();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button login,register;

        login = findViewById(R.id.login);
        register = findViewById(R.id.register);

        login.setOnClickListener(view -> {
            Fragment fragment = new Login();
            loadFragment(fragment);
        });

        register.setOnClickListener(view -> {
            Fragment fragment = new Register();
            loadFragment(fragment);
        });

    }

    public void loadFragment(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction trac = fm.beginTransaction();
        trac.replace(R.id.fragmentContainerView,fragment);
        trac.commit();
    }
}