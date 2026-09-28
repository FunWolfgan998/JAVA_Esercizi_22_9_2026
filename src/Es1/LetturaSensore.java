import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LetturaSensore {
    // Usiamo le classi wrapper invece dei primitivi perché possono assumere il valore null
    public Double temperatura;
    public Integer umiditaPercentuale;
    public Long timestampUnix;
    public Boolean batteriaScarica;

    // Costruttore vuoto: tutto a null.
    public LetturaSensore() {
        this.temperatura = null;
        this.umiditaPercentuale = null;
        this.timestampUnix = null;
        this.batteriaScarica = null;
    }
    public LetturaSensore(Double temperatura, Integer umiditaPercentuale, Long timestampUnix, Boolean batteriaScarica) {
        this.temperatura = temperatura;
        this.umiditaPercentuale = umiditaPercentuale;
        this.timestampUnix = timestampUnix;
        this.batteriaScarica = batteriaScarica;
    }
    public static Optional<LetturaSensore> parsePacchetto(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Optional.empty();
        }
        LetturaSensore lettura = new LetturaSensore();
        String[] coppie = raw.split(";");

        for (String pezzo : coppie) {
            String[] kv = pezzo.split("=");
            if (kv.length != 2) {
                continue;
            }

            String chiave = kv[0].trim();
            String valore = kv[1].trim();

            switch (chiave) {
                case "temp":
                    try {
                        lettura.temperatura = Double.valueOf(valore);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore parsing temperatura '" + valore + "': non è un numero valido.");
                        // Non blocchiamo il resto, lasciamo temperatura = null
                    }
                    break;

                case "umid":
                    try {
                        //.parseInt() ritorna un int primitivo mentre .valueOf ritorna un oggetto wrapper Integer.
                        // In questo caso visto che abbiamo nella classe il campo come Integer ci conviene usare .valueOf
                        Integer u = Integer.valueOf(valore);
                        if (u < 0 || u > 100) {
                            throw new LetturaInvalidaException("Umidità fuori scala (0-100): " + u);
                        }
                        lettura.umiditaPercentuale = u;
                    } catch (NumberFormatException e) {
                        System.err.println("Errore parsing umidità '" + valore + "': non è un intero valido.");
                    }
                    break;

                case "ts":
                    try {
                        lettura.timestampUnix = Long.valueOf(valore);
                    } catch (NumberFormatException e) {
                        System.err.println("Errore parsing timestamp '" + valore + "'");
                    }
                    break;

                case "batt_low":
                    // Boolean.valueOf restituisce true solo se la stringa è "true" (case insensitive), altrimenti false
                    lettura.batteriaScarica = Boolean.valueOf(valore);
                    break;

                default:
                    // Chiave sconosciuta ignorata
                    break;
            }
        }

        return Optional.of(lettura);
    }


    public static void confrontaBatteria(Integer b1, Integer b2){
        /*
         * L'operatore `==` con oggetti `Integer` verifica l'uguaglianza per riferimento, per valori
         * `Integer` compresi tra -128 e 127, Java memorizza in cache tali oggetti, pertanto `==` può restituire `true`;
         * per valori al di fuori di questo intervallo, vengono creati nuovi oggetti `Integer`, portando a un esito negativo del confronto == (`false`) pur avendo lo stesso valore.
        */

        System.out.println("Con operatore '==': " + (b1 == b2));

        System.out.println("Con Objects.equals(): " + Objects.equals(b1, b2));

    }

    @Override
    public String toString() {
        return "LetturaSensore [temp=" + temperatura +
                ", umid=" + umiditaPercentuale + "%" +
                ", ts=" + timestampUnix +
                ", batt_low=" + batteriaScarica + "]";
    }
}
