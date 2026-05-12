package fr.idmc.backend.config;

public class AppConfig {

    // MQTT
    public static final String BROKER_URL = System.getenv()
            .getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");

    public static final String CLIENT_ID = "usine-serveur";
    public static final int QOS = 1;

    // Topics
    public static final String TOPIC_COMMANDES    = "lunettes/commandes";
    public static final String TOPIC_VERIFICATION = "lunettes/verification";

    public static String topicLivraison(String clientId) {
        return "lunettes/livraisons/" + clientId;
    }
    public static String topicVerificationResult(String clientId) {
        return "lunettes/verification/result/" + clientId;
    }

    // Pool de threads de l'Usine
    public static final int POOL_SIZE = 10;
}
