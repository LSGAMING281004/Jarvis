package com.example.translateanywhere;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

public class IdentifierHelper {
    LanguageIdentifier languageIdentifier;



    public  interface Identifier{
        void onSuccesListener(String language);
        void onFailiuareListener(Exception e);
    }
    public void identifyLanguage(Context context,String text,Identifier identifier){
        if(text!=null) {
            languageIdentifier = LanguageIdentification.getClient();
            languageIdentifier.identifyLanguage(text).addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String s) {
                    if (s != null && identifier != null) {
                        identifier.onSuccesListener(s);
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    if (identifier != null) {
                        identifier.onFailiuareListener(e);
                    }
                }
            });
        }else {
            Toast.makeText(context, "Is Empty", Toast.LENGTH_SHORT).show();
        }
    }

}
