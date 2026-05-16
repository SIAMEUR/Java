package fr.idmc.backend.serialization;

import bernard_flou.Fabricateur.TypeLunette;

public class LunettesException extends RuntimeException {

    private final String code;

    public LunettesException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() { return code; }

    // Codes d'erreur standards
    public static LunettesException clientIdManquant() {
        return new LunettesException("CLIENT_ID_MANQUANT", "clientId absent ou vide");
    }
    public static LunettesException quantiteInvalide(int q) {
        return new LunettesException("QUANTITE_INVALIDE",
                "Quantité invalide : " + q + " (attendu : 1-100)");
    }
    public static LunettesException typeInconnu(String type) {
        return new LunettesException("TYPE_INCONNU", "Type de lunettes inconnu : " + type);
    }
}
