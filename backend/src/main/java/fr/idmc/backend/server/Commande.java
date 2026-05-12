package fr.idmc.backend.server;
import bernard_flou.Fabricateur.TypeLunette;
import fr.idmc.backend.serialization.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Commande {

    private final String commandeId;
    private final String clientId;
    private Map<TypeLunette, Integer> lunettes;

    public Commande(String commandeId, String clientId,
                    Map<TypeLunette, Integer> lunettes) {
        this.commandeId = commandeId;
        this.clientId   = clientId;
        this.lunettes   = lunettes;
    }


    public static Commande depuisMessage(Message msg) {
        String id = (msg.getCommandeId() != null && !msg.getCommandeId().isBlank())
                ? msg.getCommandeId()
                : UUID.randomUUID().toString();

        return new Commande(id, msg.getClientId(), msg.getLunettes());
    }



    public String getCommandeId()              { return commandeId; }
    public String getClientId()                { return clientId; }
    public Map<TypeLunette, Integer> getLunettes() { return lunettes; }
}