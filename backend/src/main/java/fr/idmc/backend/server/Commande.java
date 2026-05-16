package fr.idmc.backend.server;

import bernard_flou.Fabricateur.TypeLunette;

import java.util.Map;

public class Commande {

    private final String commandeId;
    private final Map<TypeLunette, Integer> lunettes;

    public Commande(String commandeId, Map<TypeLunette, Integer> lunettes) {
        this.commandeId = commandeId;
        this.lunettes   = lunettes;
    }

    public String getCommandeId()                        { return commandeId; }
    public Map<TypeLunette, Integer> getLunettes()       { return lunettes; }
}