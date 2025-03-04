package com.example.translateanywhere;



import android.Manifest;
import android.annotation.SuppressLint;

import android.app.ActivityManager;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.hardware.ConsumerIrManager;

import android.os.Build;
import android.os.Bundle;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.Observer;

import static com.example.translateanywhere.R.menu.menu_main;

import android.speech.tts.UtteranceProgressListener;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;



import com.google.firebase.FirebaseApp;


import java.util.ArrayList;
import java.util.Locale;



public class MainActivity extends AppCompatActivity {
    Intent intent,intent1;
    TextToSpeech toSpeech;
    TextView textView,Riddle,txttask;
    Button speakbtn;

    private boolean isTextToSpeechInitialized = false;
    IdentifierHelper helper;
    Toolbar toolbar1;
    TranslationHelper translationHelper;
    String identifiedLanguage,targetlanguage="ta",selectedtexts,recodedtext,step;
    EditText editText;
    SpeechRecognizer speechRecognizer;
    Boolean wakeup=false;
    ConsumerIrManager irManager;
    ConstraintLayout constraintLayout;
    ScrollView scrollView;






    @SuppressLint({"MissingInflatedId", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        irManager= (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        FirebaseApp.initializeApp(this);
        chechProfile();
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
        String UserId = sharedPreferences.getString("UserId", null);
        if(UserId==null){
            updateprofile();
        }
        requestAudioPermission();
        smspermission();
        helper=new IdentifierHelper();
        translationHelper=new TranslationHelper();
        speechRecognizer =SpeechRecognizer.createSpeechRecognizer(this);
        toSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = toSpeech.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "TTS language not supported", Toast.LENGTH_SHORT).show();
                } else {
                    isTextToSpeechInitialized = true;
                    handleIncomingIntent();
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_SHORT).show();
            }
        });
        toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {

            }

            @Override
            public void onDone(String s) {
                intent1=new Intent(getApplicationContext(),MyForegroundServices.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && wakeup) {
                    startForegroundService(intent1);
                }
            }

            @Override
            public void onError(String s) {

            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editText=findViewById(R.id.edittext);
        speakbtn=findViewById(R.id.btnSpeak);
        toolbar1=findViewById(R.id.my_toolbar);
        textView=findViewById(R.id.textview);
        setSupportActionBar(toolbar1);
        Riddle=findViewById(R.id.txtRiddle1);
        scrollView=findViewById(R.id.scrollView2);
        constraintLayout=findViewById(R.id.Task);
        txttask=findViewById(R.id.txttask);


        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        final Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");


        speakbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isServiceisRunning(MyForegroundServices.class)){
                    Toast.makeText(MainActivity.this, "Deactivate Jarvis And access speak", Toast.LENGTH_SHORT).show();
                }else {
                    speechRecoder();
                }
            }
       });
        Riddle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isServiceisRunning(MyForegroundServices.class)) {
                    MyForegroundServices.riddleLiveData.observe(MainActivity.this, new Observer<String>() {
                        @Override
                        public void onChanged(String riddle) {
                            if(riddle.contains("completed")){
                               Riddle.setText("Today Over");
                            }else{
                                Riddle.setText(riddle);
                                Riddle.setEnabled(false);
                                toSpeech.speak("Your Riddle Is "+riddle,TextToSpeech.QUEUE_FLUSH,null,null);
                            }

                        }
                    });
                }else {
                    Toast.makeText(MainActivity.this, "Activate Jarvis First ", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void chechProfile() {
        SharedPreferences preferences=getSharedPreferences("MyAppPrefs",MODE_PRIVATE);
        boolean isProfileUpdated=preferences.getBoolean("isProfileUpdated",false);

        if(!isProfileUpdated){
            updateprofile();
            SharedPreferences.Editor editor=preferences.edit();
            editor.putBoolean("isProfileUpdated",true);
            editor.apply();
        }
    }

    private void updateprofile() {
        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setTitle("Update Your Profile")
                .setMessage("Update your profile for jarvis to know who are you!")
                .setPositiveButton("Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent2=new Intent(getApplicationContext(), Profile.class);
                        startActivity(intent2);
                    }
                });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();
    }

    private void smspermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, 1);
        }
    }

    private void handleIncomingIntent() {
        intent = getIntent();
        if (Intent.ACTION_PROCESS_TEXT.equals(intent.getAction()) && intent.getType() != null) {
            CharSequence selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT);
            if (selectedText != null) {
                txttask.setVisibility(View.GONE);
                constraintLayout.setVisibility(View.GONE);
                scrollView.setVisibility(View.VISIBLE);
                Identify(selectedText.toString());
            }
        }
    }

    private void Identify(String string) {
        selectedtexts=string;
        if(selectedtexts!=null && isTextToSpeechInitialized) {
            helper.identifyLanguage(MainActivity.this, string, new IdentifierHelper.Identifier() {
                @Override
                public void onSuccesListener(String language) {
                    identifiedLanguage = language;
                    translate();
                }

                @Override
                public void onFailiuareListener(Exception e) {
                    Toast.makeText(MainActivity.this, "Identification Failed", Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Toast.makeText(this, "No text Detected", Toast.LENGTH_SHORT).show();
        }
    }
    private void translate(){
        String target=targetlanguage;
        translationHelper.downloadModel(MainActivity.this, identifiedLanguage, target, new TranslationHelper.TranslationCallback() {
            @Override
            public void onTranslationSuccess(String translatedText) {
                translationHelper.translateText(MainActivity.this, selectedtexts, new TranslationHelper.TranslationCallback() {
                    @Override
                    public void onTranslationSuccess(String translatedText) {
                        textView.setText(translatedText);
                        toSpeech.speak(translatedText,TextToSpeech.QUEUE_FLUSH,null,null);
                    }

                    @Override
                    public void onTranslationFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "Translation Failed", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onTranslationFailure(Exception e) {
                Toast.makeText(MainActivity.this, "Language Identification Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
    public boolean onCreatePanelMenu(int featureId, @NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(menu_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.japan) {
            targetlanguage = "ja";

            Toast.makeText(this, "Language Selected to Japanese", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.korean) {
            targetlanguage = "ko";

            Toast.makeText(this, "Language Selected to Korean", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.indo) {
            targetlanguage = "ch";
            
            Toast.makeText(this, "Language Selected to Indonesia", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.eng) {
            targetlanguage = "en";

            Toast.makeText(this, "Language Selected to English", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.tam) {
            targetlanguage = "ta";

            Toast.makeText(this, "Language Selected to Tamil", Toast.LENGTH_SHORT).show();

        }if(id==R.id.WakeJarvis){
            wakeup=true;
            wakeupjarvis();
        }
        return true;
    }

    private void wakeupjarvis() {

        AlertDialog.Builder builder=new AlertDialog.Builder(this)
                .setTitle("Wake Up Jarvis!")
                .setMessage("Wake up jarvis is running in Background It take Battery consume Would you need to woke up jarvis")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(isServiceisRunning(MyForegroundServices.class)){
                            Toast.makeText(MainActivity.this, "Jarvis is already Activated", Toast.LENGTH_SHORT).show();
                        }else {
                            toSpeech.speak("Jarvis activated", TextToSpeech.QUEUE_FLUSH, null, "INTRODUCING");
                        }

                    }
                }).setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(MainActivity.this, "Jarvis Activation Cancelled", Toast.LENGTH_SHORT).show();
                    }
                });
        AlertDialog alertDialog=builder.create();
        alertDialog.show();

    }

    @SuppressLint("ObsoleteSdkInt")
    private void requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.RECORD_AUDIO},
                    1);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, 7);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_CONTACTS},77);
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new  String[]{Manifest.permission.READ_PHONE_STATE},777);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS}, 1);
            }
        }
    }
    private void speechRecoder(){
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

                Toast.makeText(MainActivity.this, "Listening finished", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(int i) {
                Toast.makeText(MainActivity.this, "Error"+i , Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    recodedtext=matches.get(0);
                    editText.setText(recodedtext);
                    Identify(recodedtext);
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        speechRecognizer.startListening(recognizerIntent);

    }
    protected void onStart() {
        super.onStart();


    }
    private boolean isServiceisRunning(Class<?> serviceClass){
        ActivityManager manager= (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if(manager!=null){
            for(ActivityManager.RunningServiceInfo service1:manager.getRunningServices(Integer.MAX_VALUE)){
                if(serviceClass.getName().equals(service1.service.getClassName())){
                    return true;
                }
            }

        }
        return false;
    }
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 7) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Premission granted", Toast.LENGTH_SHORT).show();
            }
        }
    }


}
