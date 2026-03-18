package com.isl.audioautocut;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
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
    private boolean isConfigLoaded = false;
    
    private final String GITHUB_HTML_URL = "https://IgoSimplicio.github.io/app_html_ads/";
    private final String GITHUB_CONFIG_URL = "https://raw.githubusercontent.com/IgoSimplicio/app_html_ads/main/anuncios.json";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "🚀 App iniciado");
        
        // Inicializar componentes
        webView = findViewById(R.id.webview);
        bannerContainer = findViewById(R.id.banner_container);
        
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        
        // Configurar WebView
        setupWebView();
        
        // Inicializar AdMob
        initializeAdMob();
        
        // Carregar configuração do GitHub
        loadConfigFromGitHub();
    }
    
    private void setupWebView() {
        try {
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            
            // Adicionar interface JavaScript - ISSO É CRÍTICO!
            WebAppInterface interface_ = new WebAppInterface(this);
            webView.addJavascriptInterface(interface_, "AndroidInterface");
            Log.d(TAG, "✅ JavaScriptInterface adicionado");
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "📄 Página carregada: " + url);
                    
                    // Aguardar um pouco e enviar configuração
                    new Handler().postDelayed(() -> {
                        sendConfigToWebView();
                    }, 1000);
                }
            });
            
            Log.d(TAG, "📱 Carregando HTML: " + GITHUB_HTML_URL);
            webView.loadUrl(GITHUB_HTML_URL);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro WebView: " + e.getMessage());
        }
    }
    
    private void initializeAdMob() {
        try {
            // Configurar dispositivos de teste
            RequestConfiguration configuration = new RequestConfiguration.Builder()
                    .setTestDeviceIds(Arrays.asList("ABCDEF012345"))
                    .build();
            MobileAds.setRequestConfiguration(configuration);
            
            MobileAds.initialize(this, initializationStatus -> {
                Log.d(TAG, "✅ AdMob inicializado");
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro AdMob: " + e.getMessage());
        }
    }
    
    private void loadConfigFromGitHub() {
        Log.d(TAG, "🌐 Carregando config de: " + GITHUB_CONFIG_URL);
        
        Request request = new Request.Builder()
                .url(GITHUB_CONFIG_URL)
                .addHeader("User-Agent", "Android App")
                .addHeader("Accept", "application/json")
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "❌ Falha na requisição: " + e.getMessage());
                runOnUiThread(() -> {
                    loadDefaultConfig();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        Log.d(TAG, "✅ Config recebida: " + body);
                        
                        runOnUiThread(() -> {
                            currentConfig = body;
                            isConfigLoaded = true;
                            Log.d(TAG, "📦 Config salva, tamanho: " + currentConfig.length());
                            
                            // Processar e mostrar banner
                            processConfigAndShowBanner();
                            
                            // Enviar para WebView
                            sendConfigToWebView();
                        });
                    } else {
                        Log.e(TAG, "❌ HTTP " + response.code());
                        runOnUiThread(() -> loadDefaultConfig());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Erro: " + e.getMessage());
                    runOnUiThread(() -> loadDefaultConfig());
                }
            }
        });
    }
    
    private void loadDefaultConfig() {
        Log.d(TAG, "📝 Usando config padrão");
        currentConfig = "{\"app\":{\"nome\":\"Audio Auto Cut\"},\"admob\":{\"app_id\":\"ca-app-pub-5799980230102893~3570820869\",\"banners\":[{\"ad_unit_id\":\"ca-app-pub-5799980230102893/9398812287\",\"ativo\":true}]},\"conteudo\":{\"url\":\"https://www.google.com\"}}";
        isConfigLoaded = true;
        processConfigAndShowBanner();
        sendConfigToWebView();
    }
    
    private void processConfigAndShowBanner() {
        try {
            Log.d(TAG, "🔄 Processando config para banner");
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
                            Log.d(TAG, "📋 Banner ID da config: " + bannerId);
                        }
                    }
                }
            }
            
            loadBanner(bannerId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Erro processando config: " + e.getMessage());
            loadBanner("ca-app-pub-5799980230102893/9398812287");
        }
    }
    
    private void loadBanner(String adUnitId) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "📢 Carregando banner: " + adUnitId);
                
                if (adView != null) {
                    adView.destroy();
                }
                
                adView = new AdView(this);
                adView.setAdUnitId(adUnitId);
                adView.setAdSize(AdSize.BANNER);
                
                bannerContainer.removeAllViews();
                bannerContainer.addView(adView);
                
                // Listener para debug
                adView.setAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdLoaded() {
                        super.onAdLoaded();
                        Log.d(TAG, "✅ Banner carregado!");
                        
                        // Notificar HTML
                        sendCommandToWebView("bannerLoaded");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(com.google.android.gms.ads.LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        Log.e(TAG, "❌ Falha banner: " + loadAdError.getMessage());
                        
                        // Notificar HTML
                        sendCommandToWebView("bannerFailed");
                    }
                });
                
                AdRequest adRequest = new AdRequest.Builder().build();
                adView.loadAd(adRequest);
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Erro loadBanner: " + e.getMessage());
            }
        });
    }
    
    private void sendConfigToWebView() {
        if (!currentConfig.isEmpty() && webView != null) {
            String escapedConfig = currentConfig
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", " ")
                    .replace("\r", "");
            
            String jsCode = "javascript:(function() {" +
                    "try {" +
                    "  console.log('📦 Recebendo config do app nativo');" +
                    "  if(window.receiveFromNative) {" +
                    "    window.receiveFromNative('configUpdated', '" + escapedConfig + "');" +
                    "    console.log('✅ Config enviada');" +
                    "  } else {" +
                    "    console.log('⚠️ receiveFromNative não encontrado');" +
                    "  }" +
                    "} catch(e) {" +
                    "  console.log('❌ Erro:', e);" +
                    "}" +
                    "})()";
            
            webView.loadUrl(jsCode);
            Log.d(TAG, "📤 Config enviada para WebView");
        }
    }
    
    private void sendCommandToWebView(String command) {
        if (webView != null) {
            String jsCode = "javascript:(function() {" +
                    "try {" +
                    "  if(window.receiveFromNative) {" +
                    "    window.receiveFromNative('" + command + "', null);" +
                    "  }" +
                    "} catch(e) {}" +
                    "})()";
            webView.loadUrl(jsCode);
            Log.d(TAG, "📤 Comando enviado: " + command);
        }
    }
    
    // Métodos chamados pelo JavaScript
    public void handleWebCommand(String command) {
        Log.d(TAG, "📨 Comando do HTML: " + command);
        
        runOnUiThread(() -> {
            switch(command) {
                case "refresh":
                    loadConfigFromGitHub();
                    break;
                case "show_banner":
                    processConfigAndShowBanner();
                    break;
                case "debug":
                    Toast.makeText(this, "Config: " + (isConfigLoaded ? "carregada" : "vazia"), Toast.LENGTH_SHORT).show();
                    break;
                case "config_received":
                    Log.d(TAG, "✅ HTML confirmou recebimento da config");
                    break;
            }
        });
    }
    
    public String getCurrentConfig() {
        Log.d(TAG, "📞 getCurrentConfig() chamado, retornando: " + (currentConfig.isEmpty() ? "vazio" : "config existe"));
        
        // Se não tiver config, retorna uma config padrão
        if (currentConfig.isEmpty()) {
            String defaultConfig = "{\"app\":{\"nome\":\"Audio Auto Cut\"},\"conteudo\":{\"url\":\"https://www.google.com\"}}";
            Log.d(TAG, "📝 Retornando config padrão");
            return defaultConfig;
        }
        
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
