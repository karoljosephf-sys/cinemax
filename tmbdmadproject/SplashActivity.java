package com.example.tmbdmadproject;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser != null){
            currentUser.reload().addOnCompleteListener(task->{
                if(task.isSuccessful()){
                    startActivity(new Intent(SplashActivity.this, Container.class));
                }else{
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(this, MainActivity.class));
                }
            });

        }else{
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        }
        finish();
    }
}
