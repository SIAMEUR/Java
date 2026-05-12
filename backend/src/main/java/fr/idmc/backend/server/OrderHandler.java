package fr.idmc.backend.server;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.idmc.backend.serialization.LunettesException;
import fr.idmc.backend.serialization.Message;
import fr.idmc.factory.Usine;




import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderHandler {

     private static final Logger log = LoggerFactory.getLogger(OrderHandler.class);

    private final Usine usine;
    private final ValidateurCommande validateur;
    private final ObjectMapper mapper = new ObjectMapper();

    // Stockage thread-safe des serials pour vérification
    private final Set<String> tousLesSerials =
            Collections.newSetFromMap(new ConcurrentHashMap<>());

    public OrderHandler(Usine usine, ValidateurCommande validateur) {
        this.usine     = usine;
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
            log.warn("Commande invalide [{}] : {}", 400, e.getMessage());
            return CompletableFuture.completedFuture(
                    creerErreur(entrant.getClientId(), entrant.getCommandeId(), e.getMessage())
            );
        }

        // 3. Conversion Message → Commande interne
        Commande commande = Commande.depuisMessage(entrant);

        // 4. Envoi à l'Usine (async)
        return usine.traiterCommande(commande)
                .thenApply(serials -> {
                    tousLesSerials.addAll(serials);

                    Message rep = new Message();
                    rep.setType("LIVRAISON");
                    rep.setClientId(commande.getClientId());
                    rep.setCommandeId(commande.getCommandeId());
                    rep.setNumerosSerie(serials);
                    rep.setStatut("OK");
                    return rep;
                })
                .exceptionally(ex -> {
                    log.error("Fabrication échouée", ex);
                    return creerErreur(
                            commande.getClientId(),
                            commande.getCommandeId(),
                            "Fabrication échouée : " + ex.getMessage()
                    );
                });
    }

    // ─────────────────────────────────────
    // Vérification d'un numéro de série
    // ─────────────────────────────────────

    public Message traiterVerification(String payloadJson) {
        try {
            Message entrant = mapper.readValue(payloadJson, Message.class);
            boolean valide  = tousLesSerials.contains(entrant.getNumeroSerie());

            Message rep = new Message();
            rep.setType("VERIFICATION_RESULT");
            rep.setClientId(entrant.getClientId());
            rep.setNumeroSerie(entrant.getNumeroSerie());
            rep.setValide(valide);
            rep.setStatut("OK");
            return rep;

        } catch (Exception e) {
            log.error("Erreur vérification", e);
            return creerErreur(null, null, "Vérification impossible : " + e.getMessage());
        }
    }

    // ─────────────────────────────────────
    // Helper
    // ─────────────────────────────────────

    private Message creerErreur(String clientId, String commandeId, String texte) {
        Message m = new Message();
        m.setType("ERREUR");
        m.setClientId(clientId);
        m.setCommandeId(commandeId);
        m.setStatut("ERREUR");
        m.setErreur(texte);
        return m;
    }
}