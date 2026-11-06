# Contexto de Conversas — 1.1

Correção do erro de compilação `:app:processDebugResources`:
`style attribute android:attr/windowDimAmount not found`.

No arquivo `app/src/main/res/values/styles.xml`, alterado `android:windowDimAmount` para `android:backgroundDimAmount`.
Mantidos os recursos do aplicativo e o workflow `.github/workflows/main.yml`.
Atualizado versionCode para 2 e versionName para 1.1.

O projeto foi conferido estruturalmente, mas a compilação completa ainda precisa ser executada no GitHub Actions.

Para instalar: envie este ZIP diretamente pelo Gerenciador GitHub, aguarde o Actions, baixe o artefato `ContextoConversas-APK` e extraia `app-debug.apk`.
