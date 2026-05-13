package fr.idmc.backend.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    private static final String BROKER_URL  = "tcp://localhost:1883";
    private static final String CLIENT_ID   = "client-test-" + UUID.randomUUID();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        System.out.println("=== Client Test Lunettes Connectées ===");
        System.out.println("ClientId : " + CLIENT_ID);

        // ─────────────────────────────────────
        // Connexion au broker
        // ─────────────────────────────────────

        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());

        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(false);
        client.connect(opts);
        System.out.println("[OK] Connecté au broker : " + BROKER_URL);

        // ─────────────────────────────────────
        // TEST 1 : Commande valide
        // ─────────────────────────────────────

        testerCommande(client);

        // ─────────────────────────────────────
        // TEST 2 : Commande invalide (type inconnu)
        // ─────────────────────────────────────

        testerCommandeInvalide(client);

        // ─────────────────────────────────────
        // TEST 3 : Vérification d'un serial
        //          (après avoir reçu la livraison du test 1)
        // ─────────────────────────────────────

        testerVerification(client);

        // ─────────────────────────────────────
        // Déconnexion
        // ─────────────────────────────────────

        client.disconnect();
        System.out.println("\n[OK] Déconnecté. Tests terminés.");
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 1 — Commande valide : 1 CLAUDE + 2 BANANA
    // ─────────────────────────────────────────────────────────────

    private static void testerCommande(MqttClient client) throws Exception {
        System.out.println("\n--- TEST 1 : Commande valide ---");

        // Payload JSON qui reflète exactement la structure attendue
        Map<String, Object> payload = Map.of(
                "clientId",   CLIENT_ID,
                "commandeId", UUID.randomUUID().toString(),
                "lunettes", List.of(
                        Map.of("type", "CLAUDE",  "quantity", 1),
                        Map.of("type", "BANANA",  "quantity", 2)
                )
        );

        String json = mapper.writeValueAsString(payload);
        System.out.println("[>>] Envoi commande : " + json);

        // On attend la réponse sur lunettes/livraisons/{clientId}
        String topicReponse = "lunettes/livraisons/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] Livraison reçue : " + reponse);

            // Parse et affichage lisible
            Map<?, ?> rep = mapper.readValue(reponse, Map.class);
            System.out.println("     Statut      : " + rep.get("statut"));
            System.out.println("     CommandeId  : " + rep.get("commandeId"));
            System.out.println("     NumérosSérie: " + rep.get("numerosSerie"));

            latch.countDown();
        });

        // Publication de la commande
        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        client.publish("lunettes/commandes", msg);

        // Attente de la réponse (60s max car la fabrication prend ~2-3s par lunette)
        boolean recu = latch.await(60, TimeUnit.SECONDS);
        if (!recu) System.out.println("[!!] Timeout — pas de réponse du serveur.");

        client.unsubscribe(topicReponse);
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 2 — Commande invalide : type "SOLAIRE" inexistant
    // ─────────────────────────────────────────────────────────────

    private static void testerCommandeInvalide(MqttClient client) throws Exception {
        System.out.println("\n--- TEST 2 : Commande invalide (type inconnu) ---");

        Map<String, Object> payload = Map.of(
                "clientId",   CLIENT_ID,
                "commandeId", UUID.randomUUID().toString(),
                "lunettes", List.of(
                        Map.of("type", "SOLAIRE", "quantity", 1)  // type inexistant
                )
        );

        String json = mapper.writeValueAsString(payload);
        System.out.println("[>>] Envoi commande invalide : " + json);

        String topicReponse = "lunettes/livraisons/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] Réponse reçue : " + reponse);

            Map<?, ?> rep = mapper.readValue(reponse, Map.class);
            System.out.println("     Statut : " + rep.get("statut"));
            System.out.println("     Erreur : " + rep.get("erreur"));

            latch.countDown();
        });

        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        client.publish("lunettes/commandes", msg);

        boolean recu = latch.await(10, TimeUnit.SECONDS);
        if (!recu) System.out.println("[!!] Timeout — pas de réponse du serveur.");

        client.unsubscribe(topicReponse);
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 3 — Vérification de serial
    //          valide : un serial forgé à la main pour tester les deux cas
    // ─────────────────────────────────────────────────────────────

    private static void testerVerification(MqttClient client) throws Exception {
        System.out.println("\n--- TEST 3 : Vérification de serial ---");

        // 3a. Serial inconnu (forgé)
        envoyerVerification(client, "CL-FAKESERIAL-0000");

        // 3b. Serial vide
        envoyerVerification(client, "");
    }

    private static void envoyerVerification(MqttClient client,
                                            String numeroSerie) throws Exception {
        System.out.println("\n[>>] Vérification serial : '" + numeroSerie + "'");

        Map<String, Object> payload = Map.of(
                "clientId",    CLIENT_ID,
                "numeroSerie", numeroSerie
        );

        String json = mapper.writeValueAsString(payload);

        String topicReponse = "lunettes/verification/result/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] Résultat vérification : " + reponse);

            Map<?, ?> rep = mapper.readValue(reponse, Map.class);
            System.out.println("     Valide      : " + rep.get("valide"));
            System.out.println("     TypeLunette : " + rep.get("typeLunette"));

            latch.countDown();
        });

        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        client.publish("lunettes/verification", msg);

        boolean recu = latch.await(10, TimeUnit.SECONDS);
        if (!recu) System.out.println("[!!] Timeout — pas de réponse.");

        client.unsubscribe(topicReponse);
    }
}