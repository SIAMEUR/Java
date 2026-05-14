package fr.idmc.backend.server;

import bernard_flou.Fabricateur.TypeLunette;
import fr.idmc.backend.serialization.LunettesException;
import fr.idmc.backend.serialization.Message;

import java.util.List;

public class ValidateurCommande {

    public void valider(Message msg) throws LunettesException {

        // 1. clientId obligatoire
        if (msg.getClientId() == null || msg.getClientId().isBlank())
            throw LunettesException.clientIdManquant();

        // 2. liste lunettes obligatoire et non vide
        List<Message.LunettesItem> lunettes = msg.getLunettes();
        if (lunettes == null || lunettes.isEmpty())
            throw new LunettesException("LUNETTES_MANQUANTES",
                    "La liste de lunettes est vide ou absente");

        // 3. chaque ligne
        for (Message.LunettesItem item : lunettes) {

            // type null
            if (item.getType() == null || item.getType().isBlank())
                throw new LunettesException("TYPE_MANQUANT",
                        "Un type de lunette est absent");

            // type valide dans l'enum TypeLunette ?
            // Les vrais types sont : CHATGPT, LE_CHAT, BANANA, CLAUDE
            try {
                TypeLunette.valueOf(item.getType().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw LunettesException.typeInconnu(item.getType());
            }

            // quantité entre 1 et 100
            if (item.getQuantity() <= 0 || item.getQuantity() > 100)
                throw LunettesException.quantiteInvalide(item.getQuantity());
        }
    }
}