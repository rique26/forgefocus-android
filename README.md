# ⛏️ ForgeFocus — Produtividade Gamificada

O **ForgeFocus** é um aplicativo multiplataforma baseado na metodologia de blocos de foco (estilo Pomodoro). O usuário registra blocos de 30 minutos de foco para evoluir metas de longo prazo de forma gamificada: cada bloco registrado "minera" um pedaço de uma montanha ilustrada, que se revela pedra a pedra até ser totalmente concluída.

O projeto foi construído em Kotlin aproveitando as capacidades do **Kotlin Multiplatform (KMP)** e **Compose Multiplatform (CMP)**, aplicando os padrões recomendados pelo Google ([Guide to App Architecture](https://developer.android.com/topic/architecture)), Clean Architecture e desenvolvimento orientado a pacotes por funcionalidade (*Feature-Driven*).

---

## 📐 Estrutura do Projeto & Arquitetura

O projeto adota o fluxo unidirecional de dados (UDF) aliado ao padrão MVVM para manipulação de estados e intenções da interface de forma imutável, dentro do módulo compartilhado `:shared`.

A estrutura de código compartilha toda a lógica de negócios, banco de dados e componentes visuais entre as plataformas (Android e iOS), separando a infraestrutura global (`core`) do módulo específico da funcionalidade de metas (`features`).

### Responsabilidades das camadas em `:shared` (`commonMain`)

| Camada | Responsabilidade |
|---|---|
| **Data** | Gerencia a persistência local com Room KMP. As entidades do banco (`GoalEntity`, `ProgressLogEntity`) são isoladas e convertidas para objetos de domínio puramente em Kotlin através de mappers. |
| **Domain** | Isola as regras de negócio em casos de uso independentes (`BreakMountainBlockUseCase`, `GetDashboardDataUseCase`, `GetDailyProgressUseCase`), facilitando a escrita de testes unitários no `commonTest`. |
| **Presentation** | Consome estados imutáveis do `DashboardViewModel` via `StateFlow` e processa as ações do usuário mapeadas como eventos unificados (`MountainsEvent`), renderizando telas e componentes com Compose Multiplatform. |

### Destaque visual: shatter procedural da montanha

O card de progresso não usa um grid fixo de blocos sobre a imagem — a montanha (`mountain_illustration.png`) é dividida em células irregulares via um algoritmo simplificado de Voronoi (`buildShatterCells`), restrito à silhueta real da ilustração. Cada clique em "Quebrar!" recorta e anima um pedaço dessa silhueta, que voa e se empilha organizadamente em blocos estilizados do lado direito do card — mantendo proporção 1:1 entre bloco de progresso e clique, independentemente de quantos blocos a meta exigir no total.

---

## 🛠️ Stack Tecnológica

* **Multiplataforma:** Kotlin Multiplatform (KMP) & Compose Multiplatform (CMP)
* **UI:** Compose Multiplatform — paradigma declarativo, com componentes customizados desenhados via `Canvas`/`DrawScope` (ex.: `MountainReveal`, `MountainSnapshot`)
* **Navegação:** Navigation Compose Multiplatform / Type-Safe Navigation (navegação baseada em tipos seguros/objetos)
* **Injeção de Dependência:** Koin (DI focado em ecossistema KMP)
* **Banco de Dados Local:** Room KMP, com suporte multiplataforma e controle de conversores de tipo (`Converters`)
* **Assincronismo:** Kotlin Coroutines & Flow (uso de operadores como `flatMapLatest` e `combine`)

---

## 🏁 Status do Projeto

O ForgeFocus está em fase de **MVP (Minimum Viable Product)**. As regras fundamentais de persistência offline, lógica de foco, módulo compartilhado KMP e navegação estruturada estão implementadas e prontas para execução em alvos Android e iOS.

### Já implementado
- Criação e acompanhamento de metas (diárias ou por projeto)
- Registro de blocos de foco com persistência local (Room)
- Visualização de progresso macro (montanha) e histórico por período (dia/semana/mês/ano)
- Componente de shatter procedural com animação de queda e empilhamento

### Backlog
Itens já identificados para próximas iterações:

- [ ] Corrigir cálculo de blocos necessários por dia, com persistência correta no banco
- [ ] Revisar formatação de datas em pt-BR na camada de exibição
- [ ] Suportar múltiplas durações de bloco (5 a 60 min, não só 30 min fixos)
- [ ] Adicionar confirmação de exclusão de meta também no card do dashboard (já existe na tela de detalhe)
- [ ] Permitir navegação por períodos passados também no dashboard (hoje só existe na tela de detalhe)
- [ ] Suportar múltiplos estilos/skins de montanha
- [ ] Refatorar a camada de apresentação para MVI
- [ ] Autenticação com Google

---

## 📁 Estrutura de pastas

```
shared/src/commonMain/
├── composeResources/drawable/     # assets (ex: mountain_illustration.png)
└── kotlin/com/app/forgefocus/
    ├── core/                      # infraestrutura global (db, domain base, tema, DI)
    └── features/mountains/        # feature de metas e progresso
        ├── data/
        ├── domain/
        └── presentation/
            ├── components/
            ├── navigation/
            ├── screens/
            ├── util/
            └── viewmodel/
```