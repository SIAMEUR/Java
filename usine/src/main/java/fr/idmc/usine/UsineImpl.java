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
}