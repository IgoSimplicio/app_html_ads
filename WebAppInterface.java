package com.isl.audioautocut;

import android.webkit.JavascriptInterface;
import android.util.Log;
import android.widget.Toast;

public class WebAppInterface {
    private MainActivity activity;
    private static final String TAG = "WebAppInterface";
    
    public WebAppInterface(MainActivity activity) {
        this.activity = activity;
        Log.d(TAG, "🚀 WebAppInterface CRIADO com sucesso!");
        showToast("Interface Android pronta!");
    }
    
    @JavascriptInterface
    public void receiveConfig(String configJson) {
        Log.d(TAG, "📥 receiveConfig chamado: " + configJson);
        
        // Mostrar toast no app
        showToast("Config recebida do HTML");
        
        // Encaminhar para a MainActivity
        if (activity != null) {
            activity.handleWebCommand("refresh");
            
            // Atualizar a configuração se necessário
            if (configJson != null && !configJson.isEmpty()) {
                Log.d(TAG, "Config JSON recebida, tamanho: " + configJson.length());
            }
        } else {
            Log.e(TAG, "❌ activity é null em receiveConfig");
        }
    }
    
    @JavascriptInterface
    public void receiveCommand(String command) {
        Log.d(TAG, "📥 receiveCommand chamado: " + command);
        
        // Mostrar toast no app
        showToast("Comando: " + command);
        
        // Encaminhar para a MainActivity
        if (activity != null) {
            activity.handleWebCommand(command);
            Log.d(TAG, "✅ Comando encaminhado para MainActivity: " + command);
        } else {
            Log.e(TAG, "❌ activity é null em receiveCommand");
        }
    }
    
    @JavascriptInterface
    public String getConfig() {
        Log.d(TAG, "📤 getConfig chamado pelo HTML");
        
        if (activity != null) {
            String config = activity.getCurrentConfig();
            Log.d(TAG, "✅ Config retornada: " + (config != null ? "tamanho: " + config.length() : "null"));
            
            if (config == null || config.isEmpty()) {
                Log.d(TAG, "⚠️ Config vazia, retornando config padrão");
                // Retornar uma config padrão se estiver vazia
                return "{\"admob\":{\"app_id\":\"ca-app-pub-5799980230102893~3570820869\",\"banners\":[{\"ad_unit_id\":\"ca-app-pub-5799980230102893/9398812287\",\"ativo\":true}]},\"conteudo\":{\"url\":\"https://www.google.com\"}}";
            }
            
            return config;
        } else {
            Log.e(TAG, "❌ activity é null em getConfig");
            return "{\"erro\":\"activity null\"}";
        }
    }
    
    @JavascriptInterface
    public void showToast(String message) {
        Log.d(TAG, "📢 showToast: " + message);
        
        if (activity != null) {
            activity.runOnUiThread(() -> {
                try {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e(TAG, "❌ Erro ao mostrar toast: " + e.getMessage());
                }
            });
        }
    }
    
    @JavascriptInterface
    public void log(String message) {
        // Método para debug do HTML
        Log.d(TAG, "📝 HTML Log: " + message);
        
        // Também mostrar como toast para debug visual
        if (message.contains("✅") || message.contains("❌")) {
            showToast(message);
        }
    }
    
    @JavascriptInterface
    public String getDeviceInfo() {
        String info = "Android: " + android.os.Build.VERSION.RELEASE + 
                     ", SDK: " + android.os.Build.VERSION.SDK_INT +
                     ", Device: " + android.os.Build.MODEL;
        Log.d(TAG, "📱 Device info: " + info);
        return info;
    }
    
    @JavascriptInterface
    public void testConnection() {
        Log.d(TAG, "🔌 Teste de conexão recebido do HTML");
        showToast("✅ Conexão OK! App nativo respondendo");
        
        // Enviar resposta para o HTML via JavaScript
        if (activity != null && activity.webView != null) {
            activity.runOnUiThread(() -> {
                String jsCode = "javascript:(function() {" +
                        "console.log('✅ Resposta do teste de conexão');" +
                        "if(window.receiveFromNative) {" +
                        "  window.receiveFromNative('connectionTest', 'success');" +
                        "}" +
                        "})()";
                activity.webView.loadUrl(jsCode);
            });
        }
    }
    
    @JavascriptInterface
    public void sendBannerStatus(String status) {
        Log.d(TAG, "📊 Status do banner recebido do HTML: " + status);
        
        if ("loaded".equals(status)) {
            // HTML confirmou que o banner foi carregado
            activity.runOnUiThread(() -> {
                Toast.makeText(activity, "✅ Banner confirmado pelo HTML", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
