# Relazione Tecnica

## 1. Motivazione della scelta delle Strutture Dati

### 1.1 `ArrayList<RichiestaHttp>` per lo Storico Completo
* **Scopo**: Mantenere l'elenco cronologico di tutte le richieste arrivate al server nell'ordine esatto di arrivo.
* **Motivazione**: L'`ArrayList` si basa su un array ridimensionabile continuo in memoria. L'inserimento in coda (append) è estremamente veloce ($O(1)$ ammortizzato) e l'accesso per indice tramite `get(i)` è immediato ($O(1)$).
* **Efficienza nell'iterazione**: Per estrarre le ultime $N$ richieste con errore $\ge 500$, partiamo dall'ultimo indice (`size() - 1`) e andiamo a ritroso. In questo modo evitiamo di clonare la lista o creare costose sottoliste (`subList`).

### 1.2 `LinkedList<RichiestaHttp>` per la Finestra Scorrevole (Sliding Window)
* **Scopo**: Tenere in memoria **solo le ultime 10 richieste** ricevute (coda FIFO a dimensione fissa).
* **Motivazione**: Quando arriva l'undicesima richiesta, dobbiamo aggiungere il nuovo elemento in fondo ed eliminare il più vecchio in testa.
    * In `LinkedList` i metodi `addLast()` e `removeFirst()` modificano unicamente i puntatori dei nodi: l'operazione costa **$O(1)$**.
    * Se avessimo usato `ArrayList`, fare `remove(0)` avrebbe costretto la CPU a **spostare verso sinistra tutti gli altri 9 elementi** in memoria ($O(N)$). Su grandi volumi di log ad alta frequenza, questo overhead continuo degraderebbe le prestazioni del server.

### 1.3 `HashSet<String>` per gli Indirizzi IP Sospetti (Errori 4xx/5xx)
* **Scopo**: Raccogliere l'elenco degli IP che hanno generato errori, senza ripetizioni.
* **Motivazione**:
    * Un insieme (`Set`) **non ammette duplicati**: se lo stesso IP genera 20 errori di fila, verrà inserito una sola volta automaticamente.
    * L'`HashSet` si basa su una tabella hash, garantendo inserimento e ricerca (`contains`) in tempo medio **$O(1)$**. Con una `ArrayList`, per evitare duplicati avremmo dovuto fare un controllo lineare con `contains()`, che scansiona tutta la lista con complessità **$O(N)$**.

### 1.4 `TreeSet<Long>` per la Classifica dei Tempi di Risposta
* **Scopo**: Mantenere i tempi di risposta unici già ordinati in modo crescente.
* **Motivazione**: Il `TreeSet` è implementato tramite un albero binario auto-bilanciante (Red-Black Tree). Ogni inserimento mantiene la collezione sempre ordinata in $O(\log N)$.
    * Ci permette di effettuare navigazioni avanzate senza dover richiamare un ordinamento manuale (`Collections.sort()` che costerebbe $O(N \log N)$):
        * `floor(soglia)`: trova il tempo registrato minore o uguale alla soglia.
        * `higher(soglia)`: trova il tempo strettamente maggiore della soglia.
        * `tailSet(soglia)`: estrae istantaneamente tutti i tempi critici uguali o superiori a un certo valore.

---

## Tabella Comparativa di Complessità Computazionale (Big-O)

| Struttura Scelta | Operazione Critica | Complessità Struttura Scelta | Alternativa Scartata | Complessità Alternativa | Perché l'alternativa è peggiore |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`ArrayList`** | Accesso e scorrimento a ritroso per indice | **$O(1)$** per accesso | `LinkedList` | **$O(N)$** per ogni `get(i)` | `LinkedList` non ha accesso diretto: per arrivare all'indice $i$ deve scorrere i puntatori uno per uno. |
| **`LinkedList`** | Rimozione del dato più vecchio in testa (`removeFirst`) | **$O(1)$** | `ArrayList.remove(0)` | **$O(N)$** | `ArrayList` deve scalare fisicamente a sinistra tutti i rimanenti elementi contigui in memoria. |
| **`HashSet`** | Verifica presenza e inserimento senza duplicati | **$O(1)$** (caso medio) | `ArrayList` con controllo `contains()` | **$O(N)$** | `ArrayList` deve esaminare ogni singolo elemento da capo a fondo per verificare se è già presente. |
| **`TreeSet`** | Inserimento e mantenimento ordinato | **$O(\log N)$** | `ArrayList` + ordinamento ad ogni arrivo | **$O(N \log N)$** | Richiederebbe di riordinare l'intero array dopo ogni singolo pacchetto o lettura. |

---
