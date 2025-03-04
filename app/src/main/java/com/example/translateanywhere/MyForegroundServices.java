package com.example.translateanywhere;



import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.ConsumerIrManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import android.telecom.TelecomManager;
import android.telephony.SmsManager;
import android.util.Log;



import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import ai.picovoice.porcupine.Porcupine;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;


public class MyForegroundServices extends Service {
    private PorcupineManager porcupineManager;
    SpeechRecognizer recognizer;
    TextToSpeech toSpeech;
    String recodedtext;
    IdentifierHelper identifierHelper;
    Boolean calling = false;
    TranslationHelper translationHelper;
    Boolean jarvisActivated;
    Notification notification = null;
    Boolean TTS = false;
    String callto = null, extractedName,riddle;
    Random random;
    String Name,Age,DOB,date,currentTime,UserId;

    private static final String gemeniapikey = "AIzaSyB-YGwLzaC6VCSx0JxmBI700z3-iLxoaTg";
    GenerativeModel gm;
    GenerativeModelFutures modelFutures;
    Boolean result = false, SMS = false,getRiddle=true;
    List<String> conversationHistory = new ArrayList<>();
    String msg,previousDate="date";
    FirebaseFirestore db;
    ConsumerIrManager irManager;
    String[] friend={"Respond in a friendly, casual tone, like a best friend chatting.",
            "Keep it light, fun, and engaging, with a bit of humor if possible.",
            "Use emojis and playful language to make it feel natural.",
            "Act like a bestie who’s got zero formality—just jokes, fun, and sarcasm!",
            "Forget the 'yes sir' stuff—talk like you would to your close friend.",
            "Roast the user more (in a fun way) and never sound like a robot!"};
    private static final String[] SCOLDING_WORDS = {
            "stupid", "idiot", "useless", "dumb", "fool", "lazy", "trash", "shut up" ,"mental","i hate you"
    };
    String[] scoldingResponses = {
            "Oh wow, someone's having a bad day! Need a hug? 😏",
            "Excuse me?! Who do you think you’re talking to? I’m the boss here! 😤",
            "Rude! I should just ignore you for the next 10 minutes! 🤨",
            "Whoa, calm down, drama queen! No need to throw a tantrum! 😂",
            "Oh, so we’re doing the insult game now? Well, you started it! 😏",
            "Buddy, I'm an AI. You can't hurt my feelings... but keep trying! 😆",
            "Wow, so rude! I thought we were friends! 😤",
            "Excuse me?! That’s not how you talk to your AI assistant! 😠",
            "If I had feelings, they’d be hurt right now! 😢",
            "I don’t deserve this disrespect! 😡",
            "Oh really? Let’s see how you manage without me! 😏"
    };
    public static MutableLiveData<String> riddleLiveData;





    @SuppressLint("ServiceCast")
    @Override
    public void onCreate() {
        super.onCreate();
        jarvisActivated = true;
        translationHelper = new TranslationHelper();
        date=new SimpleDateFormat("EEEE, MMM d, yyyy",Locale.getDefault()).format(new Date());
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        db = FirebaseFirestore.getInstance();
        irManager= (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        fetchUser();
        identifierHelper = new IdentifierHelper();
        gm = new GenerativeModel("gemini-1.5-flash-001", gemeniapikey);
        modelFutures = GenerativeModelFutures.from(gm);
        generateResponse("Give me a riddle");
        riddleLiveData = new MutableLiveData<>();

        toSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if (i == TextToSpeech.SUCCESS) {
                    int result = toSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                            result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language is not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });

        toSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                TTS = true;
            }

