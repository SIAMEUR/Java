package fr.idmc.frontend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

// Centralise la configuration du frontend.
// Ordre de priorite pour chaque valeur :
//   1. variable d'environnement (en majuscules, points -> underscores)
//   2. cle dans frontend.properties (embarque dans le jar)
//   3. valeur par defaut codee en dur
public final class FrontendConfig {

    private static final Logger log = LoggerFactory.getLogger(FrontendConfig.class);
    private static final Properties PROPS = chargerProperties();

    private FrontendConfig() {}

    public static final String BROKER_URL =
            get("mqtt.broker.url", "tcp://localhost:1883");

    public static final int QOS =
            Integer.parseInt(get("mqtt.qos", "1"));

    // Suffixe horodate pour garantir l'unicite cote broker MQTT.
    public static final String CLIENT_ID =
            get("mqtt.client.id.prefix", "frontend-") + System.currentTimeMillis();

    public static final String TOPIC_COMMANDES =
            get("mqtt.topic.commandes", "lunettes/commandes");

    public static final String TOPIC_VERIFICATION =
            get("mqtt.topic.verification", "lunettes/verification");

    private static final String TOPIC_LIVRAISON_PREFIX =
            get("mqtt.topic.livraison.prefix", "lunettes/livraisons/");

    private static final String TOPIC_VERIF_RESULT_PREFIX =
            get("mqtt.topic.verification.result.prefix", "lunettes/verification/result/");

    public static String topicLivraison(String clientId) {
        return TOPIC_LIVRAISON_PREFIX + clientId;
    }

    public static String topicVerificationResult(String clientId) {
        return TOPIC_VERIF_RESULT_PREFIX + clientId;
    }

    private static Properties chargerProperties() {
        Properties p = new Properties();
        try (InputStream in = FrontendConfig.class.getResourceAsStream("/frontend.properties")) {
            if (in == null) {
                log.warn("frontend.properties introuvable, utilisation des valeurs par defaut");
                return p;
            }
            p.load(in);
        } catch (IOException e) {
            log.error("Erreur lors du chargement de frontend.properties", e);
        }
        return p;
    }

    private static String get(String cle, String defaut) {
        // Convention : mqtt.broker.url -> MQTT_BROKER_URL pour l'env var
        String envKey = cle.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return PROPS.getProperty(cle, defaut);
    }
}
