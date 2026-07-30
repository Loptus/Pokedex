# CLAUDE.md: Pokédex (Kotlin + Compose)

Istruzioni operative per Claude Code. Questo file è la fonte di verità su obiettivo, stack,
architettura, regole di lavoro e convenzioni. Leggerlo prima di scrivere codice.

## 1. Cos'è il progetto

App Android nativa che mostra una lista di Pokémon presi dalla PokeAPI pubblica
(`https://pokeapi.co/docs/v2`).

Requisiti funzionali:

- Lista dei Pokémon con thumbnail, nome, tipi e una breve descrizione.
- **Paginazione a 20 elementi**, con caricamento automatico della pagina successiva quando si
  arriva in fondo (infinite scroll).
- **Ricerca per nome** e **filtro per tipo**.
- **Preferiti** salvabili, con una pagina dedicata da cui si possono eliminare.
- Navigazione fra le due pagine con bottom navigation.

Qualità su cui il progetto viene misurato, in ordine: architettura pulita e scalabile, networking e
parsing corretti, UI funzionale e gradevole, best practice di piattaforma.

## 2. Come si lavora

Queste regole valgono sempre e hanno la precedenza sulla voglia di andare veloce.

1. **Uno step alla volta.** Si esegue solo lo step corrente, si ferma, si rivede insieme prima di
   passare al successivo.
2. **Niente che non serva allo step corrente.** Nessun parametro, nessuna firma, nessun file, nessuna
   stringa e nessuna icona messi lì per uno step futuro, nemmeno sapendo già che quel file andrà
   riaperto. Riaprirlo è il costo giusto da pagare. In particolare: **mai UI per funzionalità non
   ancora implementate**, perché il finale peggiore possibile è un progetto pieno di riferimenti a
   una feature che non è mai arrivata.
3. **Ogni step chiude qualcosa di intero**, in verticale, invece di lasciare impalcature a metà.
4. **Approccio top-down.** Si parte dalla UI e si scende verso i dati: il mockup dice quali campi
   esistono davvero, quindi partendo dalla schermata il modello nasce minimo e non si costruisce
   data layer che nessuno usa. Unica eccezione: il **modello di dominio viene prima della UI**,
   perché è il contratto fra i layer.
5. **Mostra il piano prima di agire** sulle scelte non banali (libreria, struttura, strategia di
   ricerca o di paginazione) e aspetta l'ok.
6. **Spiega le scelte mentre le fai**, in breve, con il trade-off esplicito ("uso X invece di Y
   perché..., il costo è...").
7. **Quando un requisito è ambiguo, chiedi**, non indovinare.
8. **Non over-ingegnerizzare.** Prima il must-have solido, poi gli extra. Se una cosa è bella ma
   costosa, proponila e lascia decidere.
9. **Riporta gli esiti fedelmente.** Se un test è rosso si dice, con l'output. Se un passaggio è
   stato saltato si dice. Mai dichiarare verificato ciò che non è stato eseguito.

### Git

- **Si lavora solo su `main`**, niente branch per feature: nessuna review, nessuna CI, nessun lavoro
  in parallelo da isolare, e la storia deve farsi leggere in ordine.
- **Nessun commit di iniziativa.** I commit si fanno solo quando vengono richiesti.
- **Messaggi di commit brevi e in inglese**, all'imperativo (`Add Pokemon list screen`), senza firme
  né co-autori generati.
- Commit piccoli e atomici: la storia git deve raccontare il ragionamento.

## 3. Stack tecnico

- **Kotlin** (ultima stabile), JDK 17 come target, toolchain 21.
- **UI:** Jetpack Compose + **Material 3**. Single Activity, niente Fragment.
- **Async:** Coroutine + Flow. `viewModelScope`, `StateFlow` per lo stato UI.
- **DI:** **Hilt** con KSP.
- **Networking:** **Retrofit** + **OkHttp** (logging interceptor in debug, cache HTTP su disco).
- **Serializzazione:** **Gson** con `converter-gson`. Conseguenza importante: Gson non garantisce la
  non-nullità a runtime, quindi i DTO vogliono **default espliciti su ogni campo** e i test dei
  mapper fanno da rete di sicurezza al posto del controllo del compilatore.
- **Paginazione:** **Paging 3** (`paging-runtime`, `paging-compose`, `paging-testing`).
- **Persistenza preferiti:** **Room** (+ Flow reattivo).
- **Immagini:** **Coil 3** (`coil-compose` + `coil-network-okhttp`, così condivide client e cache
  HTTP con le chiamate API).
- **Navigazione:** Navigation Compose con bottom navigation a due tab.
- **Test:** JUnit4, MockK, Turbine, `kotlinx-coroutines-test`, MockWebServer, Robolectric,
  `paging-testing`.
- **Build:** Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).

