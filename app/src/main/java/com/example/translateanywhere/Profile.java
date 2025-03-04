package com.example.translateanywhere;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.auth.User;

import java.util.HashMap;
import java.util.Map;

public class Profile extends AppCompatActivity {
    Button save;
    EditText name,age,mobile,dob,Email;
    FirebaseFirestore db;
    String Name,Age,Mobile,dateofbirth,Gmail;
    ProgressDialog progressDialog;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        db = FirebaseFirestore.getInstance();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        save=findViewById(R.id.save);
        name=findViewById(R.id.name);
        age=findViewById(R.id.age);
        mobile=findViewById(R.id.Mobile);
        dob=findViewById(R.id.dob);
        Email=findViewById(R.id.mail);

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isValidInput(name)) {
                    name.setError("Name cannot be empty!");
                    return;
                }
                if (!isValidInput(age)) {
                    age.setError("Age cannot be empty!");
                    return;
                }
                if (!isValidInput(mobile)) {
                    mobile.setError("Mobile number cannot be empty!");
                    return;
                }
                if (!isValidInput(dob)) {
                    dob.setError("Date of Birth cannot be empty!");
                    return;
                }
                if(!isValidInput(Email)){
                    Email.setError("Email Cannot Be empty! or Invalid Email");
                }
                Name= String.valueOf(name.getText());
                Age= String.valueOf(age.getText());
               Mobile= String.valueOf(mobile.getText());
               dateofbirth= String.valueOf(dob.getText());
               Gmail= String.valueOf(Email.getText());
               checkUsersId(Mobile);
            }
        });
    }
    private void StoretoFireStore(String Name, String Age, String UserId, String DOB,String Email) {
        progressDialog=ProgressDialog.show(this,"Updating Profile","Please Be Patient");
        Map<String, Object> user = new HashMap<>();
        user.put("Name", Name);
        user.put("Age", Age);
        user.put("UserId", UserId);
        user.put("DOB",DOB);
        user.put("Email",Email);

        db.collection("users").document(UserId)
                .set(user)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressDialog.dismiss();
                        Log.d("Firestore", "User data saved successfully!");
                        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("UserId",UserId);
                        editor.apply();
                        Intent intent=new Intent(getApplicationContext(), MainActivity.class);
                        intent.putExtra("UserId",UserId);
                        startActivity(intent);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(Profile.this, "Error Please Try Again After Few Minutes", Toast.LENGTH_SHORT).show();
                        Log.d("Firestore", "Error saving data", e);
                    }
                });
    }
    private void checkUsersId(String UserId){
        if (UserId == null || UserId.isEmpty()) {
            Log.e("Firestore", "UserId is empty!");
            return;
        }
        db.collection("users").document(UserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.e("Firestore", "UserId already exists!");
                        Toast.makeText(Profile.this, "User ID already exists! Try another.", Toast.LENGTH_LONG).show();
                    } else {
                        StoretoFireStore(Name,Age,Mobile,dateofbirth,Gmail);
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error checking UserId", e));
    }
    private boolean isValidInput(EditText editText) {
        String input = editText.getText().toString().trim();
        return !input.isEmpty();
    }
}