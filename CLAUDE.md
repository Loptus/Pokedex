# CLAUDE.md: Test tecnico Soluzione 1, app Pokédex (Kotlin + Compose)

> Istruzioni operative per Claude Code. Il progetto si costruisce "a quattro mani":
> Alessio Fallone (Senior Android Engineer, 13+ anni) guida le scelte e revisiona, Claude Code
> propone e implementa. Questo file è la fonte di verità su obiettivo, stack, architettura e
> vincoli. Leggerlo prima di scrivere qualsiasi codice.

## 0. Contesto: cos'è questo lavoro

È il test tecnico consegnato da Soluzione 1 dopo un colloquio andato bene (recruiter Marco).
Dopo la consegna, Alessio dovrà **discutere il codice di persona in sede**: ogni scelta va quindi
capita e difendibile, non "presa dalla libreria a caso". Meglio poco codice pulito e motivato che
molto codice non padroneggiato.

Traccia originale (sintesi fedele):

- App mobile che mostra una **lista di Pokémon** dalla PokeAPI pubblica (`https://pokeapi.co/docs/v2`),
  con **ricerca per nome o tipo**.
- **Paginazione** a massimo 20 elementi per pagina, con **caricamento automatico** della pagina
  successiva quando si arriva in fondo (infinite scroll).
- **Asincronia fatta bene** (coroutine).
- **Codice pulito** e **design pattern adeguati** (MVVM, Clean Architecture).
- Extra graditi (non obbligatori): **preferiti** salvabili e visualizzabili in una **pagina dedicata**
  dove si possono eliminare; seguire il **mockup** fornito e migliorarlo; **navigazione** con tab
  bar / navigation drawer / FAB o simili; **icona dell'app**.
- Consegna come **archivio ZIP**. Nome app e icona adattabili allo stile di Soluzione 1.
- Budget dichiarato: **massimo 6 ore complessive**. Valutano qualità del codice, chiarezza,
  performance e aderenza alle indicazioni.

Criteri di valutazione su cui puntare, in ordine: architettura pulita e scalabile, networking e
parsing corretti, UI funzionale e gradevole, best practice di piattaforma.

## 1. Come lavoriamo insieme (protocollo a quattro mani)

1. **Mostra il piano prima di agire** sui pezzi non banali (scelta libreria, struttura moduli,
   strategia ricerca/paginazione) e aspetta l'ok di Alessio.
2. **Commit piccoli e atomici**, un messaggio chiaro per ciascuno. La storia git deve raccontare
   il ragionamento: sarà utile in sede.
3. **Spiega le scelte mentre le fai**, in breve, così Alessio le può difendere. Segnala i
   trade-off ("uso X invece di Y perché..., il costo è...").
4. **Non over-ingegnerizzare per il budget di 6 ore.** Prima il must-have solido, poi gli extra.
   Se una cosa è bella ma costosa, proponila come stretch e lasciala decidere ad Alessio.
5. Quando un requisito è ambiguo, **chiedi**, non indovinare.

## 2. Stack tecnico (deciso)

- **Linguaggio:** Kotlin (ultima stabile), JDK 17.
- **UI:** Jetpack Compose + **Material 3**. Single Activity, niente Fragment.
- **Async:** Coroutine + **Flow**. `viewModelScope`, `StateFlow` per lo stato UI.
- **DI:** **Hilt**.
- **Networking:** **Retrofit** + **OkHttp** (logging interceptor + cache HTTP). Serializzazione con
  **kotlinx.serialization** (converter `retrofit2-kotlinx-serialization-converter`).
- **Paginazione:** **Paging 3** (`androidx.paging:paging-compose`).
- **Persistenza preferiti:** **Room** (+ Flow reattivo).
- **Immagini:** **Coil** (`coil-compose`), con crossfade e placeholder.
- **Navigazione:** **Navigation Compose** con **bottom navigation** a due tab.
- **Test:** JUnit4, **MockK**, **Turbine** (per i Flow), `kotlinx-coroutines-test`.
- **Build:** Gradle **Kotlin DSL** (`build.gradle.kts`) + **version catalog** (`libs.versions.toml`).

Regola generale: scegli sempre la soluzione idiomatica e recente ma **stabile**, niente alpha se
esiste una beta/stable equivalente. Se una versione dà problemi di compatibilità, segnalalo e
proponi il fallback invece di forzare.

