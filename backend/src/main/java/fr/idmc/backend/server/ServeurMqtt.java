package fr.idmc.backend.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idmc.backend.config.AppConfig;
import fr.idmc.backend.serialization.Message;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServeurMqtt implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(ServeurMqtt.class);

    private MqttClient client;
    private final OrderHandler orderHandler;
    private final ObjectMapper mapper = new ObjectMapper();

    public ServeurMqtt(OrderHandler orderHandler) {
        this.orderHandler = orderHandler;
        this.orderHandler.setSerialsUpdated(this::publierListeSerials);
    }



    public void demarrer() throws MqttException {
        client = new MqttClient(AppConfig.BROKER_URL, AppConfig.CLIENT_ID);
        client.setCallback(this);

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(30);

        client.connect(opts);
        log.info("Connecté au broker : {}", AppConfig.BROKER_URL);

        client.subscribe(AppConfig.TOPIC_COMMANDES,    AppConfig.QOS);
        client.subscribe(AppConfig.TOPIC_VERIFICATION, AppConfig.QOS);
        log.info("Abonné aux topics commandes et vérification");

        // Publication de l'état initial (liste vide au démarrage)
        publierListeSerials(orderHandler.getTousLesSerials());
    }

    public void arreter() throws MqttException {
        if (client != null && client.isConnected()) {
            client.disconnect();
            log.info("Déconnecté proprement.");
        }
    }

    //Réception et routage

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("Reçu sur [{}] : {}", topic, payload);

        switch (topic) {
            case AppConfig.TOPIC_COMMANDES ->
                    orderHandler.traiterCommande(payload)
                            .thenAccept(rep -> {
                                if (rep.getClientId() == null) {
                                    log.warn("Réponse sans clientId, publication impossible.");
                                    return;
                                }
                                publier(AppConfig.topicLivraison(rep.getClientId()), rep, false);
                            });

            case AppConfig.TOPIC_VERIFICATION -> {
                Message rep = orderHandler.traiterVerification(payload);
                if (rep.getClientId() == null) {
                    log.warn("Vérification sans clientId, publication impossible.");
                    return;
                }
                publier(AppConfig.topicVerificationResult(rep.getClientId()), rep, false);
            }

            default -> log.warn("Topic non géré : {}", topic);
        }
    }


    private void publierListeSerials(List<String> serials) {
        try {
            java.util.Map<String, Object> payload = java.util.Map.of(
                    "serials", serials,
                    "total",   serials.size()
            );
            byte[] json = mapper.writeValueAsBytes(payload);

            MqttMessage msg = new MqttMessage(json);
            msg.setQos(AppConfig.QOS);
            msg.setRetained(true);  // retained : dernier message conservé par le broker
            client.publish(AppConfig.TOPIC_FABRIQUEES, msg);

            log.info("Liste serials publiée sur [{}] : {} serial(s)",
                    AppConfig.TOPIC_FABRIQUEES, serials.size());
        } catch (Exception e) {
            log.error("Erreur publication liste serials", e);
        }
    }



    private void publier(String topic, Object objet, boolean retained) {
        try {
            byte[] json = mapper.writeValueAsBytes(objet);
            MqttMessage msg = new MqttMessage(json);
            msg.setQos(AppConfig.QOS);
            msg.setRetained(retained);
            client.publish(topic, msg);
            log.debug("Publié sur [{}]", topic);
        } catch (Exception e) {
            log.error("Erreur lors de la publication sur [{}]", topic, e);
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