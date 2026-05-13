package fr.idmc.backend.server;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idmc.backend.serialization.Message;
import fr.idmc.factory.Usine;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderHandler.class);

    private final Usine usine;
    private final ValidateurCommande validateur;
    private final ObjectMapper mapper = new ObjectMapper();

    // Stockage thread-safe des serials produits
    private final Set<String> tousLesSerials =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public OrderHandler(Usine usine, ValidateurCommande validateur) {
        this.usine      = usine;
        this.validateur = validateur;
    }

    // ─────────────────────────────────────
    // Traitement d'une commande
    // ─────────────────────────────────────

    public CompletableFuture<Message> traiterCommande(String payloadJson) {

        // 1. Parse JSON → Message
        Message entrant;
        try {
            entrant = mapper.readValue(payloadJson, Message.class);
        } catch (JsonProcessingException e) {
            log.error("JSON invalide : {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    creerErreur(null, null, "JSON invalide : " + e.getMessage())
            );
        }

        // 2. Validation
        try {
            validateur.valider(entrant);
        } catch (Exception e) {
            log.warn("Commande invalide : {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    creerErreur(entrant.getClientId(), entrant.getCommandeId(), e.getMessage())
            );
        }

        // 3. Message → Commande (List<LunettesItem> → Map<TypeLunette, Integer>)
        Commande commande;
        try {
            commande = Commande.depuisMessage(entrant);
        } catch (IllegalArgumentException e) {
            log.warn("Type de lunette inconnu : {}", e.getMessage());
            return CompletableFuture.completedFuture(
                    creerErreur(entrant.getClientId(), entrant.getCommandeId(),
                            "Type inconnu : " + e.getMessage())
            );
        }

        // appeler produire thread séparé
        return CompletableFuture.supplyAsync(() ->
                        usine.produire(commande.getLunettes())  // List<Lunette>
                )
                .thenApply(lunettes -> {
                    List<String> serials = lunettes.stream()
                            .map(lunette -> lunette.serial)
                            .collect(Collectors.toList());

                    tousLesSerials.addAll(serials);

                    Message rep = new Message();
                    rep.setClientId(commande.getClientId());
                    rep.setCommandeId(commande.getCommandeId());
                    rep.setNumerosSerie(serials);
                    rep.setStatut("OK");
                    return rep;
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log.error("Fabrication échouée pour {} : {}",
                            commande.getCommandeId(), cause.getMessage());
                    return creerErreur(
                            commande.getClientId(),
                            commande.getCommandeId(),
                            "Fabrication échouée : " + cause.getMessage()
                    );
                });
    }

    // ─────────────────────────────────────
    // Vérification d'un numéro de série
    // Utilise Fabricateur.validateSerial() qui retourne
    // le TypeLunette si valide, null sinon
    // ─────────────────────────────────────

    /*public Message traiterVerification(String payloadJson) {
        try {
            Message entrant = mapper.readValue(payloadJson, Message.class);
            String numeroSerie = entrant.getNumeroSerie();

            // validateSerial() vérifie le format ET le CRC du serial
            // retourne TypeLunette si valide, null sinon
            TypeLunette type = Fabricateur.validateSerial(numeroSerie);
            boolean valide   = type != null;

            // Double vérification : le serial doit aussi avoir été produit
            // par cette usine (pas un serial forgé extérieur valide par hasard)
            boolean connuLocalement = tousLesSerials.contains(numeroSerie);

            Message rep = new Message();
            rep.setClientId(entrant.getClientId());
            rep.setNumeroSerie(numeroSerie);
            rep.setValide(valide && connuLocalement);
            rep.setStatut("OK");

            if (valide && connuLocalement) {
                // On renvoie aussi le type pour que le client sache
                // de quel modèle il s'agit
                rep.setTypeLunette(type.name());
            }

            return rep;

        } catch (Exception e) {
            log.error("Erreur vérification", e);
            return creerErreur(null, null, "Vérification impossible : " + e.getMessage());
        }
    }*/

    // ─────────────────────────────────────
    // Helper
    // ─────────────────────────────────────

    private Message creerErreur(String clientId, String commandeId, String texte) {
        Message m = new Message();
        m.setClientId(clientId);
        m.setCommandeId(commandeId);
        m.setStatut("ERREUR");
        m.setErreur(texte);
        return m;
    }
}