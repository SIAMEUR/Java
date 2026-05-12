package fr.idmc.backend.serialization;
import bernard_flou.Fabricateur.TypeLunette;
import bernard_flou.Fabricateur.Lunette;

import java.util.List;
import java.util.Map;

public class Message {

    private String clientId;
    private String commandeId;
    private Map<TypeLunette, Integer> lunettes;

    // Champs réponse (serveur → client)
    private String statut;        // "OK" ou "ERREUR"
    private String erreur;
    private List<String> numerosSerie;

    // Champs vérification
    private String numeroSerie;
    private Boolean valide;

    // ─── Constructeurs ───────────────────────

    public Message() {}

    // Getters / Setters
    public String getClientId()              { return clientId; }
    public void setClientId(String v)        { this.clientId = v; }

    public String getCommandeId()            { return commandeId; }
    public void setCommandeId(String v)      { this.commandeId = v; }

    public Map<TypeLunette, Integer> getLunettes()  { return lunettes; }
    public void setLunettes(Map<TypeLunette, Integer> v) { this.lunettes = v; }

    public String getStatut()                { return statut; }
    public void setStatut(String v)          { this.statut = v; }

    public String getErreur()                { return erreur; }
    public void setErreur(String v)          { this.erreur = v; }

    public List<String> getNumerosSerie()    { return numerosSerie; }
    public void setNumerosSerie(List<String> v) { this.numerosSerie = v; }

    public String getNumeroSerie()           { return numeroSerie; }
    public void setNumeroSerie(String v)     { this.numeroSerie = v; }

    public Boolean getValide()               { return valide; }
    public void setValide(Boolean v)         { this.valide = v; }


}
