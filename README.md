# LUMI — Alerta Inteligente de Medicamentos
O LUMI é um aplicativo mobile desenvolvido para auxiliar na organização e no controle dos horários de medicamentos. A aplicação busca contribuir para uma rotina de tratamento mais organizada por meio do cadastro de medicamentos e de lembretes de horários, facilitando o acompanhamento da medicação pelo usuário.

## Stack e estrutura

- Kotlin + Jetpack Compose (Material 3), minSdk 26
- Pacotes em `app/src/main/java/com/lumi/`:
  - `data/` banco local (Room/SQLite): remédios, doses tomadas e responsável
  - `ui/theme/` tema e tokens de cor
- Build: `./gradlew assembleDebug` · Testes: `./gradlew testDebugUnitTest` · Testes do banco (emulador): `./gradlew connectedDebugAndroidTest`
