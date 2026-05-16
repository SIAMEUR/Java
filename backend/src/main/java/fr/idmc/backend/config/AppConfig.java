package fr.idmc.backend.config;

public final class AppConfig {

    public static final String BROKER_URL = System.getenv()
            .getOrDefault("MQTT_BROKER_URL", "tcp://localhost:1883");

    public static final String CLIENT_ID = "usine-serveur";
    public static final int    QOS       = 1;

    // ── Topics entrants (client → usine) ──────────────────────────

    // Le client publie sur orders/{commandeId}
    // On s'abonne avec le wildcard + pour recevoir toutes les commandes
    public static final String TOPIC_ORDERS_WILDCARD  = "orders/+";

    // Le client publie sur serials/{serial}/check pour vérifier un serial
    public static final String TOPIC_SERIALS_WILDCARD = "serials/+/check";

    // ── Topics sortants (usine → client) ──────────────────────────

    // orders/{commandeId}/validated  ← commande valide
    public static String topicValidated(String commandeId) {
        return "orders/" + commandeId + "/validated";
    }

    // orders/{commandeId}/cancelled  ← commande invalide
    public static String topicCancelled(String commandeId) {
        return "orders/" + commandeId + "/cancelled";
    }

    // orders/{commandeId}/delivery  ← livraison finale
    public static String topicDelivery(String commandeId) {
        return "orders/" + commandeId + "/delivery";
    }

    // orders/{commandeId}/error  ← erreur pendant le traitement
    public static String topicError(String commandeId) {
        return "orders/" + commandeId + "/error";
    }

    // orders/{commandeId}/status  ← progression (bonus)
    public static String topicStatus(String commandeId) {
        return "orders/" + commandeId + "/status";
    }

    // serials/{serial}  ← résultat de vérification
    public static String topicSerialResult(String serial) {
        return "serials/" + serial;
    }
}