# ⛏️ ForgeFocus — Produtividade Gamificada

O **ForgeFocus** é um aplicativo Android nativo baseado na metodologia de blocos de foco (estilo Pomodoro). O usuário registra blocos de 30 minutos de foco para evoluir metas de longo prazo de forma gamificada.

O projeto foi construído em Kotlin usando os padrões recomendados pelo Google (**Guide to App Architecture**), aplicando **Clean Architecture** e desenvolvimento orientado a pacotes por funcionalidade (*Feature-Driven*).

---

## 📐 Estrutura do Projeto & Arquitetura

O projeto adota o fluxo unidirecional de dados (**UDF**) aliado ao padrão **MVVM/MVI** para manipulação de estados e intenções da interface de forma imutável.

A árvore de diretórios separa a infraestrutura global (`core`) do módulo específico da funcionalidade de metas (`features`):


### Responsabilidades das Camadas:
* **Data:** Gerencia o armazenamento local com **Room Database**. As entidades do banco (`GoalEntity`, `ProgressLogEntity`) são isoladas e convertidas para objetos de domínio puramente em Kotlin através de mappers.
* **Domain:** Isola as regras de negócio em casos de uso independentes (`BreakMountainBlockUseCase`, `GetDashboardDataUseCase`), facilitando a escrita de testes unitários.
* **Presentation:** Consome estados imutáveis do `DashboardViewModel` via `StateFlow` e processa as ações do usuário mapeadas como eventos unificados (`MountainsEvent`).

---

## 🛠️ Stack Tecnológica

* **UI:** Jetpack Compose (Paradigma declarativo e componentes customizados com `MountainCanvas`).
* **Navegação:** Type-Safe Navigation (Navegação baseada em tipos seguros/objetos, eliminando Strings).
* **Injeção de Dependência:** Hilt (Dagger).
* **Banco de Dados Local:** Room Database com controle de conversores de tipo (`Converters`).
* **Assincronismo:** Kotlin Coroutines & Flow (uso de operadores como `flatMapLatest` e `combine`).

---

## 🚀 Desafios Implementados

### 1. Filtro Temporal Dinâmico de Dados
* **Contexto:** Filtrar o histórico de blocos de foco do usuário por blocos de Dia, Semana, Mês ou Ano no passado.
* **Solução:** Centralizado no `GetDashboardDataUseCase`, que recalcula os intervalos de tempo (milissegundos) com base no deslocamento (`timeOffset`) selecionado pelo usuário. O fluxo se conecta ao banco de dados usando `flatMapLatest`, atualizando a tela de forma reativa à medida que o usuário navega pelo histórico.

### 2. Controle de Recomposições no Jetpack Compose
* **Contexto:** Manter a rolagem de listas fluida em componentes dinâmicos repetitivos.
* **Solução:** Uso explícito de chaves estáveis (`key`) nos escopos de loops do `LazyColumn` para forçar o Compose a pular a recomposição de cards cujos dados não sofreram alterações.

---

## 🏁 Status do Projeto

O **ForgeFocus** está em fase de **MVP (Minimum Viable Product)**. As regras fundamentais de persistência offline, lógica de foco e navegação estruturada estão implementadas e prontas para a adição de novas interfaces e funcionalidades.

---

## ⚙️ Como Rodar o Projeto

1. Certifique-se de utilizar o **Android Studio Ladybug** (ou superior).
2. Clone o repositório:
   ```bash
   git clone [https://github.com/seu-usuario/forgefocus-android.git](https://github.com/seu-usuario/forgefocus-android.git)