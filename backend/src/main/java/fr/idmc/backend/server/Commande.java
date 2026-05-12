package fr.idmc.backend.server;
import bernard_flou.Fabricateur.TypeLunette;
import fr.idmc.backend.serialization.Message;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Commande {

    private final String commandeId;
    private final String clientId;
    private final Map<TypeLunette, Integer> lunettes;

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

        // Conversion List<LunettesItem> → Map<TypeLunette, Integer>
        Map<TypeLunette, Integer> map = new LinkedHashMap<>();
        for (Message.LunettesItem item : msg.getLunettes()) {
            TypeLunette type = TypeLunette.valueOf(item.getType().toUpperCase());
            map.merge(type, item.getQuantity(), Integer::sum);
        }

        return new Commande(id, msg.getClientId(), map);
    }

    public String getCommandeId()                       { return commandeId; }
    public String getClientId()                         { return clientId; }
    public Map<TypeLunette, Integer> getLunettes()      { return lunettes; }
}