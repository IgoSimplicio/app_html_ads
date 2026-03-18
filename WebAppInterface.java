package com.isl.audioautocut;

import android.webkit.JavascriptInterface;
import android.util.Log;

public class WebAppInterface {
    private MainActivity activity;
    private static final String TAG = "WebAppInterface";
    
    public WebAppInterface(MainActivity activity) {
        this.activity = activity;
    }
    
    @JavascriptInterface
    public void receiveConfig(String configJson) {
        Log.d(TAG, "Recebendo configuração: " + configJson);
        // Atualizar configuração no app nativo
        activity.handleWebCommand("refresh");
    }
    
    @JavascriptInterface
    public void receiveCommand(String command) {
        Log.d(TAG, "Comando recebido: " + command);
        activity.handleWebCommand(command);
    }
    
    @JavascriptInterface
    public String getConfig() {
        return activity.getCurrentConfig();
    }
    
    @JavascriptInterface
    public void showToast(String message) {
        // Para debug
        Log.d(TAG, "Toast: " + message);
    }
}