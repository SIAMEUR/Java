package fr.idmc.backend.serialization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format de sérialisation custom : CHAMP=valeur|CHAMP=valeur
 *
 * Séparateur de champs  : |
 * Séparateur clé=valeur : =
 * Séparateur de liste   : ,
 * Séparateur type:qté   : :
 *
 * Exemple commande :
 *   CMD_ID=abc-123|LUNETTES=CLAUDE:2,BANANA:1|TOTAL=3
 *
 * Exemple livraison :
 *   CMD_ID=abc-123|LUNETTES=CLAUDE:CL-ABC-FF,BANANA:BA-XYZ-AA
 *
 * Exemple erreur :
 *   CMD_ID=abc-123|REASON=Type inconnu : SOLAIRE
 *
 * Exemple serial check :
 *   SERIAL=CL-ABC-FF|RESULT=CLAUDE
 *   SERIAL=XX-000-00|RESULT=invalid
 */
public class MessageSerializer {

    private static final String SEP_CHAMP  = "|";
    private static final String SEP_KV     = "=";
    private static final String SEP_LISTE  = ",";
    private static final String SEP_DETAIL = ":";

    // ──────────────────────────────────────────────────────────────
    // SÉRIALISATION
    // ──────────────────────────────────────────────────────────────

    /**
     * Sérialise une commande entrante.
     * Entrée  : {CLAUDE=2, BANANA=1}
     * Sortie  : CMD_ID=abc|LUNETTES=CLAUDE:2,BANANA:1|TOTAL=3
     */
    public static String serializeCommande(String commandeId,
                                           Map<String, Integer> lunettes) {
        StringBuilder sb = new StringBuilder();
        sb.append("CMD_ID").append(SEP_KV).append(commandeId);
        sb.append(SEP_CHAMP);

        sb.append("LUNETTES").append(SEP_KV);
        List<String> items = new ArrayList<>();
        int total = 0;
        for (Map.Entry<String, Integer> e : lunettes.entrySet()) {
            items.add(e.getKey() + SEP_DETAIL + e.getValue());
            total += e.getValue();
        }
        sb.append(String.join(SEP_LISTE, items));
        sb.append(SEP_CHAMP);
        sb.append("TOTAL").append(SEP_KV).append(total);

        return sb.toString();
    }

    /**
     * Sérialise une livraison.
     * Entrée  : [{type=CLAUDE, serial=CL-ABC-FF}, ...]
     * Sortie  : CMD_ID=abc|LUNETTES=CLAUDE:CL-ABC-FF,BANANA:BA-XYZ-AA
     */
    public static String serializeDelivery(String commandeId,
                                           List<LunetteItem> lunettes) {
        StringBuilder sb = new StringBuilder();
        sb.append("CMD_ID").append(SEP_KV).append(commandeId);
        sb.append(SEP_CHAMP);

        sb.append("LUNETTES").append(SEP_KV);
        List<String> items = new ArrayList<>();
        for (LunetteItem l : lunettes) {
            items.add(l.type + SEP_DETAIL + l.serial);
        }
        sb.append(String.join(SEP_LISTE, items));

        return sb.toString();
    }

    /**
     * Sérialise une erreur ou un cancelled.
     * Sortie : CMD_ID=abc|REASON=message d erreur
     */
    public static String serializeErreur(String commandeId, String reason) {
        return "CMD_ID" + SEP_KV + commandeId
                + SEP_CHAMP
                + "REASON" + SEP_KV + reason;
    }

    /**
     * Sérialise un statut (bonus).
     * Sortie : CMD_ID=abc|STATUS=EN_FABRICATION
     */
    public static String serializeStatus(String commandeId, String status) {
        return "CMD_ID" + SEP_KV + commandeId
                + SEP_CHAMP
                + "STATUS" + SEP_KV + status;
    }

    /**
     * Sérialise le résultat d'une vérification de serial.
     * Sortie : SERIAL=CL-ABC-FF|RESULT=CLAUDE
     *          SERIAL=XX-000|RESULT=invalid
     */
    public static String serializeSerialResult(String serial, String result) {
        return "SERIAL" + SEP_KV + serial
                + SEP_CHAMP
                + "RESULT" + SEP_KV + result;
    }

    // ──────────────────────────────────────────────────────────────
    // DÉSÉRIALISATION
    // ──────────────────────────────────────────────────────────────

    /**
     * Parse un payload custom en Map clé→valeur.
     * "CMD_ID=abc|LUNETTES=CLAUDE:2" → {CMD_ID=abc, LUNETTES=CLAUDE:2}
     */
    public static Map<String, String> parse(String payload) {
        Map<String, String> map = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) return map;

        for (String champ : payload.split("\\" + SEP_CHAMP)) {
            String[] kv = champ.split(SEP_KV, 2);  // split sur le 1er = seulement
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    /**
     * Parse la valeur LUNETTES d'une commande.
     * "CLAUDE:2,BANANA:1" → {CLAUDE=2, BANANA=1}
     */
    public static Map<String, Integer> parseLunettesCommande(String valeur) {
        Map<String, Integer> map = new LinkedHashMap<>();
        if (valeur == null || valeur.isBlank()) return map;

        for (String item : valeur.split(SEP_LISTE)) {
            String[] parts = item.split(SEP_DETAIL, 2);
            if (parts.length == 2) {
                map.put(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            }
        }
        return map;
    }

    /**
     * Parse la valeur LUNETTES d'une livraison.
     * "CLAUDE:CL-ABC-FF,BANANA:BA-XYZ-AA" → liste de LunetteItem
     */
    public static List<LunetteItem> parseLunettesDelivery(String valeur) {
        List<LunetteItem> liste = new ArrayList<>();
        if (valeur == null || valeur.isBlank()) return liste;

        for (String item : valeur.split(SEP_LISTE)) {
            // Le serial contient des '-', donc on split sur le 1er ':' seulement
            String[] parts = item.split(SEP_DETAIL, 2);
            if (parts.length == 2) {
                liste.add(new LunetteItem(parts[0].trim(), parts[1].trim()));
            }
        }
        return liste;
    }

    // ──────────────────────────────────────────────────────────────
    // Objet simple pour type + serial
    // ──────────────────────────────────────────────────────────────

    public static class LunetteItem {
        public final String type;
        public final String serial;

        public LunetteItem(String type, String serial) {
            this.type   = type;
            this.serial = serial;
        }

        @Override
        public String toString() {
            return type + ":" + serial;
        }
    }
}