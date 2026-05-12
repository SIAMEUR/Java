package fr.idmc.backend.server;

import bernard_flou.Fabricateur;
import com.sun.source.tree.TryTree;
import fr.idmc.backend.serialization.LunettesException;
import fr.idmc.backend.serialization.Message;
import bernard_flou.Fabricateur.TypeLunette;

import java.util.Map;
import java.util.Set;

public class ValidateurCommande {

    public void valider(Message msg) throws Exception {


        if (msg.getClientId() == null || msg.getClientId().isBlank())
            throw  LunettesException.clientIdManquant();




        //verification des quantity et des types
        for (Map.Entry<Fabricateur.TypeLunette, Integer> entry :
                msg.getLunettes().entrySet()) {

            try{
                TypeLunette type = entry.getKey();
            }catch (Exception e){
                throw LunettesException.typeInconnu(e.getMessage());
            }
            int quantity = entry.getValue();
            if (quantity <= 0 || quantity > 100) {
                throw LunettesException.quantiteInvalide(quantity);
            }
        }
    }
}