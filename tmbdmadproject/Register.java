package com.example.tmbdmadproject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class Register extends AppCompatActivity {
    EditText authEmail, authPassword, authConfirmPass, authName;
    Button authRegister;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    TextView signin;

    Boolean alreadyRegistered = false;
    public static class User {
        public String email;
        public String uid;
        public String name;

        public User() {}

        public User(String email, String uid, String name) {
            this.email = email;
            this.uid = uid;
            this.name = name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        View rootView = findViewById(R.id.scrollMain);

        ViewCompat.setWindowInsetsAnimationCallback(rootView,
                new WindowInsetsAnimationCompat.Callback(
                        WindowInsetsAnimationCompat.Callback.DISPATCH_MODE_STOP) {

                    @Override
                    public void onPrepare(@NonNull WindowInsetsAnimationCompat animation) {
                    }

                    @NonNull
                    @Override
                    public WindowInsetsAnimationCompat.BoundsCompat onStart(
                            @NonNull WindowInsetsAnimationCompat animation,
                            @NonNull WindowInsetsAnimationCompat.BoundsCompat bounds) {
                        return bounds;
                    }

                    @NonNull
                    @Override
                    public WindowInsetsCompat onProgress(  // ✅ return type added
                                                           @NonNull WindowInsetsCompat insets,
                                                           @NonNull List<WindowInsetsAnimationCompat> runningAnimations) {
                        int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                        int navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                        rootView.setPadding(0, 0, 0, Math.max(imeBottom, navBottom));
                        return insets; // ✅ return insets
                    }
                });

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            int imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(0, 0, 0, Math.max(imeBottom, navBottom));
            return insets;
        });
        initialize();
    }

    private void initialize() {
        authEmail = findViewById(R.id.authEmail);
        authPassword = findViewById(R.id.authPass);
        authRegister = findViewById(R.id.authRegister);
        authConfirmPass = findViewById(R.id.authConfirmPass);
        authName = findViewById(R.id.authName);
        signin = findViewById(R.id.signin);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance("https://tmbd-mad-project-default-rtdb.asia-southeast1.firebasedatabase.app/");

        signin.setOnClickListener((v) -> {
            startActivity(new Intent(Register.this, MainActivity.class));
        });

        authRegister.setOnClickListener((v) -> {
            String name = authName.getText().toString().trim();
            String email = authEmail.getText().toString().trim();
            String password = authPassword.getText().toString().trim();
            String confirmPassword = authConfirmPass.getText().toString().trim();

            if (email.isEmpty() && password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!confirmPassword.equals(password)) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            if(alreadyRegistered){
                return;
            }

            alreadyRegistered = true;
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    logTask(task);
                    if (task.isSuccessful()) {

                        String uid = task.getResult().getUser().getUid();
                        String userEmail = task.getResult().getUser().getEmail();

                        User user = new User(userEmail, uid, name);

                        firebaseDatabase
                        .getReference("users")
                        .child(uid)
                        .setValue(user)
                        .addOnCompleteListener(dbTask->{

                            if(dbTask.isSuccessful()){
                                Toast.makeText(Register.this, "Account Created!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(Register.this, MainActivity.class));
                                finish();
                            }else{
                                Toast.makeText(Register.this, "DB Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                alreadyRegistered = false;
                            }

                        });


                    }else{
                        Toast.makeText(Register.this, "Login Failed: "+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        alreadyRegistered = false;
                    }
                }
            });
        });
    }
    private void logTask(Task<AuthResult> task) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n====== TASK RESULT ======");
        sb.append("\nisSuccessful : ").append(task.isSuccessful());
        sb.append("\nisComplete   : ").append(task.isComplete());
        sb.append("\nisCanceled   : ").append(task.isCanceled());

        if (task.isSuccessful() && task.getResult() != null) {
            AuthResult result = task.getResult();
            sb.append("\n--- User Info ---");
            sb.append("\nUID          : ").append(result.getUser().getUid());
            sb.append("\nEmail        : ").append(result.getUser().getEmail());
        } else if (task.getException() != null) {
            sb.append("\n--- Error Info ---");
            sb.append("\nClass        : ").append(task.getException().getClass().getName());
            sb.append("\nMessage      : ").append(task.getException().getMessage());
        }

        sb.append("\n=========================");
        Log.d("TASK_DUMP", sb.toString());
    }
}