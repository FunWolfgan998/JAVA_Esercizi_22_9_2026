void main() {
    // Dimostrazione trabocchetto autoboxing
    System.out.println("Dimostrazione trabocchetto autoboxing");
    Integer b1 = 50;
    Integer b2 = 50;
    LetturaSensore.confrontaBatteria(b1, b2); // == darà true (valori pre-cached dentro la cache -128..127)

    Integer b3 = 150;
    Integer b4 = 150;
    LetturaSensore.confrontaBatteria(b3, b4); // == darà false per la differza di indirizzo di riferiemento

    String[] pacchetti = {
            "temp=23.5;umid=61;ts=1732000000;batt_low=false",           // Valido completo
            "temp=xx.xx;umid=50;ts=1732000010;batt_low=false",          // Temperatura corrotta
            "temp=;umid=40;ts=1732000020;batt_low=true",                // Temperatura mancante (null)
            "temp=18.2;umid=150;ts=1732000030;batt_low=false",          // Umidità fuori scala (eccezione)
            "temp=0.0;umid=80;ts=1732000040;batt_low=false",            // Temp a 0.0 (valida!)
            "temp=29.1;umid=corrotto;ts=1732000050;batt_low=false",     // Umidità testo
            "",                                                         // Pacchetto vuoto
            "temp=31.4;umid=30;ts=1732000060;batt_low=false"            // Valido completo
    };

    List<LetturaSensore> lettureValide = new ArrayList<>();
    int erroriTotali = 0;

    for (String raw : pacchetti) {
        try {
            Optional<LetturaSensore> opt = LetturaSensore.parsePacchetto(raw);
            if (opt.isPresent()) {
                lettureValide.add(opt.get());
            } else {
                erroriTotali++;
            }
        } catch (LetturaInvalidaException e) {
            System.err.println("Pacchetto rifiutato per dato invalido: " + e.getMessage());
            erroriTotali++;
        }
    }

    // Calcolo media delle temperature valide (ATTENZIONE AI NULL!)
    double sommaTemp = 0.0;
    int countTemp = 0;

    for (LetturaSensore l : lettureValide) {
        if (l.temperatura != null) {

            sommaTemp += l.temperatura;
            countTemp++;
        }
    }

    double mediaTemp = (countTemp > 0) ? (sommaTemp / countTemp) : 0.0;

    System.out.println("\nReport di analisi:");
    System.out.println("Numero di letture create: " + lettureValide.size());
    System.out.println("Numero errori/pacchetti scartati: " + erroriTotali);
    System.out.printf("Media temperature valide: %.2f °C (su %d valori non null)\n", mediaTemp, countTemp);

    // Ordinamento per temperatura decrescente usando Double.compare
    /*
     * I double in caso di sottrazione porta:
     * - alla perdita precisione per floating point underflow.
     * - in caso si valore null si scatenerebbe NullPointerException immediato.
     */
    lettureValide.sort((l1, l2) -> {
        if (l1.temperatura == null && l2.temperatura == null) return 0;
        if (l1.temperatura == null) return 1;  // i null finiscono in fondo
        if (l2.temperatura == null) return -1;
        return Double.compare(l2.temperatura, l1.temperatura); // decrescente
    });

    System.out.println("Ordine decrescente in base alla temperatura");
    for (LetturaSensore l : lettureValide) {
        System.out.println(l);
    }
}
