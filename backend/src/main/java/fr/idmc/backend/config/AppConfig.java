package fr.idmc.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    // ─────────────────────────────────────────────────────────────
    private static final Properties props = charger();

    private static Properties charger() {
        Properties p = new Properties();

        // 1. Cherche d'abord à côté du JAR (répertoire courant d'exécution)
        Path externe = Paths.get("config.properties");
        if (Files.exists(externe)) {
            try (InputStream in = Files.newInputStream(externe)) {
                p.load(in);
                log.info("Configuration chargée depuis : {}", externe.toAbsolutePath());
                return p;
            } catch (IOException e) {
                log.warn("Impossible de lire {} : {}", externe, e.getMessage());
            }
        }

        // 2. Fallback : cherche dans le classpath (resources/)
        try (InputStream in = AppConfig.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (in != null) {
                p.load(in);
                log.info("Configuration chargée depuis le classpath.");
                return p;
            }
        } catch (IOException e) {
            log.warn("Impossible de lire config.properties depuis le classpath : {}",
                    e.getMessage());
        }

        // 3. Aucun fichier trouvé → valeurs par défaut
        log.warn("config.properties introuvable — valeurs par défaut utilisées.");
        return p;
    }



    private static String get(String cle, String defaut) {
        return props.getProperty(cle, defaut);
    }

    private static int getInt(String cle, int defaut) {
        try {
            return Integer.parseInt(props.getProperty(cle, String.valueOf(defaut)));
        } catch (NumberFormatException e) {
            log.warn("Valeur invalide pour '{}', défaut utilisé : {}", cle, defaut);
            return defaut;
        }
    }



    public static String BROKER_URL() {
        return get("mqtt.broker.url", "tcp://localhost:1883");
    }

    public static String CLIENT_ID() {
        return get("mqtt.client.id", "usine-serveur");
    }

    public static int QOS() {
        return getInt("mqtt.qos", 1);
    }

    public static String TOPIC_ORDERS_WILDCARD() {
        return get("mqtt.topic.orders", "orders/+");
    }

    public static String TOPIC_SERIALS_WILDCARD() {
        return get("mqtt.topic.serials.check", "serials/+/check");
    }

    public static int POOL_SIZE() {
        return getInt("usine.pool.size", 10);
    }



    public static String topicValidated(String commandeId) {
        return "orders/" + commandeId + "/validated";
    }

    public static String topicCancelled(String commandeId) {
        return "orders/" + commandeId + "/cancelled";
    }

    public static String topicDelivery(String commandeId) {
        return "orders/" + commandeId + "/delivery";
    }

    public static String topicError(String commandeId) {
        return "orders/" + commandeId + "/error";
    }

    public static String topicStatus(String commandeId) {
        return "orders/" + commandeId + "/status";
    }

    public static String topicSerialResult(String serial) {
        return "serials/" + serial;
    }
}