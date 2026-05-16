package fr.idmc.frontend.service;

import fr.idmc.frontend.config.FrontendConfig;
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

// Encapsule toute la com MQTT du frontend.
// Les controllers passent par ici, pas par Paho directement.
//
// On envoie et recoit des String : la conversion vers le format maison
// se fait dans MessageSerializer cote appelant.
public class MqttService implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);

    private final MqttClient client;

    // topic -> fonction a appeler quand un message arrive sur ce topic.
    // ConcurrentHashMap parce que Paho declenche les callbacks depuis son propre thread.
    private final Map<String, Consumer<String>> handlers = new ConcurrentHashMap<>();

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

    // Publie un payload texte (deja serialise par le caller) sur un topic.
    public void publier(String topic, String payload) throws MqttException {
        MqttMessage msg = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(FrontendConfig.QOS);
        client.publish(topic, msg);
        log.debug("Publie sur [{}] : {}", topic, payload);
    }

    public void abonner(String topic, Consumer<String> handler) throws MqttException {
        // On enregistre le handler avant de subscribe, sinon un message qui arrive
        // dans la foulee ne trouverait pas son handler.
        handlers.put(topic, handler);
        client.subscribe(topic, FrontendConfig.QOS);
        log.info("Abonne a [{}]", topic);
    }

    // A appeler quand on n'a plus besoin d'ecouter (ex : fin d'une commande).
    // Evite que la map des handlers grossisse a l'infini.
    public void desabonner(String topic) {
        try {
            client.unsubscribe(topic);
            handlers.remove(topic);
            log.debug("Desabonne de [{}]", topic);
        } catch (MqttException e) {
            log.error("Erreur de desabonnement de [{}]", topic, e);
        }
    }

    // Callback Paho : un message vient d'arriver sur un topic auquel on est abonne.
    // Attention : on est dans un thread Paho, pas dans le thread UI JavaFX.
    // Toute mise a jour de l'interface depuis le handler doit passer par Platform.runLater.
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
    public void connectionLost(Throwable cause) {
        // setAutomaticReconnect(true) gere la reconnexion, on log juste pour info.
        log.warn("Connexion perdue : {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.trace("Delivery confirmee : {}", token.getMessageId());
    }
}
