package fr.idmc.backend.server;

import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import fr.idmc.backend.serialization.MessageSerializer;
import fr.idmc.backend.serialization.MessageSerializer.LunetteItem;
import fr.idmc.usine.Usine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderHandler.class);

    private final Usine usine;
    private final ValidateurCommande validateur;

    public OrderHandler(Usine usine, ValidateurCommande validateur) {
        this.usine      = usine;
        this.validateur = validateur;
    }

    /**
     * Traite une commande reçue sur orders/{commandeId}.
     * Les callbacks permettent à ServeurMqtt de publier sur les bons topics
     * sans que OrderHandler connaisse MQTT.
     */
    public void traiterCommande(
            String commandeId,
            String payload,
            Runnable                    onValidated,
            Consumer<String>            onCancelled,
            Consumer<String>            onStatus,
            Consumer<List<LunetteItem>> onDelivery,
            Consumer<String>            onError) {

        // 1. Parse le payload custom
        Map<String, String> champs = MessageSerializer.parse(payload);
        String lunettesRaw = champs.get("LUNETTES");

        if (lunettesRaw == null || lunettesRaw.isBlank()) {
            onCancelled.accept("Champ LUNETTES manquant dans le payload");
            return;
        }

        Map<String, Integer> lunettesStr =
                MessageSerializer.parseLunettesCommande(lunettesRaw);

        // 2. Conversion String → TypeLunette
        Map<TypeLunette, Integer> lunettes = new LinkedHashMap<>();
        try {
            for (Map.Entry<String, Integer> e : lunettesStr.entrySet()) {
                TypeLunette type = TypeLunette.valueOf(e.getKey().toUpperCase());
                lunettes.put(type, e.getValue());
            }
        } catch (IllegalArgumentException e) {
            onCancelled.accept("Type inconnu : " + e.getMessage());
            return;
        }

        // 3. Validation métier
        try {
            validateur.valider(lunettes);
        } catch (Exception e) {
            onCancelled.accept(e.getMessage());
            return;
        }

        // 4. Commande valide → notifie le client
        onValidated.run();
        onStatus.accept("EN_FABRICATION");

        // 5. Fabrication asynchrone (usine.produire est bloquante)
        new Thread(() -> {
            try {
                Commande commande = new Commande(commandeId, lunettes);
                List<Lunette> produites = usine.produire(commande.getLunettes());

                // Conversion Lunette → LunetteItem
                List<LunetteItem> items = produites.stream()
                        .map(l -> new LunetteItem(l.type.name(), l.serial))
                        .collect(Collectors.toList());

                onStatus.accept("LIVRE");
                onDelivery.accept(items);

                log.info("Commande {} livrée : {} lunettes", commandeId, items.size());

            } catch (Exception e) {
                log.error("Erreur fabrication commande {} : {}", commandeId, e.getMessage());
                onError.accept("Erreur fabrication : " + e.getMessage());
            }
        }, "fabrication-" + commandeId).start();
    }
}