package Es3;

public class Ticket implements Comparable<Ticket> {
    public String id;
    public String descrizione;
    public String livello;
    public long timestampArrivo;

    public Ticket(String id, String descrizione, String livello, long timestampArrivo) {
        this.id = id;
        this.descrizione = descrizione;
        this.livello = livello;
        this.timestampArrivo = timestampArrivo;
    }

    // Convertiamo il livello in un numero:
    // In PriorityQueue l'elemento estratto per primo con poll() è quello con valore "minore" secondo compareTo.
    // Dando 1 a CRITICO e 4 a BASSO, 1 < 2 e quindi CRITICO uscirà sempre prima di ALTO, MEDIO e BASSO.
    public int getPrioritaValore() {
        switch (livello) {
            case "CRITICO": return 1;
            case "ALTO":    return 2;
            case "MEDIO":   return 3;
            case "BASSO":   return 4;
            default:        return 99;
        }
    }

    // Minuti di lavorazione stimati per ciascun tipo di ticket
    public int getDurataMinuti() {
        switch (livello) {
            case "CRITICO": return 15;
            case "ALTO":    return 30;
            case "MEDIO":   return 60;
            case "BASSO":   return 120;
            default:        return 0;
        }
    }

    // Prima confrontiamo la priorità.
    // A parità di livello usiamo il timestamp: quello più piccolo è il più vecchio ed esce prima (FIFO)
    @Override
    public int compareTo(Ticket altro) {
        int confrontoPriorita = Integer.compare(this.getPrioritaValore(), altro.getPrioritaValore());
        if (confrontoPriorita != 0) {
            return confrontoPriorita;
        }
        return Long.compare(this.timestampArrivo, altro.timestampArrivo);
    }

    @Override
    public String toString() {
        return "[" + id + " | " + livello + "] ts=" + timestampArrivo + " - " + descrizione;
    }
}