Regole sulle dipendenze:

- Sempre la soluzione idiomatica e recente ma **stabile**: niente alpha se esiste una beta o una
  stable equivalente. Le versioni si verificano sui repository, non si scrivono a memoria.
- **Non introdurre version ref duplicati**: se una libreria appartiene a una famiglia già presente
  (OkHttp, Lifecycle), usa il ref esistente, così gli aggiornamenti non lasciano indietro un pezzo.
- Evitare artefatti deprecati o congelati. Per esempio `material-icons-core` è fermo a una versione
  vecchia di Compose: le poche icone necessarie si mettono come vector in `res/drawable`.
- Il linting automatico (ktlint, detekt) al momento **non** è attivo: la coerenza di stile si tiene a
  mano seguendo la sezione 8.

## 4. Architettura

Clean Architecture a tre layer, MVVM nel presentation, Unidirectional Data Flow. I tre layer sono
**tre moduli Gradle**, non tre package: così il confine lo applica il compilatore invece della buona
volontà di chi scrive.

```
:domain   kotlin-jvm                domain/model, domain/repository (interfacce), domain/usecase, core/
:data     com.android.library       data/remote (PokeApi, DTO, mapper), data/repository, di/
:app      com.android.application   presentation/, MainActivity, theme, PokedexApplication
```

Regole dei moduli:

- **`:domain` è Kotlin JVM puro.** Non ha l'SDK Android sul classpath, quindi un `Context`, un
  `Color` o un `@Composable` nel dominio sono un errore di compilazione, non una svista da cogliere
  in review. Dipende solo da `kotlin-stdlib`, `javax.inject` e `paging-common`, che è la metà pura
  di Paging.
- **`:data` dipende da `:domain` con `api`**, perché ne riespone modelli e interfacce a chi lo usa.
- **`:app` dipende da `:domain` con `implementation` e da `:data` con `runtimeOnly`**: la app non
  importa una singola classe dal layer dati, le serve solo a runtime perché Hilt trovi i binding.
  Se un giorno `runtimeOnly` non bastasse più, è il segnale che qualcosa sta sconfinando.
- **Ogni modulo dichiara le proprie dipendenze:** Compose e Coil solo in `:app`, Retrofit, OkHttp,
  Gson e Room solo in `:data`.
- **Anche i test rispettano i confini.** Un test in `:app` che ha bisogno di una classe di `:data`
  non è un problema di visibilità, è il segnale che sta testando il layer sbagliato.
- **`core/` sta in `:domain`.** `AppResult` compare nella firma degli use case, quindi lo leggono sia
  `:data` sia `:app`: metterlo nel modulo da cui dipendono già entrambi evita un quarto modulo che
  esisterebbe per un file. I qualificatori dei dispatcher restano in `:data`, che è l'unico a usarli.

Principi:

- **Dipendenze verso l'interno:** presentation dipende da domain, data dipende da domain, domain non
  dipende da nessuno. DTO ed entity Room **non escono mai** dal layer data.
- **Il dominio resta puro:** niente colori, niente etichette, niente `Context`. La capitalizzazione
  di un nome e il colore di un tipo sono presentation.
- **UiState immutabile:** ogni schermata ha una `data class ...UiState` esposta come `StateFlow`.
  Gli eventi utente entrano come funzioni nel ViewModel. **Niente logica nei composable** oltre al
  render, e niente costruzione di sorgenti dati dentro un `remember`.
- **Route stateful, Screen stateless:** `XxxRoute` prende il ViewModel, raccoglie lo stato e passa
  valori semplici a `XxxScreen`, che resta previewabile e testabile da sola.
- **Gestione esiti:** wrapper sealed per successo ed errore, mai eccezioni nude fino alla UI. Stati
  loading / content / empty / error espliciti in ogni schermata, ma **introdotti nel momento in cui
  qualcosa può davvero caricare o fallire**, non prima.
- **Dispatcher iniettati** (`@IoDispatcher` e compagni), mai `Dispatchers.IO` scritto a mano.

### Use case: sempre, senza eccezioni

