package fr.idmc.usine;

import bernard_flou.Fabricateur;
import bernard_flou.Fabricateur.Lunette;
import bernard_flou.Fabricateur.TypeLunette;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UsineImpl implements Usine {

    private static final Logger logger = LoggerFactory.getLogger(UsineImpl.class);

    private final Fabricateur fabricateur;
    private final int capacite;
    private final ExecutorService pool;

    public UsineImpl(Fabricateur fabricateur) {
        this.fabricateur = fabricateur;
        this.capacite    = fabricateur.getCapacity();
        this.pool        = Executors.newFixedThreadPool(capacite);
        logger.info("Usine démarré avec capacite : {}", capacite);
    }

    @Override
    public List<Lunette> produire(Map<TypeLunette, Integer> typesLunettes) {
        // TODO
        return new ArrayList<>();
    }

    /**
     * extriare que les types dans chaque map (type:quantité)
     * Exemple : {BlaBlaBla:2,Bananaaaa:1} en {BlaBlaBla,Bananaaaa}
     * @param map la commande sous forme de Map
     * @return la liste aplatie
     */
    private List<TypeLunette> aplatir(Map<TypeLunette, Integer> map) {
        List<TypeLunette> liste = new ArrayList<>();
        map.forEach((type, qnt) -> {
            for (int i = 0; i < qnt; i++) {
                liste.add(type);
            }
        });
        return liste;
    }

    /**
     * Découpe une selon la capacité
     * Exemple : {BlaBlaBla,Bananaaaa} avec capacité de 1 -> {BlaBlaBla}, {Bananaaaa}
     * @param liste  la liste a decouper
     * @param capacité la taille max de chaque sous liste
     * @param <T>    le type des élements
     * @return la liste des sous listes
     */
    private <T> List<List<T>> decouper(List<T> liste, int capacité) {
        List<List<T>> resultat = new ArrayList<>();
        for (int i = 0; i < liste.size(); i += capacité) {
            resultat.add(new ArrayList<>(liste.subList(i, Math.min(i + capacité, liste.size()))));
        }
        return resultat;
    }
    //Association entre un type de lunette à produire et la demande cliente qui l'a soumis
    //c'est notre façon pour comment savoir à qui appartient la commande
    private static class Slot {
        private final TypeLunette type;
        private final Demande demande;

        Slot(TypeLunette type, Demande demande) {
            this.type    = type;
            this.demande = demande;
        }

        TypeLunette getType()    { return type; }
        Demande     getDemande() { return demande; }
    }
    //pour savoir à qui la commande
    /**
     * @param lunette
     * @param demande
     */
    private record LunetteEtDemande(Lunette lunette, Demande demande) {}

    private static class Demande {

        private final List<TypeLunette> types;
        private final List<Lunette> lunettesRecues;
        private final int quantiteTotale;
        private ProductionException erreur = null;
        private boolean termine = false;

        Demande(List<TypeLunette> types) {
            this.types          = types;
            this.quantiteTotale = types.size();
            this.lunettesRecues = new ArrayList<>();
        }

        List<TypeLunette> getTypes() { return types; }
        /**
         * @param lunette la lunette peoduite
         */
        synchronized void ajouterLunette(Lunette lunette) {
            lunettesRecues.add(lunette);
            if (lunettesRecues.size() >= quantiteTotale) {
                termine = true;
                notifyAll();
            }
        }
        /**
         * @param e l'exception
         */
        synchronized void signalerErreur(ProductionException e) {
            this.erreur  = e;
            this.termine = true;
            notifyAll();
        }
        /**
         * @return la liste des lunettes produites
         * @throws ProductionException ProductionException
         */
        synchronized List<Lunette> attendreResultat() {
            while (!termine) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ProductionException("Attente interrompue", e);
                }
            }
            if (erreur != null) throw erreur;
            return new ArrayList<>(lunettesRecues);
        }
    }

}