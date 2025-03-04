package com.example.translateanywhere;


import android.app.ProgressDialog;
import android.content.Context;
import android.view.View;
import android.widget.Toast;

import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

public class TranslationHelper {
    private Translator translator;
    private ProgressDialog progressDialog;

    public interface TranslationCallback {
        void onTranslationSuccess(String translatedText);
        void onTranslationFailure(Exception e);
    }
    public void downloadModel(Context context, String sourceLanguage, String targetLanguage, TranslationCallback callback) {
        if (sourceLanguage != null && targetLanguage != null) {
            TranslatorOptions options = new TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage("ta")
                    .build();

            translator = Translation.getClient(options);



            translator.downloadModelIfNeeded()
                    .addOnSuccessListener(unused -> {

                        if (callback != null) {
                            callback.onTranslationSuccess(null);
                        }
                    })
                    .addOnFailureListener(e -> {


                        if (callback != null) {
                            callback.onTranslationFailure(e);
                        }
                    });
        }else {
            Toast.makeText(context, "Empty"+sourceLanguage+targetLanguage, Toast.LENGTH_SHORT).show();
        }
    }

    public void translateText(Context context, String textToTranslate, TranslationCallback callback) {



        translator.translate(textToTranslate)
                .addOnSuccessListener(translatedText -> {

                    if (callback != null) {
                        callback.onTranslationSuccess(translatedText);
                    }
                })
                .addOnFailureListener(e -> {

                    if (callback != null) {
                        callback.onTranslationFailure(e);
                    }
                });
    }

    public void closeTranslator() {
        if (translator != null) {
            translator.close();
            translator = null;
        }
    }
}

