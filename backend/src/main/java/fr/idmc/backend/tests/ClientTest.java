package fr.idmc.backend.tests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String CLIENT_ID  = "client-test-" + UUID.randomUUID();
    private static final ObjectMapper mapper = new ObjectMapper();

    // Serials récupérés au test 1, réutilisés au test 3
    private static final List<String> serialsRecus = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Client Test — Lunettes Connectées");
        System.out.println("ClientId : " + CLIENT_ID + "\n");

        MqttClient client = new MqttClient(BROKER_URL, CLIENT_ID, new MemoryPersistence());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setAutomaticReconnect(false);
        client.connect(opts);
        System.out.println("[OK] Connecté au broker : " + BROKER_URL);

        // ── Tests dans l'ordre ──────────────────────────────────────────

        testerCommande(client);          // TEST 1 : commande valide → remplit serialsRecus
        testerCommandeInvalide(client);  // TEST 2 : type inexistant → erreur attendue
        testerCommandeVide(client);      // TEST 3 : liste vide → erreur attendue
        testerSansClientId(client);      // TEST 4 : clientId absent → erreur attendue
        testerVerificationValide(client);  // TEST 5 : vrais serials du test 1 → valide=true
        testerVerificationInvalide(client); // TEST 6 : serials forgés → valide=false
        testerVerificationVidee(client);   // TEST 7 : serial vide → valide=false

        client.disconnect();
        System.out.println("\n[OK] Déconnecté. Tous les tests terminés.");
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 1 — Commande valide : 1 CLAUDE + 2 BANANA
    //          On stocke les serials reçus pour les tests de vérification
    // ══════════════════════════════════════════════════════════════════

    private static void testerCommande(MqttClient client) throws Exception {
        afficherTitre("TEST 1 : Commande valide (1 CLAUDE + 2 BANANA)");

        Map<String, Object> payload = Map.of(
                "clientId",   CLIENT_ID,
                "commandeId", UUID.randomUUID().toString(),
                "lunettes", List.of(
                        Map.of("type", "CLAUDE", "quantity", 1),
                        Map.of("type", "BANANA", "quantity", 2)
                )
        );

        String json = mapper.writeValueAsString(payload);
        System.out.println("[>>] Envoi : " + json);

        String topicReponse = "lunettes/livraisons/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] Livraison reçue : " + reponse);

            Map<?, ?> rep = mapper.readValue(reponse, Map.class);
            System.out.println("     Statut      : " + rep.get("statut"));
            System.out.println("     CommandeId  : " + rep.get("commandeId"));

            // Récupération des serials pour les tests de vérification
            List<?> serials = (List<?>) rep.get("numerosSerie");
            if (serials != null) {
                serials.forEach(s -> serialsRecus.add(s.toString()));
                System.out.println("     NumérosSérie: " + serialsRecus);
                System.out.println("     → " + serialsRecus.size() + " serial(s) stocké(s) pour test 5");
            }

            latch.countDown();
        });

        publier(client, "lunettes/commandes", json);

        // 60s car fabrication ~2-3s/lunette + cycles usine
        boolean recu = latch.await(60, TimeUnit.SECONDS);
        afficherResultat(recu);
        client.unsubscribe(topicReponse);
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 2 — Commande invalide : type "SOLAIRE" inconnu
    //          Attendu : statut=ERREUR
    // ══════════════════════════════════════════════════════════════════

    private static void testerCommandeInvalide(MqttClient client) throws Exception {
        afficherTitre("TEST 2 : Type inconnu (SOLAIRE)");

        Map<String, Object> payload = Map.of(
                "clientId",   CLIENT_ID,
                "commandeId", UUID.randomUUID().toString(),
                "lunettes", List.of(
                        Map.of("type", "SOLAIRE", "quantity", 1)
                )
        );

        attendreReponseCommande(client, payload, 10);
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 3 — Commande avec liste vide
    //          Attendu : statut=ERREUR
    // ══════════════════════════════════════════════════════════════════

    private static void testerCommandeVide(MqttClient client) throws Exception {
        afficherTitre("TEST 3 : Liste lunettes vide");

        Map<String, Object> payload = Map.of(
                "clientId",   CLIENT_ID,
                "commandeId", UUID.randomUUID().toString(),
                "lunettes",   List.of()   // liste vide
        );

        attendreReponseCommande(client, payload, 10);
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 4 — Commande sans clientId
    //          Attendu : pas de réponse routable (clientId null)
    // ══════════════════════════════════════════════════════════════════

    private static void testerSansClientId(MqttClient client) throws Exception {
        afficherTitre("TEST 4 : Sans clientId");

        // On ne peut pas s'abonner à la réponse car clientId est absent
        // Le serveur loggera "Réponse sans clientId, publication impossible"
        Map<String, Object> payload = Map.of(
                "commandeId", UUID.randomUUID().toString(),
                "lunettes", List.of(
                        Map.of("type", "CLAUDE", "quantity", 1)
                )
        );

        String json = mapper.writeValueAsString(payload);
        System.out.println("[>>] Envoi sans clientId : " + json);
        publier(client, "lunettes/commandes", json);

        // On attend 3s pour laisser le serveur logger l'erreur
        Thread.sleep(3000);
        System.out.println("     → Vérifier les logs serveur : 'Réponse sans clientId'");
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 5 — Vérification avec les VRAIS serials du test 1
    //          Attendu : valide=true pour chaque serial
    // ══════════════════════════════════════════════════════════════════

    private static void testerVerificationValide(MqttClient client) throws Exception {
        afficherTitre("TEST 5 : Vérification serials valides (issus du test 1)");

        if (serialsRecus.isEmpty()) {
            System.out.println("[!!] Aucun serial reçu au test 1 — test ignoré.");
            return;
        }

        for (String serial : serialsRecus) {
            System.out.println("\n     → Vérification de : " + serial);
            envoyerVerification(client, serial, true);  // attendu : valide=true
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 6 — Vérification avec des serials forgés
    //          Attendu : valide=false
    // ══════════════════════════════════════════════════════════════════

    private static void testerVerificationInvalide(MqttClient client) throws Exception {
        afficherTitre("TEST 6 : Vérification serials forgés (valide=false attendu)");

        // Serial au bon format mais CRC faux
        envoyerVerification(client, "CL-ABC123-0000", false);

        // Serial d'un type valide mais complètement inventé
        envoyerVerification(client, "BA-XXXXXXX-FFFF", false);

        // Serial d'un type inconnu
        envoyerVerification(client, "XX-123456-ABCD", false);
    }

    // ══════════════════════════════════════════════════════════════════
    // TEST 7 — Vérification avec serial vide ou null
    //          Attendu : valide=false
    // ══════════════════════════════════════════════════════════════════

    private static void testerVerificationVidee(MqttClient client) throws Exception {
        afficherTitre("TEST 7 : Vérification serial vide");
        envoyerVerification(client, "", false);
    }

    // ══════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════

    /**
     * Envoie une vérification et attend la réponse.
     * @param attenduValide true si on s'attend à valide=true, pour afficher PASS/FAIL
     */
    private static void envoyerVerification(MqttClient client,
                                            String numeroSerie,
                                            boolean attenduValide) throws Exception {
        System.out.println("[>>] Vérification : '" + numeroSerie + "'");

        Map<String, Object> payload = Map.of(
                "clientId",    CLIENT_ID,
                "numeroSerie", numeroSerie
        );

        String json = mapper.writeValueAsString(payload);
        String topicReponse = "lunettes/verification/result/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            Map<?, ?> rep  = mapper.readValue(reponse, Map.class);

            boolean valide      = Boolean.TRUE.equals(rep.get("valide"));
            String typeLunette  = (String) rep.get("typeLunette");
            boolean pass        = valide == attenduValide;

            System.out.println("[<<] Réponse     : " + reponse);
            System.out.println("     Valide      : " + valide);
            System.out.println("     TypeLunette : " + typeLunette);
            System.out.println("     Résultat    : " + (pass ? "✓ PASS" : "✗ FAIL")
                    + " (attendu valide=" + attenduValide + ")");

            latch.countDown();
        });

        publier(client, "lunettes/verification", json);

        boolean recu = latch.await(10, TimeUnit.SECONDS);
        if (!recu) System.out.println("[!!] Timeout — pas de réponse.");

        client.unsubscribe(topicReponse);
    }

    /**
     * Envoie une commande et affiche la réponse (statut + erreur si présente).
     */
    private static void attendreReponseCommande(MqttClient client,
                                                Map<String, Object> payload,
                                                int timeoutSec) throws Exception {
        String json = mapper.writeValueAsString(payload);
        System.out.println("[>>] Envoi : " + json);

        String topicReponse = "lunettes/livraisons/" + CLIENT_ID;
        CountDownLatch latch = new CountDownLatch(1);

        client.subscribe(topicReponse, 1, (topic, message) -> {
            String reponse = new String(message.getPayload(), StandardCharsets.UTF_8);
            System.out.println("[<<] Réponse : " + reponse);

            Map<?, ?> rep = mapper.readValue(reponse, Map.class);
            System.out.println("     Statut  : " + rep.get("statut"));
            System.out.println("     Erreur  : " + rep.get("erreur"));

            latch.countDown();
        });

        publier(client, "lunettes/commandes", json);

        boolean recu = latch.await(timeoutSec, TimeUnit.SECONDS);
        afficherResultat(recu);
        client.unsubscribe(topicReponse);
    }

    private static void publier(MqttClient client, String topic, String json)
            throws MqttException {
        MqttMessage msg = new MqttMessage(json.getBytes(StandardCharsets.UTF_8));
        msg.setQos(1);
        client.publish(topic, msg);
    }

    private static void afficherTitre(String titre) {
        System.out.println("\n┌─────────────────────────────────────────");
        System.out.println("│ " + titre);
        System.out.println("└─────────────────────────────────────────");
    }

    private static void afficherResultat(boolean recu) {
        if (!recu) System.out.println("[!!] Timeout — pas de réponse du serveur.");
        else       System.out.println("[OK] Réponse reçue.");
    }
}