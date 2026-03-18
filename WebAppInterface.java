package com.isl.audioautocut;

import android.webkit.JavascriptInterface;
import android.util.Log;

public class WebAppInterface {
    private MainActivity activity;
    private static final String TAG = "WebAppInterface";
    
    public WebAppInterface(MainActivity activity) {
        this.activity = activity;
        Log.d(TAG, "WebAppInterface criado");
    }
    
    @JavascriptInterface
    public void receiveConfig(String configJson) {
        Log.d(TAG, "receiveConfig: " + configJson);
        activity.handleWebCommand("refresh");
    }
    
    @JavascriptInterface
    public void receiveCommand(String command) {
        Log.d(TAG, "receiveCommand: " + command);
        activity.handleWebCommand(command);
    }
    
    @JavascriptInterface
    public String getConfig() {
        String config = activity.getCurrentConfig();
        Log.d(TAG, "getConfig: " + (config != null ? "config existe" : "config null"));
        return config;
    }
    
    @JavascriptInterface
    public void showToast(String message) {
        Log.d(TAG, "showToast: " + message);
    }
}
