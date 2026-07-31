# Pokédex

App Android nativa che elenca i Pokémon della [PokeAPI](https://pokeapi.co/docs/v2), con ricerca per
nome, filtro per tipo e preferiti salvati sul dispositivo.

## Come si builda e si esegue

Serve Android Studio, o in alternativa un JDK 17. Il progetto non ha chiavi né configurazione locale:
si clona e si compila.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

L'APK esce in `app/build/outputs/apk/debug/`. Da Android Studio basta aprire la cartella e premere
Run: l'unico permesso richiesto è `INTERNET`.

I test:

```bash
./gradlew :app:testDebugUnitTest --rerun :data:testDebugUnitTest --rerun :domain:test --rerun
```

I `--rerun` non sono un vezzo: senza, Gradle riusa il risultato in cache e restituisce verde senza
eseguire niente. `:domain` è un modulo JVM, quindi i suoi test stanno sotto `test` e non sotto
`testDebugUnitTest`.

Versioni: Kotlin 2.4.10, AGP 9.2.1, Gradle 9.6.1, `minSdk` 26, `compileSdk` e `targetSdk` 37.

## Architettura

Clean Architecture su tre layer, MVVM nel presentation, flusso di dati unidirezionale. I tre layer
sono **tre moduli Gradle**, non tre package, così il confine lo applica il compilatore invece della
buona volontà di chi scrive.

```
:domain   Kotlin JVM puro       modelli, interfacce dei repository, use case, AppResult
:data     Android library       Retrofit, OkHttp, Gson, Room, mapper, implementazioni
:app      Android application   Compose, ViewModel, navigazione, tema
```

- `:domain` non ha l'SDK Android sul classpath, quindi un `Context` o un `@Composable` nel dominio
  sono un errore di compilazione, non una svista da cogliere in review.
- `:app` dipende da `:domain` con `implementation` e da `:data` con **`runtimeOnly`**: la app non
  importa una sola classe dal layer dati, le serve solo a runtime perché Hilt trovi i binding.
- DTO ed entity Room non escono mai dal layer dati.

Ogni accesso ai dati passa da uno use case, anche quando è una lettura banale: ViewModel e UI non
chiamano mai un repository. La `UiState` di una schermata contiene solo stato di interfaccia (la
query digitata, i tipi selezionati), mentre i dati viaggiano su un flusso a parte. Una schermata
senza stato di interfaccia proprio, come i preferiti, non ha una `UiState` affatto.

Dipendenze principali: Compose con Material 3, Hilt con KSP, Retrofit con Gson, Paging 3, Room,
Coil 3, Navigation Compose.

## Le scelte che contano, e cosa costano

### L'indice si scarica intero una volta, la lista si pagina in memoria

Misurato sulla API reale, gzip: l'intero indice dei nomi (1351 voci) sono 11,7 KB, mentre le due
richieste che riempiono **una sola riga** sono 12 KB. Paginare i nomi via API avrebbe ottimizzato
l'unica cosa che non costa niente, e avrebbe costretto la prima ricerca ad aspettare.

Con l'indice in memoria, navigazione e ricerca diventano lo stesso percorso di codice: query vuota
significa "tutti i nomi". La API non ha ricerca fuzzy (`GET /pokemon/{name}` fa match esatto), quindi
il filtro per sottostringa in locale non è un'ottimizzazione, è l'unico modo di cercare.

Costo accettato: il primo avvio scarica 11,7 KB prima di poter mostrare qualsiasi cosa.

### Il contenuto si carica per riga visibile, non per pagina

Una pagina di 20 elementi emette solo puntatori e **non costa nessuna richiesta**, quindi la lista
appare subito. Ogni riga chiede i suoi 12 KB quando arriva a schermo, dentro un `LaunchedEffect`:
quando la riga esce, Compose lo cancella e Retrofit cancella le richieste. Scorrere veloce non paga
le righe superate.

Caricare per pagina avrebbe voluto dire 240 KB per venti righe di cui l'utente ne vede quattro.

Il contenuto già arrivato resta in una cache in memoria (un flusso per id) che sopravvive alla
rotazione e all'invalidazione del Paging. I fallimenti non si mettono in cache: una riga che torna
visibile riprova da sola, e non serve una UI di errore per riga.

### Gli url si seguono, non si costruiscono

Regola imparata rompendo l'app. La PokeAPI è una rete di link e i suoi id **non sono allineati**:
dall'id 10001 in poi le voci sono forme alternative, e la loro species sta sotto un id completamente
diverso e più basso (`/pokemon/10001/` è deoxys-attack, la sua species è `/pokemon-species/386/`,
mentre `/pokemon-species/10001/` è 404).

Costruire un indirizzo da un id funziona per le prime 1025 voci e poi fallisce in silenzio su tutta
la coda della lista. Quindi l'url del dettaglio si prende dall'indice, quello della species dal
dettaglio, e in Retrofit si usa `@Url`. L'id serve solo come chiave di lista.

Costo accettato: dettaglio e species sono **sequenziali**, perché il secondo indirizzo sta dentro la
prima risposta. Un round trip in più è il prezzo di non indovinare.

### Ricerca e filtri

La ricerca per nome è debounced a 300 ms, il toggle di un chip no: digitare arriva una lettera alla
volta, toccare un chip è un'azione singola e deliberata. Svuotare il campo salta l'attesa.

I tipi selezionati stanno in **unione** fra loro e in **AND** con il nome. Un filtro che con Fuoco e
Acqua insieme restituisce sempre zero sembra rotto, non vuoto.

I chip sono uno scostamento voluto dal mockup, che ha un campo unico per nome e tipo: mostrare i tipi
esistenti evita di farli indovinare e tiene la query non ambigua per i layer sotto.

### Preferiti

Room salva il **puntatore** (id, nome, url del dettaglio), non una copia di immagine, tipi e
descrizione. Così la pagina dei preferiti riusa la riga della lista senza modifiche e la copia non
invecchia mai. Il prezzo è che senza rete i preferiti mostrano solo i nomi e i placeholder.

L'ordine è quello del Pokédex, non quello di inserimento: la pagina non si rimescola quando togli e
rimetti un preferito, e la tabella non ha bisogno di una colonna con la data.

Il cuore compare dal primo frame, anche mentre la riga carica, perché quello che si salva è il
puntatore ed è già lì. Sulla pagina dedicata il cuore può solo rimuovere.

Chi è preferito lo decide il ViewModel: il flusso paginato emette elementi con il flag già risolto, e
il composable stampa un booleano invece di cercare un id dentro un insieme.

### UI

Material 3 con **dynamic color disattivato**: i colori dei tipi sono già tutto il colore che lo
schermo regge, e il dynamic color renderebbe incoerente ogni screenshot.

Il testo dei chip passa da bianco a nero sopra una luminanza di **0,179**, non 0,5. La soglia viene
dalla formula di contrasto WCAG, dove nero e bianco pareggiano a
`(L + 0,05) / 0,05 = 1,05 / (L + 0,05)`. Con quel pivot ogni tipo è garantito sopra 4,5:1 senza
tarare diciotto coppie a mano.

Le stringhe di interfaccia sono in inglese e in italiano. I valori che arrivano dalla API (i nomi dei
tipi) **non si traducono**: sono dati, non testo di interfaccia, e tradurli vorrebbe dire mantenere
una tabella per lingua e disallinearsi dal vocabolario che la API usa anche per la ricerca. Si
mostrano capitalizzati.

Le icone sono vector in `res/drawable`: `material-icons-core` è fermo a una versione vecchia di
Compose, e per cinque icone non vale una dipendenza congelata.

### Dettagli minori con un motivo dietro

- **DTO con default espliciti su ogni campo.** Gson costruisce per reflection e scrive
  tranquillamente un null dentro un campo che Kotlin dichiara non nullabile: i mapper sono l'unico
  posto che decide cosa significa un valore mancante, e i loro test fanno da rete al posto del
  compilatore.
- **`initialLoadSize` esplicito.** Il default di Paging è tre volte la pagina, che al primo colpo
  caricherebbe 60 elementi invece dei 20 richiesti.
- **Cache HTTP su disco di OkHttp**, 10 MB: la API manda header cache-friendly, quindi una riga già
  vista non ripaga la rete. Le immagini hanno la propria cache, quella di Coil.
- **Rotte di navigazione come stringhe** in un enum, non type safe: con due destinazioni e nessun
  argomento, il type safe costerebbe il plugin di serializzazione per proteggere da un errore che qui
  non si può fare.

## Test

107 test, tutti su JVM, nessuna rete reale.

| Modulo | Test | Cosa coprono |
| --- | --- | --- |
| `:domain` | 4 | parsing dei tipi, valori sconosciuti |
| `:data` | 64 | mapper, MockWebServer su fixture reali accorciate, PagingSource, indici in memoria, repository, Room in memoria |
| `:app` | 39 | ViewModel (debounce, filtri, preferiti, cancellazione), schermate Compose via Robolectric |

Alcuni valgono più di altri: c'è un test di regressione per un crash che si vedeva solo a
`LazyColumn` disegnata (conteggio delle righe e chiavi letti da snapshot diversi), uno che verifica
il contrasto su tutti e diciotto i tipi, e uno che verifica che un preferito sopravviva alla riga che
sparisce mentre la scrittura è ancora in volo.

Non ci sono test strumentati: la copertura è su dominio, dati, ViewModel e Compose tramite
Robolectric.

## Cosa è stato tagliato

- **Schermata di dettaglio.** I requisiti chiedono lista e preferiti, e una terza schermata avrebbe
  aggiunto navigazione e stato senza aggiungere niente a ciò su cui il progetto viene valutato.
- **Uso offline.** I preferiti sopravvivono al riavvio, ma il loro contenuto arriva sempre dalla
  rete: senza connessione la pagina mostra i nomi e i placeholder. Salvare lo snapshot completo lo
  risolverebbe, al prezzo di dati duplicati.
- **Cache condivisa fra le due schermate.** Ogni ViewModel ha la sua, quindi aprire i preferiti
  richiede di nuovo le righe. La cache HTTP evita la rete, non la chiamata.
- **La query non sopravvive alla morte del processo.** Si risolverebbe con `SavedStateHandle`.
- **Posizione della lista al cambio di query.** `LazyColumn` conserva la posizione per chiave, quindi
  svuotando la ricerca si resta a metà lista invece di tornare in cima. È una decisione di UX più che
  un bug, e non l'ho presa da solo.

## Con più tempo

1. Test strumentati, a partire da uno end to end sulla navigazione: oggi la bottom bar è testata da
   sola perché è stateless, ma che toccare una tab cambi davvero schermata non è verificato.
2. Screenshot test, così le `@Preview` smettono di essere verificabili solo a occhio nell'IDE.
3. Snapshot dei preferiti in Room, per farli funzionare offline.
4. `SavedStateHandle` per query e filtri.
5. Schermata di dettaglio, che è anche il posto naturale dove mostrare statistiche ed evoluzioni.
