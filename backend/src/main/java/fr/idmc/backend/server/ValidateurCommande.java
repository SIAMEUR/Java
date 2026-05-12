package fr.idmc.backend.server;

import fr.idmc.backend.serialization.LunettesException;
import fr.idmc.backend.serialization.Message;
import bernard_flou.Fabricateur.TypeLunette;

import java.util.List;

public class ValidateurCommande {

    public void valider(Message msg) throws LunettesException {

        // clientId obligatoire
        if (msg.getClientId() == null || msg.getClientId().isBlank())
            throw LunettesException.clientIdManquant();

        List<Message.LunettesItem> lunettes = msg.getLunettes();
        if (lunettes == null || lunettes.isEmpty())
            throw new LunettesException("LUNETTES_MANQUANTES",
                    "La liste de lunettes est vide ou absente");
        for (Message.LunettesItem item : lunettes) {
            try {
                TypeLunette.valueOf(item.getType().toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                throw LunettesException.typeInconnu(item.getType());
            }

            // quantity entre 1 et 100
            if (item.getQuantity() <= 0 || item.getQuantity() > 100)
                throw LunettesException.quantiteInvalide(item.getQuantity());
        }
    }
}