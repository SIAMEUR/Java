package fr.idmc.frontend.serialization;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Format maison du projet : CHAMP=valeur|CHAMP=valeur
// On a la meme classe cote backend, c'est notre contrat partage.
// Le prof interdit JSON/XML/etc donc on a invente ce format simple a base
// de separateurs textes.
//
// Separateurs utilises :
//   |  entre champs            ex: CMD_ID=abc|LUNETTES=...
//   =  entre cle et valeur     ex: CMD_ID=abc
//   ,  entre items de liste    ex: CLAUDE:2,BANANA:1
//   :  entre type et detail    ex: CHATGPT:CH-XXX-YYY
public final class MessageSerializer {

    private static final String CHAMP  = "|";
    private static final String KV     = "=";
    private static final String LISTE  = ",";
    private static final String DETAIL = ":";

    private MessageSerializer() {}

    // ── Serialisation cote frontend (ce qu'on envoie) ──

    // Construit le payload a publier sur orders/{commandeId}.
    // Resultat : CMD_ID=abc-123|LUNETTES=CHATGPT:2,CLAUDE:1|TOTAL=3
    public static String serialiserCommande(String commandeId,
                                            Map<String, Integer> lunettes) {
        StringBuilder sb = new StringBuilder();
        sb.append("CMD_ID").append(KV).append(commandeId).append(CHAMP);

        sb.append("LUNETTES").append(KV);
        int total = 0;
        boolean premier = true;
        for (Map.Entry<String, Integer> e : lunettes.entrySet()) {
            if (!premier) sb.append(LISTE);
            sb.append(e.getKey()).append(DETAIL).append(e.getValue());
            total += e.getValue();
            premier = false;
        }

        sb.append(CHAMP).append("TOTAL").append(KV).append(total);
        return sb.toString();
    }

    // ── Deserialisation cote frontend (ce qu'on recoit) ──

    // Coupe un payload "CHAMP=val|CHAMP=val" et le met dans une map.
    public static Map<String, String> parser(String payload) {
        Map<String, String> map = new LinkedHashMap<>();
        if (payload == null || payload.isBlank()) return map;

        // Le pipe est un caractere special dans les regex, d'ou le \\
        for (String champ : payload.split("\\" + CHAMP)) {
            // limit=2 : on coupe seulement sur le 1er '=' au cas ou la valeur
            // contiendrait un autre '=' (peu probable mais safe)
            String[] kv = champ.split(KV, 2);
            if (kv.length == 2) {
                map.put(kv[0].trim(), kv[1].trim());
            }
        }
        return map;
    }

    // Parse la valeur "CHATGPT:CH-XXX,CLAUDE:CL-YYY" en liste type+serial.
    public static List<LunetteRecue> parserLivraison(String valeur) {
        List<LunetteRecue> liste = new ArrayList<>();
        if (valeur == null || valeur.isBlank()) return liste;

        for (String item : valeur.split(LISTE)) {
            // Le serial peut contenir des '-' donc on coupe sur le 1er ':'
            String[] parts = item.split(DETAIL, 2);
            if (parts.length == 2) {
                liste.add(new LunetteRecue(parts[0].trim(), parts[1].trim()));
            }
        }
        return liste;
    }

    // Petit objet pour transporter une paire fabriquee : son type et son serial.
    public static class LunetteRecue {
        public final String type;
        public final String serial;

        public LunetteRecue(String type, String serial) {
            this.type = type;
            this.serial = serial;
        }
    }
}
