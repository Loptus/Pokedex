# Pokédex

App Android nativa che elenca i Pokémon della [PokeAPI](https://pokeapi.co/docs/v2), con ricerca per
nome, filtro per tipo e preferiti salvati sul dispositivo.

## Come si builda e si esegue

Serve Android Studio, o in alternativa un JDK 17. Non ci sono chiavi né configurazione locale: si
clona e si compila.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

L'APK esce in `app/build/outputs/apk/debug/`. Da Android Studio basta aprire la cartella e premere
Run: l'unico permesso richiesto è `INTERNET`, e `minSdk` è 26.

I test, insieme a lint e al resto dei controlli:

```bash
./gradlew build
```

Per i soli test, con un accorgimento che vale la pena conoscere:

```bash
./gradlew :app:testDebugUnitTest --rerun :data:testDebugUnitTest --rerun :domain:test --rerun
```

I `--rerun` non sono un vezzo: senza, Gradle riusa il risultato in cache e restituisce verde senza
eseguire niente. `:domain` è un modulo JVM, quindi i suoi test stanno sotto `test` e non sotto
`testDebugUnitTest`.

Le versioni di Kotlin, AGP e di ogni libreria stanno in `gradle/libs.versions.toml`.

## Come è organizzato

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
  importa una sola classe dal layer dati, le serve solo a runtime perché Hilt trovi i binding. Se un
  giorno `runtimeOnly` non bastasse più, è il segnale che qualcosa sta sconfinando.
- DTO ed entity Room non escono mai dal layer dati.

Dentro `:app` i package sono per feature (`list/`, `favorites/`, `navigation/`), e in `common/` sta
solo ciò che entrambe le schermate usano davvero: la riga con i suoi componenti e il caricatore del
contenuto per riga.

Ogni accesso ai dati passa da uno use case, anche quando è una lettura banale: ViewModel e UI non
chiamano mai un repository. La `UiState` di una schermata contiene solo stato di interfaccia (la
query digitata, i tipi selezionati), mentre i dati viaggiano su un flusso a parte. Una schermata
senza stato di interfaccia proprio, come i preferiti, non ha una `UiState` affatto.

## Le scelte che si notano per prime

### La paginazione avviene in memoria, non chiamando l'API con limit e offset

La lista è paginata a 20 elementi e carica la pagina successiva da sola, come richiesto. Quello che
non fa è chiedere le pagine alla API: scarica una volta l'indice completo dei nomi e pagina su
quello.

Il motivo è misurato sulla API reale, a parità di gzip: l'intero indice dei nomi pesa poco meno di
12 KB, cioè quanto le due richieste che riempiono **una singola riga** della lista. Paginare i nomi
via API avrebbe ottimizzato l'unica cosa che non costa niente. In più la API non ha ricerca fuzzy
(`GET /pokemon/{name}` fa match esatto), quindi senza indice locale la ricerca per sottostringa non
sarebbe possibile: con l'indice in memoria, navigazione e ricerca diventano lo stesso percorso di
codice, dove query vuota significa "tutti i nomi".

Il costo: il primo avvio scarica l'indice prima di poter mostrare qualcosa, e la lista vive in
memoria.

### Il contenuto di una riga si carica per riga, non per pagina

Una pagina emette solo i puntatori e **non costa nessuna richiesta**, quindi la lista appare subito.
Ogni riga chiede il proprio contenuto quando arriva a schermo, dentro un `LaunchedEffect`: quando la
riga esce, Compose lo cancella e Retrofit cancella le richieste, così scorrere veloce non paga le
righe superate.

L'alternativa, caricare il contenuto di tutta la pagina, avrebbe voluto dire una ventina di volte
quel peso per venti righe di cui l'utente ne vede quattro o cinque.

Il costo: è un N+1 dichiarato. È domato dalla cancellazione, da una cache in memoria per id che
sopravvive alla rotazione, e dalla cache HTTP su disco di OkHttp. I fallimenti non si mettono in
cache, così una riga che torna visibile riprova da sola.

### Il filtro per tipo è una riga di chip, non il campo unico del mockup

Il mockup ha un solo campo per nome e tipo. Qui la ricerca per nome resta nel campo e i tipi sono
chip a selezione multipla.

Chip separati mostrano quali tipi esistono invece di farli indovinare, permettono di combinarli e
tengono la query non ambigua per i layer sotto, che ricevono nome e tipi già distinti. Più tipi
selezionati stanno in unione fra loro, perché un filtro che con Fuoco e Acqua insieme restituisce
sempre zero sembra rotto e non vuoto, e in AND con il nome.

Il costo: è uno scostamento dal riferimento visivo, e occupa una riga in più sotto la barra di
ricerca.

### Un preferito salva il puntatore, non una copia della scheda

In Room finiscono id, nome e url del dettaglio. Non immagine, tipi e descrizione.

Così la pagina dei preferiti riusa la riga della lista senza modifiche e carica il contenuto allo
stesso modo, e la copia salvata non può invecchiare rispetto alla API.

Il costo, che è visibile: senza rete i preferiti mostrano i nomi e i placeholder al posto delle
schede. La cache HTTP evita la rete ma non la chiamata, e la cache in memoria vive nel ViewModel,
quindi non attraversa le due schermate.

## Le altre decisioni

**Gli url si seguono, non si costruiscono.** La PokeAPI è una rete di link e i suoi id non sono
allineati: dall'id 10001 in poi le voci sono forme alternative, e la loro species sta sotto un id
diverso e più basso (`/pokemon/10001/` è deoxys-attack, la sua species è `/pokemon-species/386/`,
mentre `/pokemon-species/10001/` è 404). Costruire un indirizzo da un id funziona per il primo
migliaio di voci e poi fallisce in silenzio su tutta la coda. Quindi l'url del dettaglio si prende
dall'indice, quello della species dal dettaglio, e in Retrofit si usa `@Url`. Il costo è che le due
richieste di una riga sono sequenziali, perché il secondo indirizzo sta dentro la prima risposta.

**Solo la digitazione è debounced.** La ricerca per nome aspetta che il testo si assesti, il toggle
di un chip no: digitare arriva una lettera alla volta, toccare un chip è un'azione singola e
deliberata. Svuotare il campo salta l'attesa.

**Chi è preferito lo decide il ViewModel.** Il flusso paginato emette elementi con il flag già
risolto, e il composable stampa un booleano invece di cercare un id dentro un insieme: applicare
quella regola nella UI sarebbe una decisione presa nel posto sbagliato.

**Dynamic color disattivato.** I colori dei tipi sono già tutto il colore che lo schermo regge, e il
dynamic color renderebbe incoerente ogni screenshot.

**Il testo dei chip passa da bianco a nero sopra una luminanza di 0,179**, non 0,5. La soglia viene
dalla formula di contrasto WCAG, dove nero e bianco pareggiano a `(L + 0,05) / 0,05 = 1,05 / (L +
0,05)`. Con quel pivot ogni tipo è garantito sopra 4,5:1 senza tarare diciotto coppie a mano.

**I valori che arrivano dalla API non si traducono.** Le stringhe di interfaccia sono in inglese e in
italiano, ma i nomi dei tipi restano quelli della API: sono dati, non testo di interfaccia, e
tradurli vorrebbe dire mantenere una tabella per lingua e disallinearsi dal vocabolario che la API
usa anche per la ricerca. Si mostrano capitalizzati.

**I DTO hanno un default esplicito su ogni campo.** Gson costruisce per reflection e scrive
tranquillamente un null dentro un campo che Kotlin dichiara non nullabile: i mapper sono l'unico
posto che decide cosa significa un valore mancante, e i loro test fanno da rete al posto del
compilatore.

**`initialLoadSize` è esplicito.** Il default di Paging è tre volte la pagina, che al primo colpo
caricherebbe sessanta elementi invece dei venti richiesti.

**Le rotte di navigazione sono stringhe** tenute insieme in un enum, non type safe: con due
destinazioni e nessun argomento, il type safe costerebbe il plugin di serializzazione per proteggere
da un errore che qui non si può fare.

## Come è testato

I test girano tutti su JVM, senza rete e senza dispositivo, e sono scritti insieme al codice.

- **Niente libreria di mocking.** I doppi sono fake scritti a mano, che rendono esplicito il
  comportamento simulato invece di nasconderlo dietro uno stub.
- **Networking e parsing** con MockWebServer su fixture JSON reali accorciate, più i test dei mapper
  sui campi mancanti, sulla scelta dello sprite e sulla pulizia del testo delle descrizioni.
- **Paginazione** con `paging-testing`, per verificare che si carichi una pagina alla volta e che
  l'append si fermi a fine lista.
- **ViewModel** con Turbine e `kotlinx-coroutines-test`: debounce, filtri, preferiti, e la
  cancellazione di una riga che esce dallo schermo.
- **Compose e Room** su JVM con Robolectric, database in memoria compreso.

Più dei numeri contano gli invarianti che si romperebbero in silenzio: un test di regressione per un
crash che si vedeva solo a `LazyColumn` disegnata, uno che verifica il contrasto su tutti e diciotto
i tipi, uno che verifica che un preferito salvato sopravviva alla riga che sparisce mentre la
scrittura è ancora in volo.

Cosa non è coperto, detto invece che lasciato intuire: non ci sono test strumentati, quindi che
toccare una tab cambi davvero schermata non è verificato (la bottom bar è testata da sola, perché è
stateless), e le `@Preview` si controllano a occhio nell'IDE.
