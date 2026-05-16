package fr.idmc.backend.server;

import bernard_flou.Fabricateur.TypeLunette;
import fr.idmc.backend.serialization.LunettesException;

import java.util.Map;

public class ValidateurCommande {

    public void valider(Map<TypeLunette, Integer> lunettes) throws LunettesException {

        // Quantité totale strictement supérieure à 0
        int total = lunettes.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0)
            throw new LunettesException("TOTAL_INVALIDE",
                    "La quantité totale doit être strictement supérieure à 0");

        // Chaque quantité entre 0 (inclus) et 10 (exclu)
        for (Map.Entry<TypeLunette, Integer> entry : lunettes.entrySet()) {
            int qte = entry.getValue();
            if (qte < 0 || qte >= 10)
                throw new LunettesException("QUANTITE_INVALIDE",
                        "Quantité invalide pour " + entry.getKey()
                                + " : " + qte + " (attendu : 0 ≤ qté < 10)");
        }
    }
}