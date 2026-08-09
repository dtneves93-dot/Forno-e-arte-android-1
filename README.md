# Forno e Arte — Android

Aplicativo offline de gestão de pedidos para pizzaria, desenvolvido em Kotlin, Jetpack Compose, Material 3, MVVM e Room.

## Executar

Abra o projeto no Android Studio (JDK 17), aguarde a sincronização e execute em um dispositivo Android 7.0 ou superior. Pela linha de comando, com Gradle 8.9 instalado, use `gradle assembleDebug`; o APK ficará em `app/build/outputs/apk/debug/app-debug.apk`. O repositório não versiona o Gradle Wrapper porque seu JAR é binário; no GitHub Actions, o Gradle é provisionado por `gradle/actions/setup-gradle`.

## Integrações

- **WhatsApp:** abre a conversa pelo `wa.me`, com mensagem correspondente ao status pronta para confirmação.
- **InfinityPay:** `PaymentGateway` mantém a UI desacoplada. O app inclui um gateway simulado funcional; a implementação real deve chamar um backend seguro que armazene as credenciais.
- **Nota fiscal:** `FiscalInvoiceService` define o contrato para uma futura API fiscal. Nenhum segredo é armazenado no aplicativo.
- **PDF:** o resumo é gerado localmente e compartilhado pelo seletor padrão do Android.

Os pedidos e todo o histórico permanecem no banco Room local e funcionam sem conexão.
