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

public class GestoreTicket {

    /*
     * MOTIVAZIONE SCELTA BufferedReader vs java.nio.file.Files.readAllLines:
     * Files.readAllLines() carica tutte le righe del file dentro una lista in memoria (RAM O(n)).
     * BufferedReader legge invece in streaming riga per riga con un buffer a dimensione fissa (RAM O(1)),
     * evitando problemi di memoria se il file CSV contiene milioni di righe.
     */
    public static void main(String[] args) {
        String fileInput = "ticket.csv";
        String fileOutput = "report_lavorazione.txt";
        String fileErrori = "log_errori.txt";

        // La PriorityQueue è un heap binario: add() e poll() costano O(log n), peek() costa O(1)
        PriorityQueue<Ticket> codaLavorazione = new PriorityQueue<>();

        // BONUS: Struttura dedicata per monitorare i soli ticket CRITICI ancora in attesa
        PriorityQueue<Ticket> codaCriticiInAttesa = new PriorityQueue<>();

        List<String> righeScartate = new ArrayList<>();

        // =========================================================================
        // LETTURA FILE CON GESTIONE ERRORI E RIGHE CORROTTE
        // =========================================================================
        try (BufferedReader br = new BufferedReader(new FileReader(fileInput))) {
            String riga;
            int contatoreRiga = 0;

            while ((riga = br.readLine()) != null) {
                contatoreRiga++;

                // Ignoriamo riga di intestazione o righe vuote
                if (contatoreRiga == 1 && riga.toLowerCase().startsWith("id,")) continue;
                if (riga.trim().isEmpty()) continue;

                String[] campi = riga.split(",");

                // Controllo numero campi
                if (campi.length != 4) {
                    righeScartate.add("Riga " + contatoreRiga + " scartata: numero campi errato -> " + riga);
                    continue;
                }

                String id = campi[0].trim();
                String descrizione = campi[1].trim();
                String livello = campi[2].trim().toUpperCase(); // Normalizziamo in maiuscolo (es. "basso" -> "BASSO")
                long timestamp;

                try {
                    timestamp = Long.parseLong(campi[3].trim());
                } catch (NumberFormatException e) {
                    righeScartate.add("Riga " + contatoreRiga + " scartata: timestamp numerico invalido -> " + campi[3]);
                    continue;
                }

                // Controllo validità livello ammesso
                if (!livello.equals("CRITICO") && !livello.equals("ALTO") &&
                        !livello.equals("MEDIO") && !livello.equals("BASSO")) {
                    righeScartate.add("Riga " + contatoreRiga + " scartata: livello sconosciuto '" + livello + "'");
                    continue;
                }

                Ticket ticket = new Ticket(id, descrizione, livello, timestamp);
                codaLavorazione.add(ticket);

                // BONUS: se il ticket è CRITICO lo tracciamo e lanciamo l'allarme se ce ne sono più di 2 in attesa
                if (ticket.livello.equals("CRITICO")) {
                    codaCriticiInAttesa.add(ticket);
                    if (codaCriticiInAttesa.size() > 2) {
                        System.out.println("[ALLARME LIVELLO CRITICO] Rilevati " + codaCriticiInAttesa.size()
                                + " ticket critici in attesa contemporaneamente! Ultimo aggiunto: " + ticket.id);
                    }
                }
            }

        } catch (FileNotFoundException e) {
            System.err.println("File non trovato: verificare che " + fileInput + " sia presente nel percorso.");
            return;
        } catch (IOException e) {
            System.err.println("Errore di I/O durante la lettura del file: " + e.getMessage());
            return;
        }

        // Salvataggio degli errori di parsing su log dedicato
        if (!righeScartate.isEmpty()) {
            try (BufferedWriter bwErr = new BufferedWriter(new FileWriter(fileErrori))) {
                for (String err : righeScartate) {
                    bwErr.write(err + "\n");
                }
                System.out.println("Segnalati " + righeScartate.size() + " errori di parsing salvati in: " + fileErrori);
            } catch (IOException e) {
                System.err.println("Impossibile scrivere il file di log errori: " + e.getMessage());
            }
        }


        // =========================================================================
        // ELABORAZIONE CON PRIORITYQUEUE E SCRITTURA REPORT (try-with-resources)
        // =========================================================================
        int tempoCumulativo = 0;
        int consecutiviCriticiOAlti = 0;
        int pauseEffettuate = 0;

        int countCritici = 0, countAlti = 0, countMedi = 0, countBassi = 0;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileOutput))) {
            bw.write("PIANO DI LAVORAZIONE TICKET (ORDINATO PER PRIORITA E FIFO)\n");
            bw.write("========================================================================================\n");
            bw.write(String.format("%-6s | %-8s | %-10s | %-16s | %s\n", "ID", "LIVELLO", "DURATA", "TEMPO CUMULATO", "DESCRIZIONE"));
            bw.write("----------------------------------------------------------------------------------------\n");

            // poll() estrae l'elemento a priorità più alta in O(log n)
            while (!codaLavorazione.isEmpty()) {
                Ticket t = codaLavorazione.poll();

                // Rimuoviamo il ticket anche dalla coda di monitoraggio dei critici non lavorati
                if (t.livello.equals("CRITICO")) {
                    codaCriticiInAttesa.remove(t);
                }

                // Incremento contatori statistici
                switch (t.livello) {
                    case "CRITICO": countCritici++; break;
                    case "ALTO":    countAlti++; break;
                    case "MEDIO":   countMedi++; break;
                    case "BASSO":   countBassi++; break;
                }

                // VINCOLO REALISTICO: massimo 5 ticket CRITICI o ALTI di fila, poi scatta pausa obbligatoria di 10 min
                if (t.livello.equals("CRITICO") || t.livello.equals("ALTO")) {
                    consecutiviCriticiOAlti++;
                } else {
                    consecutiviCriticiOAlti = 0; // se arriva un ticket MEDIO o BASSO il contatore dello stress si resetta
                }

                int durata = t.getDurataMinuti();
                tempoCumulativo += durata;

                String rigaReport = String.format("%-6s | %-8s | %3d min    | %4d min totali  | %s",
                        t.id, t.livello, durata, tempoCumulativo, t.descrizione);
                bw.write(rigaReport + "\n");
                System.out.println("Lavorato: " + rigaReport);

                // Controllo pausa tecnico dopo 5 ticket ad alta intensità
                if (consecutiviCriticiOAlti == 5) {
                    tempoCumulativo += 10;
                    pauseEffettuate++;
                    consecutiviCriticiOAlti = 0; // reset contatore dopo la pausa
                    bw.write(">> [PAUSA OBBLIGATORIA TECNICO +10 MIN] Tempo cumulato aggiornato: " + tempoCumulativo + " min\n");
                    System.out.println(">> [PAUSA TECNICO 10 MIN]");
                }
            }

            // Sezione finale di riepilogo
            int totaleTicket = countCritici + countAlti + countMedi + countBassi;
            bw.write("========================================================================================\n");
            bw.write("RIASSUNTO FINALE:\n");
            bw.write("- Totale ticket lavorati: " + totaleTicket + "\n");
            bw.write("  * CRITICI: " + countCritici + "\n");
            bw.write("  * ALTI:    " + countAlti + "\n");
            bw.write("  * MEDI:    " + countMedi + "\n");
            bw.write("  * BASSI:   " + countBassi + "\n");
            bw.write("- Pause obbligatorie effettuate: " + pauseEffettuate + " (10 min ciascuna)\n");
            bw.write("- Tempo totale stimato: " + tempoCumulativo + " minuti (~"
                    + String.format("%.2f", tempoCumulativo / 60.0) + " ore)\n");

            System.out.println("\nLavorazione completata. File generato: " + fileOutput);

        } catch (IOException e) {
            System.err.println("Errore di I/O durante la scrittura del report: " + e.getMessage());
        }
    }
}