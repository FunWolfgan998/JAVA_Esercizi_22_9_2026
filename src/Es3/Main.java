package Es3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class Main {

    public static void main(String[] args) {
        String fileInput = "src/Es3/ticket.csv";
        String fileOutput = "src/Es3/report_lavorazione.txt";
        String fileErrori = "src/Es3/log_errori.txt";

        PriorityQueue<Ticket> coda = new PriorityQueue<>();

        // Bonus: seconda PriorityQueue per tenere traccia dei soli ticket critici non ancora lavorati
        PriorityQueue<Ticket> criticiInAttesa = new PriorityQueue<>();

        List<String> logErrori = new ArrayList<>();

        // Usiamo BufferedReader al posto di Files.readAllLines perché legge riga per riga senza caricare tutto il file in memoria
        try (BufferedReader br = new BufferedReader(new FileReader(fileInput))) {
            String riga;
            int numeroRiga = 0;

            while ((riga = br.readLine()) != null) {
                numeroRiga++;

                // Saltiamo l'intestazione del CSV e le righe vuote
                if (numeroRiga == 1 && riga.toLowerCase().startsWith("id,")) continue;
                if (riga.trim().isEmpty()) continue;

                String[] campi = riga.split(",");

                // Se mancano campi salviamo l'errore e usiamo continue per non bloccare la lettura delle altre righe
                if (campi.length != 4) {
                    logErrori.add("Riga " + numeroRiga + " scartata (campi mancanti o troppi): " + riga);
                    continue;
                }

                String id = campi[0].trim();
                String descrizione = campi[1].trim();

                // Usiamo toUpperCase() così accettiamo anche livelli scritti in minuscolo come "basso"
                String livello = campi[2].trim().toUpperCase();
                long timestamp;

                // Se il timestamp non è un numero valido lo logghiamo senza interrompere il programma
                try {
                    timestamp = Long.parseLong(campi[3].trim());
                } catch (NumberFormatException e) {
                    logErrori.add("Riga " + numeroRiga + " scartata (timestamp non valido): " + campi[3]);
                    continue;
                }

                // Se il livello è sconosciuto lo scartiamo nel log a parte
                if (!livello.equals("CRITICO") && !livello.equals("ALTO") &&
                        !livello.equals("MEDIO") && !livello.equals("BASSO")) {
                    logErrori.add("Riga " + numeroRiga + " scartata (livello non valido): " + livello);
                    continue;
                }

                Ticket t = new Ticket(id, descrizione, livello, timestamp);
                coda.add(t);

                // Se il ticket è critico lo salviamo nella seconda coda e lanciamo l'allarme se ce ne sono più di 2 in attesa
                if (livello.equals("CRITICO")) {
                    criticiInAttesa.add(t);
                    if (criticiInAttesa.size() > 2) {
                        System.out.println("ALLARME: ci sono più di 2 ticket critici in attesa contemporaneamente! (Totale: " + criticiInAttesa.size() + ")");
                    }
                }
            }

            // Gestiamo FileNotFoundException da IOException con messaggi custom di errore
        } catch (FileNotFoundException e) {
            System.err.println("Errore: il file di input non è stato trovato -> " + fileInput);
            return;
        } catch (IOException e) {
            System.err.println("Errore di I/O durante la lettura del file: " + e.getMessage());
            return;
        }

        // Salviamo le righe scartate in un file di log a parte per non perderle
        if (!logErrori.isEmpty()) {
            try (BufferedWriter bwErr = new BufferedWriter(new FileWriter(fileErrori))) {
                for (String err : logErrori) {
                    bwErr.write(err + "\n");
                }
            } catch (IOException e) {
                System.err.println("Errore durante la scrittura del log errori: " + e.getMessage());
            }
        }

        int tempoTotale = 0;
        int consecutiviCriticiAlti = 0;
        int pause = 0;
        int countCritici = 0, countAlti = 0, countMedi = 0, countBassi = 0;

        // Usiamo il try-with-resources così il BufferedWriter si chiude da solo anche in caso di eccezione
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileOutput))) {
            bw.write("ORDINE EFFETTIVO DI LAVORAZIONE TICKET\n");
            bw.write("--------------------------------------------------------------------------------\n");

            // poll() estrae il ticket con priorità più alta secondo compareTo in O(log n)
            while (!coda.isEmpty()) {
                Ticket t = coda.poll();

                // Togliamo il ticket anche dalla seconda coda dei soli critici non lavorati
                if (t.livello.equals("CRITICO")) {
                    criticiInAttesa.remove(t);
                }

                switch (t.livello) {
                    case "CRITICO": countCritici++; break;
                    case "ALTO":    countAlti++; break;
                    case "MEDIO":   countMedi++; break;
                    case "BASSO":   countBassi++; break;
                }

                // Incrementiamo se il ticket è critico o alto, altrimenti resettiamo il contatore
                if (t.livello.equals("CRITICO") || t.livello.equals("ALTO")) {
                    consecutiviCriticiAlti++;
                } else {
                    consecutiviCriticiAlti = 0;
                }

                int durata = t.getDurataMinuti();
                tempoTotale += durata;

                String rigaReport = t.id + " | " + t.livello + " | Durata: " + durata + "m | Tempo cumulativo: " + tempoTotale + "m | " + t.descrizione;
                bw.write(rigaReport + "\n");
                System.out.println("Lavorazione: " + rigaReport);

                // Dopo 5 ticket critici o alti consecutivi scatta la pausa obbligatoria di 10 minuti
                if (consecutiviCriticiAlti == 5) {
                    tempoTotale += 10;
                    pause++;
                    consecutiviCriticiAlti = 0;
                    bw.write(">> Pausa obbligatoria tecnico (+10 min) -> Tempo cumulativo: " + tempoTotale + "m\n");
                }
            }

            // Riepilogo finale in fondo al file di testo come richiesto dalle specifiche
            bw.write("--------------------------------------------------------------------------------\n");
            bw.write("RIEPILOGO FINALE:\n");
            bw.write("Totale ticket lavorati: " + (countCritici + countAlti + countMedi + countBassi) + "\n");
            bw.write("Critici: " + countCritici + " | Alti: " + countAlti + " | Medi: " + countMedi + " | Bassi: " + countBassi + "\n");
            bw.write("Pause obbligatorie effettuate: " + pause + "\n");
            bw.write("Tempo totale stimato: " + tempoTotale + " minuti\n");

        } catch (IOException e) {
            System.err.println("Errore durante la scrittura del report finale: " + e.getMessage());
        }
    }
}