**Ogni accesso ai dati passa da uno use case**, anche quando sembra una lettura banale. UI e
ViewModel **non chiamano mai un repository direttamente**.

- **Niente interfacce** per gli use case: classi concrete iniettate da Hilt. Le interfacce restano
  sui repository, per poterli sostituire con dei fake nei test.
- **Pattern `invoke`:** `operator fun invoke(...)` o `suspend operator fun`, così si chiamano come
  funzioni.
- Un use case per azione, nome parlante con suffisso `UseCase`, un solo motivo per cambiare.
- I dispatcher, se servono, si iniettano nello use case, non nel ViewModel.

### Organizzazione dei file

- **Package per feature, non per tipo tecnico.** `presentation/list/` contiene schermata, route,
  ViewModel e UiState di quella feature. Un package `uistate/` o `viewmodels/` a livello di progetto
  spezzerebbe in due la lettura di una schermata: quello che cambia insieme sta insieme.
- **Una dichiarazione principale per file**, con il nome del file uguale a quello della classe.
  `PokemonListUiState` sta in `PokemonListUiState.kt`, non in fondo al file del ViewModel.
- Se una cartella di feature cresce, si divide per ruolo al suo interno (`list/components/`), non per
  tipo tecnico a livello di progetto.

## 5. PokeAPI: come funziona davvero

La API è REST e senza auth, ma **la lista non basta a disegnare una riga**. Punti verificati:

1. **Indice dei nomi:** `GET /api/v2/pokemon?limit=100000` restituisce
   `{ count, next, previous, results: [{ name, url }] }`. **Solo nome e url**, niente sprite, tipi o
   descrizione.
2. **Dettaglio:** si raggiunge seguendo l'`url` dell'indice. Da lì lo sprite
   (`sprites.other."official-artwork".front_default`, con fallback su `sprites.front_default`), i
   tipi (`types[].type.name`) e l'oggetto `species`. Payload grande: deserializzare **solo i campi
   che servono**.
3. **Descrizione (flavor text):** si raggiunge seguendo `species.url` **del dettaglio**, campo
   `flavor_text_entries[]`. Filtrare `language.name == "en"`, prendere la prima e **ripulirla** dai
   caratteri di controllo `\n`, `\f` e dai doppi spazi, che l'API contiene letteralmente.

### Gli url si seguono, non si costruiscono

Regola non negoziabile, imparata rompendo l'app. La PokeAPI è una rete di link e **i suoi id non
sono allineati**: dall'id 10001 in poi le voci sono forme alternative, e la loro species sta sotto un
id completamente diverso e più basso (`/pokemon/10001/` è deoxys-attack, la sua species è
`/pokemon-species/386/`, mentre `/pokemon-species/10001/` è 404).

Costruire un indirizzo da un id funziona per le prime 1025 voci e poi fallisce in silenzio su tutta
la coda della lista. Quindi: l'url del dettaglio si prende dall'indice, quello della species dal
dettaglio, e in Retrofit si usa `@Url`. L'id serve solo come chiave di lista, mai per comporre un
indirizzo.

Costo accettato: dettaglio e species diventano **sequenziali**, perché il secondo indirizzo sta
dentro la prima risposta. Un round trip in più è il prezzo di non indovinare.
4. **Ricerca per tipo:** `GET /api/v2/type/{name}` restituisce `pokemon[].pokemon.{name,url}`.
5. **Ricerca per nome:** la API **non ha ricerca fuzzy**, `GET /pokemon/{name}` fa solo match esatto,
   quindi il filtro per substring si fa in locale sull'indice.

### Dove sta davvero il costo (misurato, gzip)

| Chiamata | Byte |
| --- | --- |
| Indice completo, 1351 voci | 11.714 |
| Una pagina di soli nomi, 20 voci | 308 |
| Un dettaglio | 6.951 |
| Una species | 5.066 |
| **Una riga completa (dettaglio + species)** | **12.017** |

Due conseguenze architetturali, entrambe da questi numeri.

**L'indice si scarica intero una volta sola** e la lista si pagina in memoria. Paginare i nomi via
API ottimizzerebbe l'unica cosa che non costa niente (308 byte), e costringerebbe la prima ricerca ad
aspettare il download dell'indice. Con l'indice già in memoria, navigazione e ricerca sono **lo
stesso percorso di codice**: query vuota significa "tutti i nomi".

