package Es2;

public class RichiestaHttp {
    public String ip;
    public String path;
    public int statusCode;
    public long tempoRispostaMs;
    public long timestamp;

    public RichiestaHttp(String ip, String path, int statusCode, long tempoRispostaMs, long timestamp) {
        this.ip = ip;
        this.path = path;
        this.statusCode = statusCode;
        this.tempoRispostaMs = tempoRispostaMs;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "[" + ip + "] " + path + " -> " + statusCode + " (" + tempoRispostaMs + "ms)";
    }
}