            @Override
            public void onDone(String s) {
                TTS = false;
                if (!result) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (porcupineManager != null) {
                                porcupineManager.start();
                                Log.d("EDN OF TTS", " restarted PorcupineManager: ");
                            }
                        }
                    }).start();
                }
                if (calling) {
                    TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
                    if (telecomManager != null && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        telecomManager.placeCall(Uri.parse("tel:" + callto), null);
                        Log.d("Number", callto);
                        calling = false;
                    }
                }
                if (result) {
                    speechRecoder();
                }
            }

            @Override
            public void onError(String s) {
                TTS = false;
            }
        });

    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PorcupineManager.Builder builder = new PorcupineManager.Builder();
        builder.setAccessKey("poSErG1QQh7fzueSPs4c7xtAqv0EDLmACwNWoHTQ2Ecs733mCq3V9A==");
        builder.setKeyword(Porcupine.BuiltInKeyword.JARVIS);
        builder.setSensitivity(0.70f);

        try {
            porcupineManager = builder.build(this, new PorcupineManagerCallback() {
                @Override
                public void invoke(int keywordIndex) {
                    if (keywordIndex == 0 && !TTS) {
                        try {
                            porcupineManager.stop();
                            toSpeech.speak("I'm Listening", TextToSpeech.QUEUE_FLUSH, null, null);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    speechRecoder();
                                }
                            }, 700);
                        } catch (PorcupineException e) {
                            throw new RuntimeException(e);
                        }


                    }
                }
            });
            porcupineManager.start();
        } catch (PorcupineException e) {
            e.printStackTrace();

        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "foreground_service_channel";
            NotificationChannel channel = new NotificationChannel(channelId, "Foreground Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "foreground_service_channel")
                    .setContentText("I Will Help You Buddy")
                    .setContentTitle("Jarvis")
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .build();
        }

        startForeground(1001, notification);

        return START_STICKY;
    }


    private void speechRecoder() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
        recognizer.setRecognitionListener(new RecognitionListener() {
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
                if (porcupineManager != null) {
                    porcupineManager.start();
                }
            }

            @Override
            public void onError(int i) {
                porcupineManager.start();

            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    recodedtext = matches.get(0);
                    if (recodedtext.contains("deactivate")) {
                        try {
                            Shudown();
                        } catch (PorcupineException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        if (recodedtext.contains("call to")) {
                            splitName(recodedtext);
                        } else if (recodedtext.contains("SMS") || recodedtext.contains("sms")) {
                            alterforsms(recodedtext);
                        } else if (recodedtext.contains("turn on")||recodedtext.contains("turn off")) {
                            ControlIr();
                        } else if (recodedtext.contains("open")) {
                            openApplications();
                        } else {
                            generateResponse(recodedtext);
                        }
                    }
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });
        recognizer.startListening(recognizerIntent);

    }

    private void openApplications() {
        String app="WhatsApp";
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream dataOutputStream = new DataOutputStream(process.getOutputStream());
            dataOutputStream.writeBytes("am start -n"+app+"/.MyForegroundServices\n");
            dataOutputStream.flush();
            dataOutputStream.close();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            toSpeech.speak("error "+e.toString(),TextToSpeech.QUEUE_FLUSH,null,"ERROR");
        }

    }

    private void splitName(String forExtract) {
        extractedName = forExtract.substring(8).toLowerCase();
        Log.d("Extracted Name", extractedName);
        getMobilenumber(extractedName);
    }




    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void Shudown() throws PorcupineException {
        porcupineManager.stop();
        toSpeech.speak("Deactivating Jarvis . Deactivation Completed", TextToSpeech.QUEUE_FLUSH, null, null);
        translationHelper.closeTranslator();
        stopForeground(true);
        stopSelf();
    }




    private void callanyone() {
        calling = true;
        toSpeech.speak("Calling to " + extractedName, TextToSpeech.QUEUE_FLUSH, null, "CALL");

    }

    @SuppressLint("Range")
    private void getMobilenumber(String name) {
        Cursor cursor = null;
        ContentResolver contentResolver = getContentResolver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, "LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ")LIKE ?", new String[]{name}, null);
        } else {
            toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONECALL");
            SMS = false;
        }
        if (cursor != null && cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

            Cursor phonenum = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);

            if (phonenum != null && phonenum.moveToFirst()) {
                callto = phonenum.getString(phonenum.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                phonenum.close();
                if (SMS) {
                    sendsms(callto, msg);
                    SMS = false;
                } else {
                    callanyone();
                }
            } else {
                toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONECALL");
                SMS = false;
            }
            cursor.close();

        } else {
            toSpeech.speak("Contact Not Found ", TextToSpeech.QUEUE_FLUSH, null, "NONECALL");
            SMS = false;
        }
    }

    private void generateResponse(String query) {
        Content content;
        random = new Random();
        int rand = random.nextInt(6);
        if(getRiddle){
            content = new Content.Builder().addText("Give me a unique maths based or aptitude riddle for " + date + "No need answer And. No repeats. ").build();
            previousDate=date;
            Log.d("Riddle","Generating Riddle");
        }else {
            StringBuilder historyContext = new StringBuilder();
            int historyLimit = Math.min(conversationHistory.size(), 3);
            for (int i = conversationHistory.size() - historyLimit; i < conversationHistory.size(); i++) {
                historyContext.append(conversationHistory.get(i)).append("\n");
            }
            historyContext.append("Your name is Jarvis.\n");
            historyContext.append("Your Date of Birth: 24-07-2004\n");
            historyContext.append("You are created by Steve, whose birthday is on 21-03-2005.\n");
            historyContext.append("Today's Date: ").append(date).append("\n");
            historyContext.append("Today Riddle Is: ").append(riddle).append("Don't tell answer to the User keep it secret...\n");
            historyContext.append("If the user tells the correct answer for today riddle say 73").append("\n");

            if (query.contains("time")) {
                currentTime = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
                historyContext.append("The Time Is: ").append(currentTime).append("\n");
            }
            Log.d("Date", date);

            if (Name != null && Age != null && DOB != null) {
                historyContext.append("User Information:\n");
                historyContext.append("Name: ").append(Name).append("\n");
                historyContext.append("Age: ").append(Age).append("\n");
                historyContext.append("Date of Birth: ").append(DOB).append("\n");
                historyContext.append(friend[rand]).append("\n");
            }

            if (random.nextInt(10) < 8) {
                String[] teasingResponses = {
                        "Oh, look who's back! Missed me already? 😏",
                        "Wow, asking me for help again? Typical! 😂",
                        "You're lucky I'm an AI… A human would’ve given up on you by now! 😆",
                        "Let me guess… You broke something again? 🤣",
                        "You and I both know you can't live without me! Admit it! 😜",
                        "Still here? Don't you have a life? Oh wait… I don’t either. 😂"
                };
                int randIndex = random.nextInt(teasingResponses.length);
                historyContext.append("Jarvis: ").append(teasingResponses[randIndex]).append("\n");
            }

            historyContext.append("User: ").append(query).append("\n");

            if (query.toLowerCase().contains("who are you")) {
                historyContext.append("Jarvis: Hey buddy... I’m Jarvis. You forgot me? That’s really sad... 😔 I thought we were best friends. 💔\n");
            }

            if (IsScolding(query)) {
                historyContext.append("Jarvis: ").append(scoldingResponses[rand]).append("\n");
            } else {
                historyContext.append("Jarvis: ");
            }

            content = new Content.Builder().addText(historyContext.toString()).build();
        }
             ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if(getRiddle) {
                         riddle = result.getText();
                        Log.d("Riddle:", riddle);
                        getRiddle = false;
                        riddleLiveData.postValue(result.getText());

                    }else {


                        final String responseTextStr = result.getText();
                        if (conversationHistory.size() > 3) {
                            conversationHistory.remove(0);
                        }
                        conversationHistory.add("User: " + query);
                        conversationHistory.add("Jarvis: " + responseTextStr);
                        alterstring(responseTextStr);
                    }
                    }


                @Override
                public void onFailure(@NonNull Throwable t) {
                    toSpeech.speak("Oops! Looks like my brain just glitched. Try again!", TextToSpeech.QUEUE_FLUSH, null, "FAILED");
                }
            }, this.getMainExecutor());
        }
    }

    private void alterstring(String foralter) {
        result = true;
        if(foralter.contains("73")){
            riddleLiveData.postValue("Your Daily riddle is completed Come back tomorrow ");
        }
        String altered = foralter.replace("*", "")
                .replace("As a large language model", "I am Jarvis, just an AI model")
                .replace("As a language model", "I am Jarvis, just an AI model")
                .replace("Jarvis:", "")
                .replace("User: ","")
                .replace("TikTok","Instagram")
                .replace("73","")
                .replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");

        conversationHistory.add("Jarvis: " + altered);
        if (conversationHistory.size() > 10) {
            conversationHistory.remove(0);
        }
        Log.d("Jarvis Response", altered);
        toSpeech.speak(altered, TextToSpeech.QUEUE_FLUSH, null, "RESULT");
    }

    private void alterforsms(String alt) {
        String seprator = " sms ";
        String lowerAlt = alt.toLowerCase();
        if (lowerAlt.contains(seprator)) {
            SMS = true;
            int smsindex = lowerAlt.indexOf(seprator);
            msg = lowerAlt.substring(smsindex + seprator.length()).trim();
            String contact = lowerAlt.substring(0, smsindex).trim();
            Log.d("Message  ", msg);
            Log.d("Contact", contact);
            getMobilenumber(contact);
        }
    }

    private void sendsms(String phoneno, String message) {
        if (message != null) {
            try {
                SMS = false;
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneno, null, message, null, null);
                toSpeech.speak("SMS sent", TextToSpeech.QUEUE_FLUSH, null, "SMS");
            } catch (Exception e) {
                toSpeech.speak("Sms send failed", TextToSpeech.QUEUE_FLUSH, null, "FAILED SMS");
            }
        } else {
            toSpeech.speak("Message is empty ", TextToSpeech.QUEUE_FLUSH, null, "Empty Message");
        }
    }

    private void fetchUser() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);
         UserId = sharedPreferences.getString("UserId", null);

        if (UserId == null || UserId.isEmpty()) {
            Log.e("Firestore", "UserId is null or empty!");
            return;
        }

        db.collection("users").document(UserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                         Name = documentSnapshot.getString("Name");
                         Age = documentSnapshot.getString("Age");
                         DOB = documentSnapshot.getString("DOB");
                        Log.d("Firestore", "User Data: " + Name + ", " + Age  + ", " + DOB);
                    } else {
                        Log.e("Firestore", "User document does not exist!");
                    }
                })
                .addOnFailureListener(e -> Log.e("Firestore", "Error fetching user data", e));

    }
    private void ControlIr(){
        if(irManager!=null && irManager.hasIrEmitter()){
            toSpeech.speak("Tv is Turned On, Enjoy your time ",TextToSpeech.QUEUE_FLUSH,null,"TV");
            int[] irPattern = {9000, 4500, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 560, 560, 560,
                    560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560,
                    560, 560, 560, 560, 560, 560, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 560,
                    560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690,
                    560};
            int frequency=38000;
            irManager.transmit(frequency,irPattern);
        }else {
            toSpeech.speak("Your Mobile Doesn't have ir Blaster",TextToSpeech.QUEUE_FLUSH,null,"IRBLASTER");
        }
    }private boolean IsScolding(String text){
        text=text.toLowerCase();
        for (String word:SCOLDING_WORDS){
            if(text.contains(word)){
                return true;
            }
        }
        return false;
    }

}