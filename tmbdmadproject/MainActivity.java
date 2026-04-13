package com.example.tmbdmadproject;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsAnimationCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    EditText authEmail, authPassword;
    Button authLogin;
    FirebaseAuth firebaseAuth;
    TextView register;
    ImageButton btnTogglePass;
    boolean isVisible = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

    private void initialize(){
        authEmail = findViewById(R.id.authEmail);
        authPassword = findViewById(R.id.authPass);
        authLogin = findViewById(R.id.authLogin);
        register = findViewById(R.id.register);
        firebaseAuth = FirebaseAuth.getInstance();
        btnTogglePass = findViewById(R.id.btnTogglePass);

        btnTogglePass.setOnClickListener((v -> {
            if(isVisible){
                authPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
                );
                isVisible=false;
                btnTogglePass.setImageResource(R.drawable.view_12199468);
            }else{
                authPassword.setInputType(
                        InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                );
                isVisible=true;
                btnTogglePass.setImageResource(R.drawable.hide_12682128);
            }

            authPassword.setTypeface(
                    ResourcesCompat.getFont(this, R.font.poppins_medium)
            );
        }));

        authLogin.setOnClickListener((v)->{
            String email = authEmail.getText().toString().trim();
            String password = authPassword.getText().toString().trim();


            if(!email.isEmpty() && !password.isEmpty()){
                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        logTask(task);
                        if(task.isSuccessful()){
                            Toast.makeText(MainActivity.this, "Login Successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, Container.class));
                            finish();
                        }else{
                            Toast.makeText(MainActivity.this, "Login Failed: "+task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }else{

            }
        });

        register.setOnClickListener((v)->{
            startActivity(new Intent(MainActivity.this, Register.class));
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