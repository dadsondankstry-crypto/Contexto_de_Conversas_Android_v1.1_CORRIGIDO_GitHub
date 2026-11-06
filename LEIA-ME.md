# Contexto de Conversas — Android nativo 1.0

Projeto Android em **Java nativo** (sem HTML, sem PWA), pronto para compilar no GitHub Actions. Os arquivos estão **diretamente na raiz deste ZIP**. Interface em português brasileiro.

## O que faz

- Várias conversas separadas, com mensagens organizadas em ordem, marcadas como **USUÁRIO** ou **CHATGPT**.
- **Colar texto copiado agora** (manual e confiável), além de adicionar e editar mensagem, corrigir autor, excluir e desfazer.
- **Botão flutuante** ao ativar o serviço de acessibilidade. Toque no botão após copiar: uma tela momentânea assume foco, lê a cópia e salva.
- **Captura automática EXPERIMENTAL** após copiar: opção separada, com teclado auxiliar definido como teclado padrão. O Android 10+ não permite que apps comuns leiam a área de transferência em segundo plano, **mesmo com permissão de sobreposição/acessibilidade**. O app inclui um teclado auxiliar próprio (IME), que é uma das exceções reconhecidas pelo Android. Para a coleta ser confiável, ligue também o serviço de acessibilidade, que mantém o processo de monitoramento ativo e acompanha qual aplicativo está em primeiro plano. Em aparelhos Xiaomi/HyperOS, o comportamento pode variar; se não funcionar, use a captura pelo botão flutuante. **Não há garantia de captura automática em todo aparelho, especialmente quando o teclado está fechado.**
- Por padrão, captura automática apenas quando o app ChatGPT (`com.openai.chatgpt`) é o ativo. A opção de monitorar outros apps fica desativada.
- Recebe texto via menu **Compartilhar** do Android (quando o outro app oferece texto de fato).
- **Importar exportação oficial ZIP do ChatGPT** com `conversations.json` (ou variantes numeradas `conversations-*.json`) e escolher um chat. O importador mantém o caminho principal da conversa (`current_node` e ancestrais) e apenas mensagens de texto do usuário e do assistente. Arquivos multimodais, anexos, áudios e certos conteúdos não textuais não se convertem automaticamente em texto.
- Exportar uma conversa como **TXT**, backup próprio em **JSON**, copiar tudo ou compartilhar.
- Persistência em SQLite, sem login, **sem permissão de INTERNET**, sem envio de conteúdo a servidores.

## Instalar e começar

1. Abra **Contexto de Conversas** no celular.
2. Crie uma conversa com o nome do projeto, ou use a primeira conversa criada.
3. No ChatGPT, copie **uma mensagem**. Volte ao app e pressione **Colar texto copiado agora**.
4. Repita alternando sua mensagem e a resposta do ChatGPT. O aplicativo alterna automaticamente os autores; se copiar fora da ordem, toque na mensagem para corrigir o autor.
5. Para ficar no ChatGPT sem trocar de tela, habilite **Acessibilidade** → **Contexto - botão flutuante**, e toque no botão 📋 após cada cópia.
6. Para experimentar o automático, marque **Salvar cópias automaticamente**, em **Teclado** habilite/seleciona **Teclado auxiliar Contexto** como padrão, e mantenha o serviço de acessibilidade ativo. Isso substitui temporariamente o teclado normal. O teclado auxiliar é básico. Você pode restaurar o Gboard a qualquer momento via seletor de teclados. **Durante a captura automática, copie só mensagens que deseja salvar.**
7. **Salvar TXT** ao final, depois anexe o arquivo ao novo ChatGPT.

### Sobre permissões

- **Acessibilidade** é ativada manualmente nas configurações do Android. Este serviço **não inspeciona texto da tela**, pois só acompanha a janela em primeiro plano e oferece botão flutuante. Não é um acesso para burlar as limitações do clipboard.
- **Teclado padrão** é uma escolha do usuário nas configurações de métodos de entrada. Esse recurso existe para o teste da captura automática, mas pode afetar sua digitação: é um teclado simples e sem autocorretor.
- Não requer permissão de armazenamento geral, microfone, SMS, internet, nem acesso irrestrito a outros aplicativos.
- Qualquer conteúdo copiado pode ser sensível. A captura automática é desligada por padrão. Textos marcados como sensíveis pelo app de origem são ignorados quando identificáveis.

## Como solicitar a exportação oficial do ChatGPT

No aplicativo ChatGPT para Android, abra a **barra lateral**, toque no **perfil** → **Configurações** → **Controles de dados** → **Exportar dados**. Confirme. A OpenAI envia um link para seu e-mail ou SMS; pode levar até **7 dias** e o link normalmente expira em **24 horas**. Baixe o ZIP original e selecione-o em **Importar exportação ChatGPT (ZIP)** neste aplicativo. A disponibilidade da exportação depende do tipo de conta e workspace.

Se a opção não aparecer no aplicativo, entre no ChatGPT pelo navegador → **Configurações** → **Controles de dados** → **Exportar**. O ZIP contém dados privados: proteja o arquivo.

Fontes oficiais:
- https://help.openai.com/pt-br/articles/8167885-como-exportar-seus-dados-do-aplicativo-chatgpt-para-android
- https://help.openai.com/pt-br/articles/7260999-exporting-your-chatgpt-history-and-data
- https://developer.android.com/about/versions/10/privacy/changes

## GitHub Actions / gerar o APK

1. Baixe o ZIP deste projeto.
2. No **Gerenciador GitHub**, envie o conteúdo do ZIP para a raiz de um repositório, preservando pastas `.github` e `app`.
3. GitHub → **Actions** → **Gerar APK - Contexto de Conversas**. O push em `main` ou `master` dispara automaticamente; também aceita **Run workflow**.
4. Depois da execução bem-sucedida, baixe o artefato **ContextoConversas-APK**; dentro há `app-debug.apk`.
5. Instale o APK no aparelho, liberando a instalação de fonte desconhecida quando o Android pedir.

O workflow usa Ubuntu 24.04, Java 17, SDK 35, Gradle 8.7 e Android Gradle Plugin 8.6.1. Usa as ações oficiais `checkout@v4`, `setup-java@v4`, `setup-android@v4`, `setup-gradle@v4` e `upload-artifact@v4`, com até 3 tentativas para rede instável/Maven 429.

**Status dos testes:** foi feita revisão estrutural do ZIP, YAML, recursos e código, mas **não houve compilação do APK dentro deste ambiente** nem teste em um aparelho real. O GitHub Actions deve confirmar a compilação. Se houver erro, envie o log exato da primeira etapa vermelha.