## 3. Architettura

Clean Architecture a tre layer, MVVM nel presentation, **Unidirectional Data Flow**.

```
:app
 └─ com.soluzione1.pokedex
     ├─ di/                      # moduli Hilt
     ├─ core/                    # util, Result wrapper, dispatchers, network base
     ├─ data/
     │   ├─ remote/              # PokeApi (Retrofit), DTO, mapper DTO->domain
     │   ├─ local/               # Room: entity, DAO, database
     │   └─ repository/          # implementazioni dei repository
     ├─ domain/
     │   ├─ model/               # Pokemon, PokemonType, ... (modelli puliti, no annotazioni framework)
     │   ├─ repository/          # interfacce dei repository
     │   └─ usecase/             # use case (uno per azione: GetPokemonPagingUseCase, ToggleFavoriteUseCase, ...)
     └─ presentation/
         ├─ list/                # schermata lista + ricerca (ViewModel, UiState, screen, composables)
         ├─ favorites/           # schermata preferiti
         ├─ detail/              # (opzionale) dettaglio pokemon
         ├─ navigation/          # NavHost, rotte, bottom bar
         └─ theme/               # Material3 theme, colori, tipografia
```

Principi:

- **Dipendenze verso l'interno:** presentation dipende da domain, data dipende da domain, domain
  non dipende da nessuno. I DTO e le entity Room **non escono mai** dal layer data: si mappano in
  modelli di dominio.
- **UiState immutabile:** ogni schermata ha una `data class ...UiState` (loading, dati, errore,
  query, ecc.) esposta come `StateFlow`. Gli eventi utente entrano come funzioni/`onEvent` nel
  ViewModel. Niente logica nei composable oltre al render.
- **Gestione esiti:** wrapper `Result`/`sealed` per successo/errore, mai eccezioni nude che
  arrivano alla UI. Stati **loading / content / empty / error** gestiti esplicitamente in ogni
  schermata.
- **Dispatcher iniettati** (non `Dispatchers.IO` hardcoded) per testabilità.

### Use case: sempre, senza eccezioni

Regola ferma (decisa da Alessio): **ogni accesso ai dati passa da uno use case**, anche quando
sembra una read banale. La UI e i ViewModel **non chiamano mai un repository direttamente**, il
repository è raggiunto solo attraverso uno use case. Questo tiene la logica in un posto solo e
rende ogni operazione testabile in isolamento.

Convenzioni sugli use case:

- **Niente interfacce** per gli use case: sono classi concrete iniettate da Hilt. (Le interfacce
  restano solo sui repository, per poterli sostituire con fake nei test.)
- **Pattern `invoke`:** ogni use case espone `operator fun invoke(...)` (o `suspend operator fun`),
  così si chiama come una funzione: `getPokemonPaging()`, `toggleFavorite(pokemon)`.
- Un use case per azione, nome parlante con suffisso `UseCase`, un solo motivo per cambiare.
- I dispatcher, se servono, sono iniettati nello use case, non nel ViewModel.

## 4. PokeAPI: come funziona davvero (leggere, qui stanno le insidie)

La PokeAPI è REST e senza auth, ma **la lista non basta a disegnare la riga del mockup**. Punti chiave
verificati:

1. **Lista paginata:** `GET /api/v2/pokemon?limit=20&offset=0` restituisce
   `{ count, next, previous, results: [{ name, url }] }`. **Solo nome e url**, niente sprite, tipi o
   descrizione. `next`/`previous` danno la paginazione; `count` era 1351 al momento del check.
2. **Dettaglio pokemon:** `GET /api/v2/pokemon/{id|name}` per **sprite** (`sprites.front_default`
   e, meglio, `sprites.other."official-artwork".front_default`) e **tipi** (`types[].type.name`).
   Payload grande: deserializzare **solo i campi che servono** (con kotlinx.serialization
   `ignoreUnknownKeys = true`).
