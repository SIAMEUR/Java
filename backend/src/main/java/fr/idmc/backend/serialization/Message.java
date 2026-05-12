package fr.idmc.backend.serialization;
import bernard_flou.Fabricateur.TypeLunette;
import bernard_flou.Fabricateur.Lunette;

import java.util.List;
import java.util.Map;

public class Message {

    private String clientId;
    private String commandeId;

    // Reflète exactement le JSON : liste de {type, quantity}
    private List<LunettesItem> lunettes;

    private String statut;
    private String erreur;
    private List<String> numerosSerie;
    private String numeroSerie;
    private Boolean valide;

    public Message() {}

    // ── Getters / Setters ──────────────────

    public String getClientId()                  { return clientId; }
    public void setClientId(String v)            { this.clientId = v; }

    public String getCommandeId()                { return commandeId; }
    public void setCommandeId(String v)          { this.commandeId = v; }

    public List<LunettesItem> getLunettes()      { return lunettes; }
    public void setLunettes(List<LunettesItem> v){ this.lunettes = v; }

    public String getStatut()                    { return statut; }
    public void setStatut(String v)              { this.statut = v; }

    public String getErreur()                    { return erreur; }
    public void setErreur(String v)              { this.erreur = v; }

    public List<String> getNumerosSerie()        { return numerosSerie; }
    public void setNumerosSerie(List<String> v)  { this.numerosSerie = v; }

    public String getNumeroSerie()               { return numeroSerie; }
    public void setNumeroSerie(String v)         { this.numeroSerie = v; }

    public Boolean getValide()                   { return valide; }
    public void setValide(Boolean v)             { this.valide = v; }

    // ── Classe imbriquée ───────────────────
    // Représente exactement un objet { "type": "SOLAIRE", "quantity": 2 }

    public static class LunettesItem {
        private String type;     // String ici, pas TypeLunette
        private int quantity;    // conversion faite dans Commande

        public LunettesItem() {}

        public String getType()        { return type; }
        public void setType(String v)  { this.type = v; }

        public int getQuantity()       { return quantity; }
        public void setQuantity(int v) { this.quantity = v; }
    }
}