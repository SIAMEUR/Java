package fr.idmc.backend.server;

import bernard_flou.Fabricateur;

import fr.idmc.factory.Usine;
import fr.idmc.factory.IUsine;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import bernard_flou.Fabricateur;
import fr.idmc.backend.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServeurApplication {

    private static final Logger log = LoggerFactory.getLogger(ServeurApplication.class);

    public static void main(String[] args) throws Exception {

        //init
        Fabricateur fabricateur = new Fabricateur();
        Usine usine = new Usine(fabricateur);
        ValidateurCommande validateur = new ValidateurCommande();
        OrderHandler handler = new OrderHandler(usine, validateur);
        ServeurMqtt serveur = new ServeurMqtt(handler);


        //demmarage
        serveur.demarrer();
        log.info("Serveur prêt — broker : {}", AppConfig.BROKER_URL);



        //stop
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Arret du serveur en cours...");
            try {
                serveur.arreter();
                log.info("Serveur arreté proprement.");
            } catch (Exception e) {
                log.error("Erreur lors de l'arrêt : {}", e.getMessage());
            }
        }, "shutdown"));

        Thread.currentThread().join();
    }
}
