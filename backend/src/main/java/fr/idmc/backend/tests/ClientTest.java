package fr.idmc.backend.tests;

import bernard_flou.Fabricateur;
import fr.idmc.backend.serialization.MessageSerializer;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    private static final String BROKER_URL  = "tcp://localhost:1883";
    // UUID garanti unique dans l'univers entier
    private static final String COMMANDE_ID = UUID.randomUUID().toString();
    private static final String CLIENT_ID   = "client-test-" + UUID.randomUUID();

    // Serials récupérés à la livraison, pour les tester ensuite
    private static final List<String> serialsRecus = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║  Client Test — Lunettes Connectées   ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("CommandeId : " + COMMANDE_ID);

        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID,
                new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        client.connect(opts);
        System.out.println("[OK] Connecté : " + BROKER_URL);

        testerCommandeValide(client);
        testerCommandeInvalide(client);
        testerVerificationValide(client);
        testerVerificationInvalide(client);

        client.disconnect();
        System.out.println("\n[OK] Tests terminés.");
    }

    // ══════════════════════════════════════════════════════════
    // TEST 1 — Commande valide
    // ══════════════════════════════════════════════════════════

    private static void testerCommandeValide(MqttClient client) throws Exception {
        afficherTitre("TEST 1 : Commande valide (CLAUDE:1, BANANA:2)");

        String commandeId = UUID.randomUUID().toString();

        // Payload format custom
        Map<String, Integer> lunettes = new LinkedHashMap<>();
        lunettes.put("CLAUDE", 1);
        lunettes.put("BANANA", 2);
        String payload = MessageSerializer.serializeCommande(commandeId, lunettes);
        System.out.println("[>>] Payload : " + payload);

        // Latch pour attendre la livraison finale
        CountDownLatch latchDelivery = new CountDownLatch(1);

        // S'abonner à tous les sous-topics de cette commande
        String topicBase = "orders/" + commandeId + "/#";
        client.subscribe(topicBase, 1, (topic, message) -> {
            String rep = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] " + topic + " → " + rep);

            if (topic.endsWith("/validated")) {
                System.out.println("     ✓ Commande validée");

            } else if (topic.endsWith("/status")) {
                Map<String, String> champs = MessageSerializer.parse(rep);
                System.out.println("     ◷ Statut : " + champs.get("STATUS"));

            } else if (topic.endsWith("/delivery")) {
                Map<String, String> champs = MessageSerializer.parse(rep);
                List<MessageSerializer.LunetteItem> items =
                        MessageSerializer.parseLunettesDelivery(champs.get("LUNETTES"));

                System.out.println("     ✓ Livraison reçue : " + items.size() + " lunettes");
                items.forEach(item -> {
                    System.out.println("       → " + item.type + " | " + item.serial);
                    serialsRecus.add(item.serial);  // stocké pour test 3
                });

                latchDelivery.countDown();

            } else if (topic.endsWith("/error")) {
                Map<String, String> champs = MessageSerializer.parse(rep);
                System.out.println("     ✗ Erreur : " + champs.get("REASON"));
                latchDelivery.countDown();
            }
        });

        // Publication de la commande sur orders/{commandeId}
        publier(client, "orders/" + commandeId, payload);

        boolean recu = latchDelivery.await(90, TimeUnit.SECONDS);
        afficherResultat(recu);
        client.unsubscribe(topicBase);
    }

    // ══════════════════════════════════════════════════════════
    // TEST 2 — Commande invalide (quantité >= 10)
    // ══════════════════════════════════════════════════════════

    private static void testerCommandeInvalide(MqttClient client) throws Exception {
        afficherTitre("TEST 2 : Commande invalide (quantité >= 10)");

        String commandeId = UUID.randomUUID().toString();

        Map<String, Integer> lunettes = new LinkedHashMap<>();
        lunettes.put("CLAUDE", 15);  // invalide : >= 10
        String payload = MessageSerializer.serializeCommande(commandeId, lunettes);
        System.out.println("[>>] Payload : " + payload);

        CountDownLatch latch = new CountDownLatch(1);
        String topicBase = "orders/" + commandeId + "/#";

        client.subscribe(topicBase, 1, (topic, message) -> {
            String rep = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] " + topic + " → " + rep);

            if (topic.endsWith("/cancelled")) {
                Map<String, String> champs = MessageSerializer.parse(rep);
                System.out.println("     ✓ Commande annulée (attendu) : " + champs.get("REASON"));
                latch.countDown();
            }
        });

        publier(client, "orders/" + commandeId, payload);

        boolean recu = latch.await(10, TimeUnit.SECONDS);
        afficherResultat(recu);
        client.unsubscribe(topicBase);
    }

    // ══════════════════════════════════════════════════════════
    // TEST 3 — Vérification des vrais serials (test 1)
    // ══════════════════════════════════════════════════════════

    private static void testerVerificationValide(MqttClient client) throws Exception {
        afficherTitre("TEST 3 : Vérification serials valides");

        if (serialsRecus.isEmpty()) {
            System.out.println("[!!] Aucun serial reçu au test 1 — ignoré.");
            return;
        }

        for (String serial : serialsRecus) {
            System.out.println("\n[>>] Vérification : " + serial);

            CountDownLatch latch = new CountDownLatch(1);
            // Résultat sur serials/{serial}
            String topicResult = "serials/" + serial;

            client.subscribe(topicResult, 1, (topic, message) -> {
                String rep = new String(message.getPayload(), StandardCharsets.UTF_8);
                Map<String, String> champs = MessageSerializer.parse(rep);
                String result = champs.get("RESULT");

                System.out.println("[<<] " + rep);
                System.out.println("     Résultat : " + result);
                System.out.println("     " + (!"invalid".equals(result) ? "✓ PASS" : "✗ FAIL"));
                latch.countDown();
            });

            // Publication sur serials/{serial}/check (payload vide)
            publier(client, "serials/" + serial + "/check", "");

            boolean recu = latch.await(10, TimeUnit.SECONDS);
            if (!recu) System.out.println("[!!] Timeout.");
            client.unsubscribe(topicResult);
        }
    }

    // ══════════════════════════════════════════════════════════
    // TEST 4 — Vérification serial forgé
    // ══════════════════════════════════════════════════════════

    private static void testerVerificationInvalide(MqttClient client) throws Exception {
        afficherTitre("TEST 4 : Vérification serial forgé");

        String serial = "CL-FAKESERIAL-0000";
        System.out.println("[>>] Vérification : " + serial);

        CountDownLatch latch = new CountDownLatch(1);
        String topicResult = "serials/" + serial;

        client.subscribe(topicResult, 1, (topic, message) -> {
            String rep = new String(message.getPayload(), StandardCharsets.UTF_8);
            Map<String, String> champs = MessageSerializer.parse(rep);
            String result = champs.get("RESULT");

            System.out.println("[<<] " + rep);
            System.out.println("     Résultat : " + result);
            System.out.println("     " + ("invalid".equals(result) ? "✓ PASS" : "✗ FAIL"));
            latch.countDown();
        });

        publier(client, "serials/" + serial + "/check", "");

        boolean recu = latch.await(10, TimeUnit.SECONDS);
        if (!recu) System.out.println("[!!] Timeout.");
        client.unsubscribe(topicResult);
    }

    // ══════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════

    private static void publier(MqttClient client, String topic, String payload)
            throws MqttException {
        MqttMessage msg = new MqttMessage(
                payload.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        client.publish(topic, msg);
        System.out.println("[>>] Publié sur [" + topic + "]");
    }

    private static void afficherTitre(String titre) {

        System.out.println(titre);
        System.out.println("──────────────────────────────────────────");
    }

    private static void afficherResultat(boolean recu) {
        System.out.println(recu ? "[OK] Réponse reçue." : "[!!] Timeout — pas de réponse.");
    }
}