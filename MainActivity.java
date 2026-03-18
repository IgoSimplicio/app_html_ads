package com.isl.audioautocut;

import android.os.Bundle;
import android.util.Log;
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
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Call;
import okhttp3.Callback;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "AudioAutoCut";
    private WebView webView;
    private LinearLayout bannerContainer;
    private AdView adView;
    private OkHttpClient client;
    private Gson gson;
    private String currentConfig = "";
    
    // URLs CORRETAS (apontando para anuncios.json)
    private final String GITHUB_HTML_URL = "https://IgoSimplicio.github.io/app_html_ads/";
    private final String GITHUB_CONFIG_URL = "https://raw.githubusercontent.com/IgoSimplicio/app_html_ads/main/anuncios.json";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "App iniciado");
        
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
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "AdMob inicializado: " + initializationStatus.toString());
        });
        
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
            webSettings.setBuiltInZoomControls(false);
            webSettings.setDisplayZoomControls(false);
            webSettings.setAllowFileAccess(true);
            webSettings.setAllowContentAccess(true);
            webSettings.setCacheMode(WebSettings.LOAD_DEFAULT);
            
            // Adicionar interface JavaScript
            webView.addJavascriptInterface(new WebAppInterface(this), "AndroidInterface");
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    Log.d(TAG, "Carregando página: " + url);
                }
                
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    Log.d(TAG, "Página carregada: " + url);
                    
                    // Injetar JavaScript para debug
                    injectDebugScript();
                    
                    // Enviar configuração para o HTML quando a página carregar
                    sendConfigToWebView();
                }
                
                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    Log.e(TAG, "Erro no WebView: " + errorCode + " - " + description);
                    showToast("Erro ao carregar página: " + description);
                }
            });
            
            // Carregar o HTML do GitHub
            Log.d(TAG, "Carregando URL: " + GITHUB_HTML_URL);
            webView.loadUrl(GITHUB_HTML_URL);
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao configurar WebView", e);
        }
    }
    
    private void injectDebugScript() {
        String jsCode = "javascript:(function() {" +
                "console.log('📱 App nativo conectado');" +
                "if(!window.debugLog) {" +
                "  window.debugLog = [];" +
                "  window.originalLog = console.log;" +
                "  console.log = function(msg) {" +
                "    window.debugLog.push(msg);" +
                "    window.originalLog.apply(console, arguments);" +
                "    if(window.AndroidInterface && window.AndroidInterface.showToast) {" +
                "      window.AndroidInterface.showToast(msg);" +
                "    }" +
                "  };" +
                "}" +
                "console.log('✅ Debug ativado');" +
                "})()";
        webView.loadUrl(jsCode);
    }
    
    private void loadConfigFromGitHub() {
        Log.d(TAG, "Tentando carregar configuração de: " + GITHUB_CONFIG_URL);
        showToast("Carregando configuração...");
        
        Request request = new Request.Builder()
                .url(GITHUB_CONFIG_URL)
                .addHeader("User-Agent", "Android App")
                .addHeader("Accept", "application/json")
                .get()
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Falha na requisição: " + e.getMessage());
                e.printStackTrace();
                
                runOnUiThread(() -> {
                    String erro = "Erro de rede: " + e.getMessage();
                    showToast(erro);
                    
                    // Tentar carregar configuração padrão
                    loadDefaultConfig();
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    int responseCode = response.code();
                    Log.d(TAG, "Resposta HTTP: " + responseCode);
                    
                    if (response.isSuccessful()) {
                        String body = response.body().string();
                        Log.d(TAG, "Configuração recebida: " + body);
                        
                        // Validar se é JSON válido
                        try {
                            JsonParser.parseString(body);
                            currentConfig = body;
                            
                            runOnUiThread(() -> {
                                showToast("Configuração carregada com sucesso!");
                                updateBannersFromConfig();
                                sendConfigToWebView();
                            });
                            
                        } catch (Exception e) {
                            Log.e(TAG, "JSON inválido: " + body);
                            runOnUiThread(() -> {
                                showToast("Configuração inválida (JSON mal formatado)");
                                loadDefaultConfig();
                            });
                        }
                    } else {
                        String errorBody = response.body() != null ? response.body().string() : "vazio";
                        Log.e(TAG, "Erro HTTP " + responseCode + ": " + errorBody);
                        
                        runOnUiThread(() -> {
                            showToast("Erro HTTP " + responseCode + " ao carregar configuração");
                            loadDefaultConfig();
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao processar resposta", e);
                    runOnUiThread(() -> {
                        showToast("Erro ao processar configuração");
                        loadDefaultConfig();
                    });
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
            }
        });
    }
    
    private void loadDefaultConfig() {
        Log.d(TAG, "Carregando configuração padrão");
        
        // Configuração padrão baseada no seu anuncios.json
        String defaultConfig = "{" +
                "\"app\": {" +
                "  \"nome\": \"Audio Auto Cut\"," +
                "  \"versao\": \"1.0.0\"" +
                "}," +
                "\"admob\": {" +
                "  \"app_id\": \"ca-app-pub-5799980230102893~3570820869\"," +
                "  \"banners\": [" +
                "    {" +
                "      \"id\": \"banner_principal\"," +
                "      \"ad_unit_id\": \"ca-app-pub-5799980230102893/9398812287\"," +
                "      \"ativo\": true," +
                "      \"posicao\": \"bottom\"," +
                "      \"tamanho\": \"BANNER\"" +
                "    }" +
                "  ]," +
                "  \"intersticiais\": []," +
                "  \"recompensados\": []" +
                "}," +
                "\"conteudo\": {" +
                "  \"url\": \"https://www.google.com\"," +
                "  \"tipo\": \"webview\"" +
                "}," +
                "\"configuracoes\": {" +
                "  \"modo_teste\": false," +
                "  \"dispositivos_teste\": []," +
                "  \"atualizacao_automatica\": true," +
                "  \"intervalo_atualizacao\": 3600" +
                "}" +
                "}";
        
        currentConfig = defaultConfig;
        updateBannersFromConfig();
        sendConfigToWebView();
        showToast("Usando configuração padrão");
    }
    
    private void updateBannersFromConfig() {
        try {
            Log.d(TAG, "Atualizando banners com config: " + currentConfig);
            
            if (currentConfig == null || currentConfig.isEmpty()) {
                Log.e(TAG, "Config vazia");
                return;
            }
            
            JsonObject config = gson.fromJson(currentConfig, JsonObject.class);
            
            if (config.has("admob")) {
                JsonObject admob = config.getAsJsonObject("admob");
                
                // Verificar se tem banners no formato do seu JSON
                if (admob.has("banners")) {
                    // Pega o primeiro banner ativo
                    var bannersArray = admob.getAsJsonArray("banners");
                    if (bannersArray != null && bannersArray.size() > 0) {
                        for (int i = 0; i < bannersArray.size(); i++) {
                            JsonObject banner = bannersArray.get(i).getAsJsonObject();
                            if (banner.has("ativo") && banner.get("ativo").getAsBoolean()) {
                                String adUnitId = banner.get("ad_unit_id").getAsString();
                                loadBanner(adUnitId);
                                break;
                            }
                        }
                    }
                } else if (admob.has("banner_id")) {
                    // Formato alternativo
                    String bannerId = admob.get("banner_id").getAsString();
                    loadBanner(bannerId);
                }
            } else {
                // Fallback para o ID direto
                loadBanner("ca-app-pub-5799980230102893/9398812287");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar config para banner", e);
            loadBanner("ca-app-pub-5799980230102893/9398812287");
        }
    }
    
    private void loadBanner(String adUnitId) {
        runOnUiThread(() -> {
            try {
                Log.d(TAG, "Carregando banner: " + adUnitId);
                
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
                
                Log.d(TAG, "Banner carregado com sucesso");
                
            } catch (Exception e) {
                Log.e(TAG, "Erro ao carregar banner", e);
            }
        });
    }
    
    private void sendConfigToWebView() {
        if (!currentConfig.isEmpty() && webView != null) {
            String jsonConfig = currentConfig.replace("'", "\\'").replace("\n", " ").replace("\r", "");
            String jsCode = "javascript:(function() {" +
                    "try {" +
                    "  console.log('Recebendo config do app nativo');" +
                    "  if(window.receiveFromNative) {" +
                    "    window.receiveFromNative('configUpdated', '" + jsonConfig + "');" +
                    "    console.log('Config enviada para o HTML');" +
                    "  } else {" +
                    "    console.log('window.receiveFromNative não encontrado');" +
                    "    // Criar função se não existir" +
                    "    window.receiveFromNative = function(cmd, data) {" +
                    "      console.log('Comando nativo:', cmd, data);" +
                    "    };" +
                    "    window.receiveFromNative('configUpdated', '" + jsonConfig + "');" +
                    "  }" +
                    "} catch(e) {" +
                    "  console.log('Erro ao enviar config:', e);" +
                    "}" +
                    "})()";
            
            webView.loadUrl(jsCode);
        }
    }
    
    private void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            Log.d(TAG, "Toast: " + message);
        });
    }
    
    // Métodos chamados pelo JavaScript
    public void handleWebCommand(String command) {
        Log.d(TAG, "Comando recebido do HTML: " + command);
        
        runOnUiThread(() -> {
            switch(command) {
                case "refresh":
                    loadConfigFromGitHub();
                    break;
                case "show_banner":
                    updateBannersFromConfig();
                    break;
                case "debug":
                    showToast("Debug: " + currentConfig);
                    break;
                default:
                    showToast("Comando: " + command);
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