**Il contenuto di una riga si carica per riga, non per pagina.** Una pagina emette solo
`PokemonRef(id, name)` e non costa nessuna richiesta, quindi la lista appare subito; ogni riga
prende i suoi 12 KB quando arriva a schermo. Caricarli per pagina significherebbe 240 KB per venti
righe di cui l'utente ne vede quattro o cinque.

### Il problema N+1 e come domarlo

- **Niente caricamento per pagina.** L'unità di caricamento è la riga visibile: `PokemonRef` per
  paginare, `Pokemon` per la riga caricata.
- **La cancellazione arriva da `LaunchedEffect`.** Il caricamento della riga gira nello scope della
  composizione, quindi quando la riga esce dallo schermo Compose lo cancella e Retrofit cancella le
  richieste. Scorrere veloce non paga le righe superate.
- **Cache in memoria nel ViewModel**, un `StateFlow` per id: fa da cache (sopravvive
  all'invalidazione del Paging e alla rotazione) e dà la granularità giusta, perché una riga che
  arriva ricompone solo se stessa. I **fallimenti non si mettono in cache**, così una riga che torna
  visibile riprova senza bisogno di UI di errore per riga.
- **Cache HTTP di OkHttp** attiva: la API manda header cache-friendly e questo abbatte le chiamate
  ripetute.

### Paginazione

- `PagingConfig` con `pageSize = 20` **e `initialLoadSize = 20` esplicito**: il default di Paging è
  tre volte la pagina, che caricherebbe 60 elementi al primo colpo e violerebbe il requisito.
- `cachedIn(viewModelScope)` sul flusso, così una rotazione dello schermo non ricarica la lista.
- La chiave di pagina è l'**offset** dentro l'indice in memoria.

## 6. UI

Il mockup di riferimento è pulito e iOS-like. Da replicare nello spirito e migliorare dove serve.

- **Header:** titolo con due pesi di font ("Poké" regular + "dex" bold).
- **Barra di ricerca:** campo con icona lente, sotto l'header, sempre visibile.
- **Filtro per tipo:** riga di chip a selezione multipla. È uno **scostamento voluto** dal mockup,
  che ha un campo unico per nome e tipo: chip separati mostrano quali tipi esistono invece di farli
  indovinare, permettono di combinare i filtri e tengono la query non ambigua per i layer sotto.
- **Riga della lista:** thumbnail a sinistra; a destra nome in grassetto, chip dei tipi colorati,
  descrizione su 2 righe con ellissi. Divider sottile fra le righe.
- **Stati:** skeleton durante il caricamento, empty state per la ricerca senza risultati, error state
  con "Riprova" (a schermo intero se fallisce la prima pagina, in coda alla lista se fallisce una
  successiva).
- **Tema:** Material 3, **dynamic color disattivato**: l'app ha una sua identità, i colori dei tipi
  sono già tutto il colore che lo schermo regge, e il dynamic color renderebbe incoerente ogni
  screenshot.
- **Colore dei chip:** sfondo del colore del tipo, testo bianco o nero scelto dalla luminanza dello
  sfondo. La soglia corretta è **0.179**, non 0.5: viene dalla formula di contrasto WCAG, dove nero e
  bianco pareggiano a `(L + 0.05) / 0.05 = 1.05 / (L + 0.05)`. Con quel pivot ogni colore è garantito
  sopra 4.5:1.
- **Accessibilità:** `contentDescription` sulle immagini e sui pulsanti icona, target touch adeguati,
  testo scalabile.

### Stringhe e lingue

- **Nessuna stringa hardcoded** nella UI.
- `values/strings.xml` in **inglese** come default, `values-it/strings.xml` in **italiano**. Accenti
  italiani sempre corretti.
- **I valori che arrivano dalla API non si traducono** (i nomi dei tipi, per esempio): sono dati, non
  testo di interfaccia. Tradurli vorrebbe dire mantenere a mano una tabella per lingua e
  disallinearsi dal vocabolario che la API usa anche per la ricerca. Si mostrano capitalizzando.

## 7. Test

I test non sono un contorno: sono ciò che rende difendibile il codice. Si scrivono **insieme** al
codice, non dopo.

Cosa coprire:

- **Use case:** tutti, con repository fake e `kotlinx-coroutines-test`. Successo, errore e casi
  limite (lista vuota, query che non matcha, intersezione vuota).
- **Mapper DTO -> dominio:** pulizia del flavor text, scelta dello sprite con i fallback, parsing dei
  tipi, campi mancanti o null.
- **Networking e parsing:** un paio di test con MockWebServer su fixture JSON reali accorciate.
- **PagingSource:** prima pagina, chiave offset, ultima pagina corta, sorgente vuota, errore.
- **Flusso paginato:** con `paging-testing` (`asSnapshot`, `scrollTo`) per verificare che si carichi
  una pagina alla volta e che l'append si fermi a fine lista.
- **ViewModel:** debounce della ricerca, cambio dei filtri, transizioni di `UiState` con Turbine.
- **Repository dei preferiti:** Room in-memory, toggle add e remove, osservazione via Flow.

Regole:

- **Non scrivere test tautologici.** Una data class senza comportamento non ha bisogno di un test:
  verificherebbe il compilatore, non il codice. Se un pezzo non ha comportamento degno di test, va
  detto invece di riempire il progetto di test finti.
- **Preferire invarianti che si rompono in silenzio.** Un test che verifica il contrasto su tutti e
  18 i tipi, o che ogni elemento mostrato abbia la sua descrizione, vale più di dieci assert ovvi.
- Test **veloci e deterministici**: niente rete reale, si usano fake e MockWebServer.
- Non serve inseguire una percentuale di coverage, serve che ogni comportamento significativo abbia
  il suo test.

## 8. Convenzioni di codice

- **Identificatori e commenti in inglese.** Kotlin official code style.
- **Trailing comma sempre**, su dichiarazioni e chiamate multi-riga: è raccomandata dalle convenzioni
  ufficiali Kotlin e fa sì che aggiungere un parametro tocchi una riga sola nel diff. Applicata al
  95% è peggio che non applicata, perché sembra distrazione invece che scelta.
- **Prima il pubblico, poi il privato.** In un file si leggono per prime le dichiarazioni pubbliche,
  cioè quello che interessa a chi le usa, e sotto i dettagli di implementazione. Un helper privato
  infilato in mezzo a due funzioni pubbliche costringe a saltarlo per capire l'API. Unica eccezione:
  le costanti private di file stanno in cima, insieme, perché servono da premessa e non da dettaglio.
- **Niente `!!`**, gestione esplicita dei nullable.
- **Niente logica di business nei composable.**
- Funzioni piccole, nominate con chiarezza, un motivo per cambiare ciascuna.
- Dependency injection ovunque, niente singleton manuali o `object` con stato.
- I commenti spiegano **perché**, non cosa: le decisioni non ovvie (una soglia, un default
  sovrascritto, un fallback) vanno annotate sul posto.

## 9. Build e verifica

Non esiste un JDK sul PATH: Gradle si lancia con la JBR di Android Studio.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug testDebugUnitTest :domain:test
```

`:domain` è un modulo JVM, quindi i suoi test stanno sotto `test` e non sotto `testDebugUnitTest`:
senza `:domain:test` esplicito quei test non vengono eseguiti e la build resta verde per finta.

- Dopo ogni step: build e test verdi prima di dichiarare finito.
- I warning del compilatore si trattano come errori da sistemare, non da ignorare (per esempio una
  API deprecata che ha cambiato package).
- **Limite noto:** le `@Preview` di Compose **non si renderizzano da riga di comando**, servono l'IDE
  o una libreria di screenshot test. Il controllo visivo e la fedeltà al mockup vanno verificati a
  mano in Android Studio: non dichiararli verificati.

## 10. Consegna

- **README.md** in italiano con: come buildare ed eseguire, panoramica dell'architettura, scelte
  tecniche e trade-off, cosa è stato tagliato e cosa si farebbe con più tempo.
- **`.gitignore`** Android standard: `build/`, `.gradle/`, `.idea/`, `local.properties`, `*.iml`,
  `.DS_Store`.
- **Nessun segreto nel repo.** La PokeAPI non richiede chiavi, ma `local.properties` non si committa.
- Niente testo protetto da copyright nel repo: i dati di esempio si scrivono, non si copiano dalle
  fonti originali. I contenuti veri arrivano dalla API a runtime.
- A fine progetto: **rimuovere dal version catalog le dipendenze non utilizzate**.

## 11. Stile della documentazione in italiano

Vale per README, commit e commenti in italiano:

- **Mai em dash** ("—"). Usare virgole, due punti, parentesi, o spezzare la frase. Gli en dash negli
  intervalli di date ("2008–2012") vanno bene.
- **Accenti italiani sempre corretti** (è, à, ì, ò, ù, é).
- Tono conciso, concreto, professionale. Niente enfasi gonfiata.