3. **Descrizione (flavor text):** sta in `GET /api/v2/pokemon-species/{id}`, campo
   `flavor_text_entries[]`: filtrare `language.name == "en"`, prendere la prima, **ripulire** i
   caratteri di controllo `\n`, `\f` e doppi spazi (l'API li contiene letteralmente).
4. **Ricerca per tipo:** `GET /api/v2/type/{name}` restituisce `pokemon[].pokemon.{name,url}`. I
   18 tipi sono un set noto e finito (normal, fire, water, grass, electric, ice, fighting, poison,
   ground, flying, psychic, bug, rock, ghost, dragon, dark, steel, fairy).
5. **Ricerca per nome:** la PokeAPI **non ha endpoint di ricerca fuzzy**. `GET /pokemon/{name}` fa
   solo match esatto. Strategia: al primo avvio scaricare **una volta** l'indice leggero completo
   (`GET /pokemon?limit=100000&offset=0`, solo name+url, poche decine di KB), tenerlo in memoria (o
   in Room), e filtrare i nomi **in locale** per substring; poi caricare i dettagli solo per i
   risultati (paginati).

### Conseguenza architetturale: N+1 e come domarlo

Ogni pagina di 20 pokémon richiede 1 chiamata lista + fino a 40 chiamate dettaglio/species. Da
gestire così:

- Caricare i dettagli della pagina **in parallelo controllato** con coroutine (`coroutineScope` +
  `async`/`awaitAll`, oppure `.asFlow().flatMapMerge(concurrency = N)`), non in sequenza.
- Abilitare la **cache HTTP di OkHttp** (la PokeAPI manda header cache-friendly): riduce di molto le
  chiamate ripetute e migliora la performance percepita, che loro valutano.
- La **descrizione (species)** non serve per far scorrere la lista: si può caricare **lazy** solo
  per gli item visibili, oppure spostarla nella schermata di dettaglio per alleggerire la lista.
  Decidere con Alessio: il mockup la mostra in lista, quindi va gestita, ma con criterio.

## 5. Feature e priorità (con budget indicativo sulle 6 ore)

> **I test non sono un extra, sono la base.** Sono ciò che certifica un livello Senior. Ogni pezzo
> che scriviamo, use case, mapper, ViewModel, repository, va corredato dal suo test nello stesso
> passo (approccio TDD dove ha senso). Una feature non è "fatta" finché non ha i suoi test verdi.
> Questo vale trasversalmente su tutti i punti qui sotto, non è una voce separata da spuntare alla
> fine. Se il budget stringe, si taglia una feature intera (con i suoi test), non i test di ciò che
> resta.

**Must-have (prima di tutto):**

1. Setup progetto, Gradle version catalog, Hilt, tema Material 3 (~30m).
2. Layer data: `PokeApi` Retrofit, DTO, mapper, repository lista+dettaglio (~60m).
3. Paginazione con Paging 3 (`PagingSource` offset/limit 20) + infinite scroll in Compose
   (`collectAsLazyPagingItems`, append automatico) con stati loading/error/retry (~60m).
4. Lista in Compose fedele al mockup (~40m).
5. Ricerca per nome e per tipo, con **debounce** sulla query nel ViewModel (~40m).

**Extra graditi (se resta tempo, in quest'ordine):**

6. Preferiti con Room: toggle dalla lista, schermata dedicata, eliminazione (swipe-to-dismiss o
   pulsante), osservati via Flow.
7. Bottom navigation a due tab: "Pokédex" e "Preferiti".
8. Schermata di dettaglio al tap (artwork grande, tipi, descrizione, stats base, toggle preferito).
9. Icona app adattiva in stile Soluzione 1 e nome app.

Ogni voce, must-have o extra, include i propri test (vedi riquadro sopra e sezione 7). Se il tempo
stringe, **tagliare gli extra dal fondo con tutto il loro codice**, non alleggerire i test di ciò
che resta.

## 6. Specifica UI (dal mockup + margine di miglioramento)

Il mockup fornito ("PokemonBox") è lo stile di riferimento: pulito, chiaro, iOS-like. Da replicare
nello spirito, poi migliorare e ribrandizzare Soluzione 1.

- **Header:** titolo con due pesi di font (es. "Pokémon" regular + "Box"/"S1" bold).
- **Barra di ricerca:** campo con icona lente e placeholder "Cerca per nome o tipo", sotto l'header,
  sempre visibile.
- **Riga lista:** thumbnail sprite a sinistra; a destra nome in grassetto, **chip dei tipi**
  (pill arrotondate, un colore per tipo è un plus gradevole), e testo descrizione su 1-2 righe con
  ellissi. Divider sottile tra le righe.
- **Stati:** skeleton/placeholder durante il load, empty state per ricerca senza risultati, error
  state con pulsante "Riprova".
- **Preferiti:** icona cuore/stella sulla riga o nel dettaglio; nella schermata preferiti,
  eliminazione con conferma leggera.
- **Naming/branding:** adattare nome app e colore primario allo stile di Soluzione 1 (accent color
  coerente). Va bene un nome tipo "S1 Pokédex". Confermare con Alessio.
- **Accessibilità di base:** `contentDescription` sulle immagini, target touch adeguati, testo
  scalabile.

## 7. Testing (la base che certifica il livello Senior)

I test non sono un contorno: sono il segnale di serietà su cui Alessio vuole essere valutato. **Tutto
ciò che scriviamo va corredato da test**, scritti insieme al codice, non dopo. Una PR/commit senza
i test della logica che introduce è incompleto.

Cosa coprire, layer per layer:

- **Use case (tutti):** sono la logica dell'app, quindi vanno testati tutti, con repository fake e
  `kotlinx-coroutines-test`. Casi di successo, errore ed edge (lista vuota, query che non matcha).
- **ViewModel:** logica di ricerca (debounce, switch nome vs tipo, empty), transizioni di
  `UiState` verificate con Turbine sugli StateFlow.
- **Mapper DTO -> domain:** pulizia flavor text (rimozione `\n`/`\f`), estrazione tipi, scelta
  sprite, gestione campi mancanti.
- **Repository:** comportamento con sorgente remota fake e, per i preferiti, Room in-memory
  (`Room.inMemoryDatabaseBuilder`), toggle add/remove e osservazione via Flow.
- **PagingSource:** caricamento pagina, chiave offset, fine lista.

Strumenti: JUnit4, MockK, Turbine, `kotlinx-coroutines-test`, Room in-memory per il DAO. Puntare a
test **veloci e deterministici** (niente rete reale: usare fake/mock). Non serve inseguire una
percentuale di coverage, serve che **ogni comportamento significativo abbia il suo test**.

## 8. Consegna e vincoli

- **Repository git** con storia pulita e leggibile (i commit raccontano il percorso).
- **README.md** in italiano con: come buildare/eseguire, panoramica architettura (diagramma a
  parole o ASCII), scelte tecniche e trade-off, cosa è stato tagliato per il budget e **"cosa farei
  con più tempo"** (RemoteMediator + cache offline completa, più test, dettaglio ricco, ecc.).
- **`.gitignore`** Android standard: escludere `build/`, `.gradle/`, `.idea/`, `local.properties`,
  `*.iml`.
- **Archivio ZIP** finale che **non** includa `build/`, `.gradle/`, `.idea/` (peso e pulizia).
  Includere il wrapper Gradle (`gradlew`, `gradle/wrapper/`) così builda ovunque.
- **Nessun segreto** nel repo (la PokeAPI non richiede chiavi, ma non committare `local.properties`).

## 9. Convenzioni di codice

- **Identificatori e commenti in inglese**, coerenti con lo standard.
- Kotlin official code style. Considerare ktlint/detekt se non costa tempo, altrimenti coerenza
  manuale.
- **Niente stringhe hardcoded** nella UI: tutto in `strings.xml` (occhio agli accenti italiani
  corretti: è, à, ì, ò, ù, é).
- **Niente `!!`**, gestione esplicita dei nullable. Niente logica di business nei composable.
- Funzioni piccole e nominate con chiarezza, un motivo per cambiare ciascuna.
- Dependency injection ovunque, niente singleton manuali o `object` con stato.

## 10. Nota di stile per la documentazione (README, commit, commenti in italiano)

Questi valgono per i testi che finiscono sotto gli occhi del recruiter:

- **Mai em dash** ("—"): Alessio lo considera un segnale di scrittura AI. Usare virgole, due punti,
  parentesi o spezzare la frase. (Gli en dash negli intervalli di date, "2008–2012", sono ok.)
- **Accenti italiani sempre corretti.**
- Tono: conciso, concreto, professionale. Niente enfasi gonfiata.

---

### Checklist di partenza per Claude Code

1. Leggi questo file per intero e il mockup fornito.
2. Proponi ad Alessio la struttura moduli e il version catalog, aspetta l'ok.
3. Parti dai must-have nell'ordine della sezione 5, con commit piccoli, ogni pezzo con i suoi test.
4. A ogni scelta non banale, una riga di spiegazione del perché.
5. Chiudi con README, `.gitignore` e istruzioni per generare lo ZIP pulito.
