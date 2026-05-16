package fr.idmc.frontend.service;

import fr.idmc.frontend.config.FrontendConfig;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

// Encapsule toute la com MQTT du frontend.
// Implemente MqttCallbackExtended pour avoir aussi le callback "connectComplete"
// qui se declenche a la connexion initiale ET aux reconnexions automatiques.
public class MqttService implements MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final MqttClient client;

    // topic -> fonction a appeler quand un message arrive sur ce topic.
    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();

    // Property observable de l'etat de connexion au broker.
    // Mise a jour via Platform.runLater car les callbacks Paho ne sont pas sur le thread UI.
    private final BooleanProperty connecte = new SimpleBooleanProperty(false);

    public MqttService() throws MqttException {
        this.client = new MqttClient(FrontendConfig.BROKER_URL, FrontendConfig.CLIENT_ID);
        this.client.setCallback(this);
    }

    public BooleanProperty connecteProperty() {
        return connecte;
    }

    public void connecter() throws MqttException {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(30);
        // Timeout court pour echouer vite si le broker est down au demarrage.
        opts.setConnectionTimeout(5);

        client.connect(opts);
        log.info("Demande de connexion au broker : {}", FrontendConfig.BROKER_URL);
        // L'etat "connecte" sera passe a true par connectComplete().
    }

    public void deconnecter() {
        try {
            if (client.isConnected()) {
                client.disconnect();
                log.info("Deconnecte du broker");
            }
        } catch (MqttException e) {
            log.error("Erreur lors de la deconnexion", e);
        }
    }

    public void publier(String topic, String payload) throws MqttException {
        MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(FrontendConfig.QOS);
        client.publish(topic, msg);
        log.debug("Publie sur [{}] : {}", topic, payload);
    }

    public void abonner(String topic, Consumer<String> handler) throws MqttException {
        handlers.put(topic, handler);
        client.subscribe(topic, FrontendConfig.QOS);
        log.info("Abonne a [{}]", topic);
    }

    public void desabonner(String topic) {
        try {
            client.unsubscribe(topic);
            handlers.remove(topic);
            log.debug("Desabonne de [{}]", topic);
        } catch (MqttException e) {
            log.error("Erreur de desabonnement de [{}]", topic, e);
        }
    }

    // ── Callbacks MQTT (executes sur le thread Paho) ──

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info(reconnect ? "Reconnecte au broker" : "Connecte au broker : {}", serverURI);
        Platform.runLater(() -> connecte.set(true));
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Connexion perdue : {}", cause.getMessage());
        Platform.runLater(() -> connecte.set(false));
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("Recu sur [{}] : {}", topic, payload);

        Consumer<String> handler = handlers.get(topic);
        if (handler == null) {
            log.warn("Aucun handler pour [{}]", topic);
            return;
        }

        try {
            handler.accept(payload);
        } catch (Exception e) {
            log.error("Erreur dans le handler pour [{}]", topic, e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.trace("Delivery confirmee : {}", token.getMessageId());
    }
}
