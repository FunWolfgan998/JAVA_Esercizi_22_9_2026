package Es2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class Main {

    // Implementate un metodo che restituisca le ultime N richieste con status ≥ 500
    public static List<RichiestaHttp> getUltimeErrori5xx(ArrayList<RichiestaHttp> storico, int n) {
        List<RichiestaHttp> risultati = new ArrayList<>();
        for (int i = storico.size() - 1; i >= 0 && risultati.size() < n; i--) {
            RichiestaHttp r = storico.get(i);
            if (r.statusCode >= 500) {
                risultati.add(r);
            }
        }
        return risultati;
    }

    public static void aggiornaFinestraScorrevole(LinkedList<RichiestaHttp> finestra, RichiestaHttp nuovaRichiesta) {
        finestra.addLast(nuovaRichiesta);
        if (finestra.size() > 10) {
            // removeFirst() su LinkedList è O(1) perché stacca solo il puntatore del nodo.
            // Se usavamo ArrayList con remove(0) era O(n) perché doveva shiftare a sinistra tutti gli altri elementi
            finestra.removeFirst();
        }
    }

    // TreeSet mantiene i dati già ordinati (inserimento in O(log n)),
    // quindi per il 90° percentile basta avanzare fino al 90% degli elementi senza fare un Collections.sort() che costerebbe O(n log n)
    public static Long calcolaPercentile90(TreeSet<Long> tempi) {
        if (tempi.isEmpty()) return 0L;

        // Calcoliamo quanti passi fare per raggiungere il valore che si trova al 90% della sequenza
        int passi = (int) Math.ceil(tempi.size() * 0.90) - 1;

        // Partiamo dal tempo minimo con first() e saltiamo al successivo con higher()
        Long valorePercentile = tempi.first();
        for (int i = 0; i < passi; i++) {
            valorePercentile = tempi.higher(valorePercentile);
        }
        return valorePercentile;
    }

    public static void stampaReport(ArrayList<RichiestaHttp> storico,
                                    LinkedList<RichiestaHttp> finestra,
                                    HashSet<String> ipSospetti,
                                    TreeSet<Long> tempiRispostaUnici) {

        System.out.println("Totale richieste storico: " + storico.size());
        System.out.println("Finestra attuale (ultime 10): " + finestra.size());
        System.out.println("IP sospetti distinti: " + ipSospetti.size() + " -> " + ipSospetti);

        int sospettiInFinestra = 0;
        for (RichiestaHttp r : finestra) {
            // contains() su HashSet lavora in O(1) medio con la tabella hash, con ArrayList O(n)
            if (ipSospetti.contains(r.ip)) {
                sospettiInFinestra++;
            }
        }
        System.out.println("Richieste da IP sospetti nelle ultime 10 in finestra: " + sospettiInFinestra);

        Long p90 = calcolaPercentile90(tempiRispostaUnici);
        System.out.println("Tempo risposta al 90° percentile: " + p90 + " ms");

        long soglia = 500L;
        System.out.println("Tempo minore o uguale più vicino a 500ms (floor): " + tempiRispostaUnici.floor(soglia));
        System.out.println("Tempo strettamente superiore più vicino a 500ms (higher): " + tempiRispostaUnici.higher(soglia));
        System.out.println("Tempi lenti >= 500ms (tailSet): " + tempiRispostaUnici.tailSet(soglia));
    }

    public static void main(String[] args) {
        ArrayList<RichiestaHttp> storico = new ArrayList<>();
        LinkedList<RichiestaHttp> finestraUltime10 = new LinkedList<>();
        HashSet<String> ipSospetti = new HashSet<>();
        TreeSet<Long> tempiRispostaUnici = new TreeSet<>();

        String[] poolIp = {
                "192.168.1.10", "10.0.0.5", "172.16.0.2", "192.168.1.10",
                "10.0.0.99", "192.168.1.55", "172.16.0.2", "10.0.0.5"
        };
        String[] poolPath = {
                "/index.html", "/api/login", "/dashboard", "/images/logo.png",
                "/checkout", "/api/data", "/admin"
        };
        int[] poolStatus = {200, 200, 404, 200, 500, 200, 503, 301, 403, 200, 502};
        long[] poolTempi = {45, 120, 310, 80, 650, 95, 1400, 30, 210, 520, 890};
        long timestampBase = 1732000000L;

        for (int i = 0; i < 35; i++) {
            RichiestaHttp richiesta = new RichiestaHttp(
                    poolIp[i % poolIp.length],
                    poolPath[i % poolPath.length],
                    poolStatus[i % poolStatus.length],
                    poolTempi[i % poolTempi.length] + (i * 3),
                    timestampBase + (i * 5)
            );

            storico.add(richiesta);
            aggiornaFinestraScorrevole(finestraUltime10, richiesta);

            // HashSet non ammette duplicati: se l'IP c'è già non lo reinserisce senza dover fare scansioni manuali O(n)
            if (richiesta.statusCode >= 400) {
                ipSospetti.add(richiesta.ip);
            }

            tempiRispostaUnici.add(richiesta.tempoRispostaMs);
        }

        List<RichiestaHttp> erroriCritici = getUltimeErrori5xx(storico, 3);
        System.out.println("Ultime 3 richieste >= 500:");
        for (RichiestaHttp r : erroriCritici) {
            System.out.println(r);
        }
        System.out.println();

        stampaReport(storico, finestraUltime10, ipSospetti, tempiRispostaUnici);
    }
}