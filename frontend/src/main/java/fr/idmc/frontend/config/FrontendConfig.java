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

    // Prefixes : on concatene le commandeId ou le serial pour faire le topic final.
    private static final String PREFIX_ORDERS =
            get("mqtt.topic.orders.prefix", "orders/");
    private static final String PREFIX_SERIALS =
            get("mqtt.topic.serials.prefix", "serials/");

    // Topics commandes (cf README du prof : orders/{commandeId}/...)
    public static String topicOrder(String commandeId) {
        return PREFIX_ORDERS + commandeId;
    }
    public static String topicValidated(String commandeId) {
        return PREFIX_ORDERS + commandeId + "/validated";
    }
    public static String topicCancelled(String commandeId) {
        return PREFIX_ORDERS + commandeId + "/cancelled";
    }
    public static String topicDelivery(String commandeId) {
        return PREFIX_ORDERS + commandeId + "/delivery";
    }
    public static String topicError(String commandeId) {
        return PREFIX_ORDERS + commandeId + "/error";
    }
    public static String topicStatus(String commandeId) {
        return PREFIX_ORDERS + commandeId + "/status";
    }

    // Topics verification (cf README : serials/{serial}/check, serials/{serial})
    public static String topicSerialCheck(String serial) {
        return PREFIX_SERIALS + serial + "/check";
    }
    public static String topicSerialResult(String serial) {
        return PREFIX_SERIALS + serial;
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
