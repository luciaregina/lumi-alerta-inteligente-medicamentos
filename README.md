# LUMI — Alerta Inteligente de Medicamentos
O LUMI é um aplicativo mobile desenvolvido para auxiliar na organização e no controle dos horários de medicamentos. A aplicação busca contribuir para uma rotina de tratamento mais organizada por meio do cadastro de medicamentos e de lembretes de horários, facilitando o acompanhamento da medicação pelo usuário.

## Stack e estrutura

- Kotlin + Jetpack Compose (Material 3), minSdk 26
- Pacotes em `app/src/main/java/com/lumi/`:
  - `data/` banco local (Room/SQLite): remédios, doses tomadas e responsável
  - `ui/theme/` tema e tokens de cor
- Build: `./gradlew assembleDebug` · Testes: `./gradlew testDebugUnitTest` · Testes do banco (emulador): `./gradlew connectedDebugAndroidTest`

## Simulado (aguardando definição do time)

| Função | Hoje | Para virar real |
|---|---|---|
| Escanear receita | Botão preenche dados de exemplo | Câmera + OCR (ex.: ML Kit Text Recognition) |
| Farmácias próximas | Lista fictícia; "Ir" abre o app de mapas | GPS + API de lugares (Google Places / OpenStreetMap) |
| Leitura do QR do responsável | Botão "Simular: responsável escaneou" | Câmera + leitor de QR no app do responsável |
| Avisos ao responsável | Lista na aba Cuidador | Push (ex.: Firebase Cloud Messaging) + API |
| Login, perfis e sincronização | Tudo fica só no aparelho | Backend com autenticação |

Privacidade (LGPD): dados ficam só no aparelho, sem backup automático na nuvem; compartilhar com o responsável exige consentimento; "Apagar meus dados" em Ajustes apaga tudo.
