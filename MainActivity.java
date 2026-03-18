package com.isl.audioautocut;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "AudioAutoCut";
    private WebView webView;
    private LinearLayout bannerContainer;
    private AdView adView;
    private OkHttpClient client;
    private Gson gson;
    private String currentConfig = "";
    private TextView statusText;
    private Button btnTestBanner;
    private Button btnTestConfig;
    
    private final String GITHUB_HTML_URL = "https://IgoSimplicio.github.io/app_html_ads/";
    private final String GITHUB_CONFIG_URL = "https://raw.githubusercontent.com/IgoSimplicio/app_html_ads/main/anuncios.json";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Inicializar componentes
        webView = findViewById(R.id.webview);
        bannerContainer = findViewById(R.id.banner_container);
        statusText = findViewById(R.id.status_text);
        btnTestBanner = findViewById(R.id.btn_test_banner);
        btnTestConfig = findViewById(R.id.btn_test_config);
        
        client = new OkHttpClient();
        gson = new Gson();
        
        // Configurar botões de teste
        setupTestButtons();
        
        // Mostrar status inicial
        updateStatus("Iniciando app...");
        
        // Configurar WebView
        setupWebView();
        
        // Inicializar AdMob com configuração de teste
        initializeAdMob();
        
        // Carregar configuração
        loadConfigFromGitHub();
    }
    
    private void setupTestButtons() {
        btnTestBanner.setOnClickListener(v -> {
            updateStatus("Testando banner manual...");
            testBannerDirectly();
        });
        
        btnTestConfig.setOnClickListener(v -> {
            updateStatus("Testando configuração...");
            testConfigDirectly();
        });
    }
    
    private void setupWebView() {
        try {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            
            webView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    updateStatus("HTML carregado: " + url);
                    sendConfigToWebView();
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    updateStatus("Erro WebView: " + description);
                }
            });
            
            webView.loadUrl(GITHUB_HTML_URL);
            updateStatus("Carregando HTML...");
            
        } catch (Exception e) {
            updateStatus("Erro WebView: " + e.getMessage());
        }
    }
    
    private void initializeAdMob() {
        updateStatus("Inicializando AdMob...");
        
        // Configurar dispositivos de teste
        RequestConfiguration configuration = new RequestConfiguration.Builder()
                .setTestDeviceIds(Arrays.asList("ABCDEF012345"))
                .build();
        MobileAds.setRequestConfiguration(configuration);
        
        MobileAds.initialize(this, initializationStatus -> {
            updateStatus("AdMob inicializado!");
            Log.d(TAG, "AdMob status: " + initializationStatus.toString());
        });
    }
    
    private void loadConfigFromGitHub() {
        updateStatus("Buscando configuração...");
        Log.d(TAG, "URL: " + GITHUB_CONFIG_URL);
        
        Request request = new Request.Builder()
                .url(GITHUB_CONFIG_URL)
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    String erro = "Falha: " + e.getMessage();
                    updateStatus(erro);
                    Log.e(TAG, erro, e);
                    loadDefaultConfig();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        Log.d(TAG, "Config recebida: " + body);
                        
                        runOnUiThread(() -> {
                            updateStatus("Config carregada!");
                            currentConfig = body;
                            processConfigAndShowBanner();
                        });
                    } else {
                        runOnUiThread(() -> {
                            updateStatus("HTTP " + response.code());
                            loadDefaultConfig();
                        });
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        updateStatus("Erro: " + e.getMessage());
                        loadDefaultConfig();
                    });
                }
            }
        });
    }
    
    private void loadDefaultConfig() {
        updateStatus("Usando config padrão");
        currentConfig = "{\"admob\":{\"app_id\":\"ca-app-pub-5799980230102893~3570820869\",\"banners\":[{\"ad_unit_id\":\"ca-app-pub-5799980230102893/9398812287\",\"ativo\":true}]}}";
        processConfigAndShowBanner();
    }
    
    private void processConfigAndShowBanner() {
        try {
            JsonObject config = gson.fromJson(currentConfig, JsonObject.class);
            String bannerId = "ca-app-pub-5799980230102893/9398812287"; // Padrão
            
            if (config.has("admob")) {
                JsonObject admob = config.getAsJsonObject("admob");
                if (admob.has("banners")) {
                    var banners = admob.getAsJsonArray("banners");
                    if (banners != null && banners.size() > 0) {
                        JsonObject banner = banners.get(0).getAsJsonObject();
                        if (banner.has("ad_unit_id")) {
                            bannerId = banner.get("ad_unit_id").getAsString();
                        }
                    }
                }
            }
            
            updateStatus("ID Banner: " + bannerId);
            loadBanner(bannerId);
            
        } catch (Exception e) {
            updateStatus("Erro processando config: " + e.getMessage());
            loadBanner("ca-app-pub-5799980230102893/9398812287");
        }
    }
    
    private void loadBanner(String adUnitId) {
        runOnUiThread(() -> {
            try {
                updateStatus("Carregando banner: " + adUnitId);
                
                if (adView != null) {
                    adView.destroy();
                }
                
                // Criar novo banner
                adView = new AdView(this);
                adView.setAdUnitId(adUnitId);
                adView.setAdSize(AdSize.BANNER);
                
                // Limpar e adicionar ao container
                bannerContainer.removeAllViews();
                bannerContainer.addView(adView);
                
                // Configurar listener para debug
                adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        updateStatus("✅ Banner carregado!");
                        Log.d(TAG, "Banner carregado com sucesso");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        String erro = "❌ Falha ao carregar banner: " + loadAdError.getMessage();
                        updateStatus(erro);
                        Log.e(TAG, erro);
                        
                        // Mostrar erro detalhado
                        String errorDetails = "Código: " + loadAdError.getCode() + "\n" +
                                             "Mensagem: " + loadAdError.getMessage() + "\n" +
                                             "Domínio: " + loadAdError.getDomain();
                        Log.e(TAG, errorDetails);
                    }
                    
                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        Log.d(TAG, "Banner clicado");
                    }
                });
                
                // Criar requisição de teste
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
                
            } catch (Exception e) {
                updateStatus("Erro ao carregar banner: " + e.getMessage());
                Log.e(TAG, "Erro loadBanner", e);
            }
        });
    }
    
    private void testBannerDirectly() {
        updateStatus("Teste direto de banner...");
        loadBanner("ca-app-pub-5799980230102893/9398812287");
    }
    
    private void testConfigDirectly() {
        updateStatus("Testando URL config...");
        
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(GITHUB_CONFIG_URL);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                    String line;
                    StringBuilder response = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String finalResponse = response.toString();
                    runOnUiThread(() -> {
                        updateStatus("✅ Config OK! Tamanho: " + finalResponse.length());
                        Log.d(TAG, "Conteúdo: " + finalResponse);
                    });
                } else {
                    runOnUiThread(() -> updateStatus("❌ HTTP " + responseCode));
                }
                conn.disconnect();
            } catch (Exception e) {
                runOnUiThread(() -> updateStatus("❌ Erro teste: " + e.getMessage()));
            }
        }).start();
    }
    
    private void sendConfigToWebView() {
        if (!currentConfig.isEmpty() && webView != null) {
            String jsCode = "javascript:(function() {" +
                    "console.log('Config recebida do app');" +
                    "if(window.receiveFromNative) {" +
                    "  window.receiveFromNative('configUpdated', '" + 
                    currentConfig.replace("'", "\\'") + "');" +
                    "}" +
                    "})()";
            webView.loadUrl(jsCode);
        }
    }
    
    private void updateStatus(String message) {
        runOnUiThread(() -> {
            statusText.setText(message);
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Log.d(TAG, message);
        });
    }
    
    public void handleWebCommand(String command) {
        updateStatus("Comando HTML: " + command);
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
