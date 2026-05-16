package fr.idmc.frontend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.model.MessageDto;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
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

// Encapsule toute la com MQTT. Les controllers passent par ici, pas par Paho directement.
public class MqttService implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final MqttClient client;
    // setSerializationInclusion(NON_NULL) : on n'envoie pas les champs null dans le JSON.
    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    // topic -> fonction a appeler quand un message arrive sur ce topic.
    // ConcurrentHashMap parce que Paho declenche les callbacks depuis son propre thread.
    private final Map<String, Consumer<MessageDto>> handlers = new ConcurrentHashMap<>();

    public MqttService() throws MqttException {
        this.client = new MqttClient(FrontendConfig.BROKER_URL, FrontendConfig.CLIENT_ID);
        this.client.setCallback(this);
    }

    public void connecter() throws MqttException {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(true);
        opts.setKeepAliveInterval(30);

        client.connect(opts);
        log.info("Connecte au broker : {}", FrontendConfig.BROKER_URL);
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

    public void publier(String topic, MessageDto dto) throws MqttException {
        try {
            byte[] payload = mapper.writeValueAsBytes(dto);
            MqttMessage msg = new MqttMessage(payload);
            msg.setQos(FrontendConfig.QOS);
            client.publish(topic, msg);
            log.debug("Publie sur [{}]", topic);
        } catch (Exception e) {
            log.error("Erreur de publication sur [{}]", topic, e);
            throw new MqttException(e);
        }
    }

    public void abonner(String topic, Consumer<MessageDto> handler) throws MqttException {
        // On enregistre le handler avant de subscribe, sinon un message qui arrive
        // dans la foulee ne trouverait pas son handler.
        handlers.put(topic, handler);
        client.subscribe(topic, FrontendConfig.QOS);
        log.info("Abonne a [{}]", topic);
    }

    // Callback Paho : un message vient d'arriver sur un topic auquel on est abonne.
    // Attention : on est dans un thread Paho, pas dans le thread UI JavaFX.
    // Toute mise a jour de l'interface depuis le handler doit passer par Platform.runLater.
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        log.debug("Recu sur [{}] : {}", topic, payload);

        Consumer<MessageDto> handler = handlers.get(topic);
        if (handler == null) {
            log.warn("Aucun handler pour [{}]", topic);
            return;
        }

        try {
            MessageDto dto = mapper.readValue(payload, MessageDto.class);
            handler.accept(dto);
        } catch (Exception e) {
            log.error("Erreur de deserialisation pour [{}]", topic, e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        // setAutomaticReconnect(true) gere la reconnexion, on log juste pour info.
        log.warn("Connexion perdue : {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.trace("Delivery confirmee : {}", token.getMessageId());
    }
}
