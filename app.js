// Configurações
const CONFIG = {
    GITHUB_CONFIG_URL: 'https://raw.githubusercontent.com/SEU_USUARIO/SEU_REPO/main/config/anuncios.json',
    DEFAULT_URL: 'https://www.google.com',
    UPDATE_INTERVAL: 30000 // 30 segundos
};

// Estado do app
let appState = {
    config: null,
    bannerLoaded: false,
    currentUrl: CONFIG.DEFAULT_URL
};

// Elementos DOM
const elements = {
    iframe: document.getElementById('webview-frame'),
    bannerContainer: document.getElementById('bannerContainer'),
    loadingOverlay: document.getElementById('loadingOverlay')
};

// Inicialização
document.addEventListener('DOMContentLoaded', async () => {
    showLoading();
    await loadConfigFromGitHub();
    setupIframe();
    hideLoading();
    
    // Configurar atualização periódica (se necessário)
    // setInterval(loadConfigFromGitHub, CONFIG.UPDATE_INTERVAL);
});

// Carregar configuração do GitHub
async function loadConfigFromGitHub() {
    try {
        const response = await fetch(CONFIG.GITHUB_CONFIG_URL, {
            method: 'GET',
            headers: {
                'Accept': 'application/json',
                'Cache-Control': 'no-cache'
            }
        });
        
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const config = await response.json();
        appState.config = config;
        
        console.log('Configuração carregada:', config);
        
        // Verificar modo de teste
        if (config.admob.test_mode) {
            console.log('Modo de teste ativado');
            // Adicionar dispositivos de teste se necessário
        }
        
        // Recarregar banner se necessário
        reloadBanner();
        
    } catch (error) {
        console.error('Erro ao carregar configuração:', error);
        showError('Erro ao carregar configurações. Usando configuração padrão.');
    }
}

// Configurar iframe
function setupIframe() {
    // Opções do iframe
    elements.iframe.src = appState.currentUrl;
    
    // Eventos do iframe
    elements.iframe.onload = () => {
        console.log('Iframe carregado:', appState.currentUrl);
        // Esconder loading se ainda estiver visível
        hideLoading();
    };
    
    elements.iframe.onerror = (error) => {
        console.error('Erro no iframe:', error);
        showError('Erro ao carregar o conteúdo');
        hideLoading();
    };
}

// Recarregar banner
function reloadBanner() {
    const bannerContainer = elements.bannerContainer;
    
    // Limpar container
    bannerContainer.innerHTML = '';
    bannerContainer.classList.add('loading');
    
    // Verificar se o banner deve ser exibido
    if (appState.config && appState.config.admob) {
        // Criar novo elemento de anúncio
        const insElement = document.createElement('ins');
        insElement.className = 'adsbygoogle';
        insElement.style.display = 'inline-block';
        insElement.style.width = '320px';
        insElement.style.height = '100px';
        insElement.setAttribute('data-ad-client', 'ca-pub-5799980230102893');
        insElement.setAttribute('data-ad-slot', '9398812287');
        insElement.setAttribute('data-ad-format', 'banner');
        insElement.setAttribute('data-full-width-responsive', 'true');
        
        bannerContainer.appendChild(insElement);
        
        // Recarregar anúncio
        try {
            (adsbygoogle = window.adsbygoogle || []).push({});
            bannerContainer.classList.remove('loading');
            appState.bannerLoaded = true;
        } catch (error) {
            console.error('Erro ao carregar banner:', error);
            bannerContainer.classList.remove('loading');
            showError('Erro ao carregar anúncio');
        }
    }
}

// Funções de utilidade
function showLoading() {
    if (elements.loadingOverlay) {
        elements.loadingOverlay.style.display = 'flex';
    }
}

function hideLoading() {
    if (elements.loadingOverlay) {
        elements.loadingOverlay.style.display = 'none';
    }
}

function showError(message) {
    console.error(message);
    // Criar elemento de erro
    const errorDiv = document.createElement('div');
    errorDiv.style.position = 'fixed';
    errorDiv.style.bottom = '120px';
    errorDiv.style.left = '50%';
    errorDiv.style.transform = 'translateX(-50%)';
    errorDiv.style.backgroundColor = '#f44336';
    errorDiv.style.color = 'white';
    errorDiv.style.padding = '10px 20px';
    errorDiv.style.borderRadius = '5px';
    errorDiv.style.zIndex = '2000';
    errorDiv.textContent = message;
    
    document.body.appendChild(errorDiv);
    
    // Remover após 3 segundos
    setTimeout(() => {
        errorDiv.remove();
    }, 3000);
}

// Função para navegar para outra URL (pode ser chamada externamente)
function navigateTo(url) {
    if (url && elements.iframe) {
        showLoading();
        appState.currentUrl = url;
        elements.iframe.src = url;
    }
}

// Função para recarregar configurações manualmente
function refreshConfig() {
    showLoading();
    loadConfigFromGitHub().then(() => {
        hideLoading();
    });
}

// Expor funções globalmente se necessário
window.navigateTo = navigateTo;
window.refreshConfig = refreshConfig;