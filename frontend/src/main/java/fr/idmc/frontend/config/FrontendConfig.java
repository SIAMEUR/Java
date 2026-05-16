package fr.idmc.frontend.config;

// Constantes partagees par tout le frontend (topics, broker, QoS).
// Si un topic change, on le change ici et pas ailleurs.
public final class FrontendConfig {

    private FrontendConfig() {}

    // Permet d'override l'URL via variable d'env si on change de broker (docker, prod...).
    public static final String BROKER_URL =
            System.getenv().getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");

    // ID unique a chaque lancement, sinon le broker refuse deux clients identiques.
    public static final String CLIENT_ID = "frontend-" + System.currentTimeMillis();

    public static final int QOS = 1;

    public static final String TOPIC_COMMANDES    = "lunettes/commandes";
    public static final String TOPIC_VERIFICATION = "lunettes/verification";

    // Les topics de reponse sont specifiques au client : on les construit dynamiquement.
    public static String topicLivraison(String clientId) {
        return "lunettes/livraisons/" + clientId;
    }

    public static String topicVerificationResult(String clientId) {
        return "lunettes/verification/result/" + clientId;
    }
}
