package com.isl.audioautocut;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    
    private WebView webView;
    private LinearLayout bannerContainer;
    private AdView adView;
    private OkHttpClient client;
    private Gson gson;
    private String currentConfig = "";
    
    // URL do seu HTML no GitHub Pages
    private final String GITHUB_HTML_URL = "https://IgoSimplicio.github.io/app_html_ads/";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar componentes
        webView = findViewById(R.id.webview);
        bannerContainer = findViewById(R.id.banner_container);
        client = new OkHttpClient();
        gson = new Gson();
        
        // Configurar WebView
        setupWebView();
        
        // Inicializar AdMob
        MobileAds.initialize(this, initializationStatus -> {
            // AdMob inicializado
        });
        
        // Carregar configuração do GitHub
        loadConfigFromGitHub();
    }
    
    private void setupWebView() {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        
        // Adicionar interface JavaScript
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Enviar configuração para o HTML quando a página carregar
                sendConfigToWebView();
            }
        });
        
        // Carregar o HTML do GitHub
        webView.loadUrl(GITHUB_HTML_URL);
    }
    
    private void loadConfigFromGitHub() {
        String configUrl = "https://raw.githubusercontent.com/IgoSimplicio/app_html_ads/main/config.json";
        
        Request request = new Request.Builder()
                .url(configUrl)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, 
                            "Erro ao carregar configuração", Toast.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    currentConfig = response.body().string();
                    runOnUiThread(() -> {
                        // Atualizar banners baseado na configuração
                        updateBannersFromConfig();
                        // Enviar para WebView
                        sendConfigToWebView();
                    });
                }
            }
        });
    }
    
    private void updateBannersFromConfig() {
        try {
            JsonObject config = gson.fromJson(currentConfig, JsonObject.class);
            if (config.has("admob")) {
                JsonObject admob = config.getAsJsonObject("admob");
                if (admob.has("banners")) {
                    // Carregar banner
                    loadBanner("ca-app-pub-5799980230102893/9398812287");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadBanner(String adUnitId) {
        runOnUiThread(() -> {
            if (adView != null) {
                adView.destroy();
            }
            
            adView = new AdView(this);
            adView.setAdSize(com.google.android.gms.ads.AdSize.BANNER);
            adView.setAdUnitId(adUnitId);
            
            bannerContainer.removeAllViews();
            bannerContainer.addView(adView);
            
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        });
    }
    
    private void sendConfigToWebView() {
        if (!currentConfig.isEmpty() && webView != null) {
            String jsCode = "javascript:(function() {" +
                    "if(window.receiveFromNative) {" +
                    "window.receiveFromNative('configUpdated', '" + 
                    currentConfig.replace("'", "\\'").replace("\n", " ") + 
                    "');" +
                    "}" +
                    "})()";
            webView.loadUrl(jsCode);
        }
    }
    
    // Métodos chamados pelo JavaScript
    public void handleWebCommand(String command) {
        runOnUiThread(() -> {
            switch(command) {
                case "refresh":
                    loadConfigFromGitHub();
                    break;
                case "show_banner":
                    updateBannersFromConfig();
                    break;
            }
        });
    }
    
    public String getCurrentConfig() {
        return currentConfig;
    }
    
    @Override
    protected void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
