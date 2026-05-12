package fr.idmc.backend.server;

import bernard_flou.Fabricateur;
import fr.idmc.usine.Usine;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class ServeurApplication {
    public static void main(String[] args) throws Exception {
        Fabricateur fabricateur = new Fabricateur();
        Usine usine = new Usine(fabricateur);
        //ValidateurCommande validateur  = new ValidateurCommande();
    }
}
