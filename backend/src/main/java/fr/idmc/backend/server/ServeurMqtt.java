package fr.idmc.backend.server;

import bernard_flou.Fabricateur;
import fr.idmc.backend.config.AppConfig;
import fr.idmc.backend.serialization.MessageSerializer;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ServeurMqtt implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(ServeurMqtt.class);

    private MqttClient client;
    private final OrderHandler orderHandler;

    public ServeurMqtt(OrderHandler orderHandler) {
        this.orderHandler = orderHandler;
    }

    // ─────────────────────────────────────
    // Démarrage
    // ─────────────────────────────────────

    public void demarrer() throws MqttException {
        client = new MqttClient(AppConfig.BROKER_URL(), AppConfig.CLIENT_ID());
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(30);

        client.connect(opts);
        log.info("Connecté au broker : {}", AppConfig.BROKER_URL());

        client.subscribe(AppConfig.TOPIC_ORDERS_WILDCARD(),  AppConfig.QOS());
        client.subscribe(AppConfig.TOPIC_SERIALS_WILDCARD(), AppConfig.QOS());
    }

    public void arreter() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            log.info("Déconnecté proprement.");
        }
    }

    // ─────────────────────────────────────
    // Réception et routage
    // ─────────────────────────────────────

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("Reçu sur [{}] : {}", topic, payload);

        if (topic.startsWith("orders/")) {
            // Extrait le commandeId depuis le topic "orders/{commandeId}"
            String commandeId = topic.split("/")[1];
            gererCommande(commandeId, payload);

        } else if (topic.startsWith("serials/") && topic.endsWith("/check")) {
            // Extrait le serial depuis "serials/{serial}/check"
            String[] parts = topic.split("/");
            // Le serial peut contenir des '/' → on recompose
            // Format garanti : serials/SERIAL/check → parts[1] = serial
            String serial = parts[1];
            gererVerification(serial);

        } else {
            log.warn("Topic non géré : {}", topic);
        }
    }

    // ─────────────────────────────────────
    // Gestion d'une commande
    // ─────────────────────────────────────

    private void gererCommande(String commandeId, String payload) {

        // Publie validated ou cancelled selon le résultat de la validation
        // puis lance la fabrication de manière asynchrone
        orderHandler.traiterCommande(commandeId, payload,

                // onValidated : la commande est valide
                () -> {
                    publier(AppConfig.topicValidated(commandeId), "");
                    publierStatus(commandeId, "EN_ATTENTE");
                },

                // onCancelled : la commande est invalide
                reason -> publier(AppConfig.topicCancelled(commandeId),
                        MessageSerializer.serializeErreur(commandeId, reason)),

                // onStatus : mise à jour de progression (bonus)
                status -> publierStatus(commandeId, status),

                // onDelivery : livraison finale
                lunettes -> publier(AppConfig.topicDelivery(commandeId),
                        MessageSerializer.serializeDelivery(commandeId, lunettes)),

                // onError : erreur pendant la fabrication
                reason -> publier(AppConfig.topicError(commandeId),
                        MessageSerializer.serializeErreur(commandeId, reason))
        );
    }

    // ─────────────────────────────────────
    // Gestion d'une vérification de serial
    // ─────────────────────────────────────

    private void gererVerification(String serial) {
        // validateSerial retourne le TypeLunette si valide, null sinon
        Fabricateur.TypeLunette type = Fabricateur.validateSerial(serial);
        String result = (type != null) ? type.name() : "invalid";

        String payload = MessageSerializer.serializeSerialResult(serial, result);
        publier(AppConfig.topicSerialResult(serial), payload);

        log.info("Vérification serial [{}] → {}", serial, result);
    }

    // ─────────────────────────────────────
    // Helpers de publication
    // ─────────────────────────────────────

    private void publierStatus(String commandeId, String status) {
        publier(AppConfig.topicStatus(commandeId),
                MessageSerializer.serializeStatus(commandeId, status));
    }

    private void publier(String topic, String payload) {
        try {
            MqttMessage msg = new MqttMessage(
                    payload.getBytes(StandardCharsets.UTF_8));
            msg.setQos(AppConfig.QOS());
            msg.setRetained(false);
            client.publish(topic, msg);
            log.debug("Publié sur [{}] : {}", topic, payload);
        } catch (MqttException e) {
            log.error("Erreur publication sur [{}]", topic, e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Connexion perdue : {}. Reconnexion auto...", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.trace("Delivery confirmée : {}", token.getMessageId());
    }
}