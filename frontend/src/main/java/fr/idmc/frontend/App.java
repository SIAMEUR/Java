package fr.idmc.frontend;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.service.MqttService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App extends Application {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    private MqttService mqtt;

    @Override
    public void start(Stage stage) {
        try {
            // Service MQTT unique pour toute l'appli, connecte des le demarrage.
            mqtt = new MqttService();
            mqtt.connecter();

            Navigation nav = new Navigation(stage, mqtt, FrontendConfig.CLIENT_ID);

            // Abonnement au topic livraison des le demarrage : si une reponse arrive
            // avant que l'utilisateur ouvre "Mes lunettes", on ne la rate pas.
            // Le callback s'execute sur le thread Paho, donc Platform.runLater pour
            // toucher a la liste observable (qui est lue par l'UI).
            mqtt.abonner(FrontendConfig.topicLivraison(FrontendConfig.CLIENT_ID), dto -> {
                if (dto.getNumerosSerie() == null) return;
                Platform.runLater(() -> {
                    nav.getNumerosSerieRecus().addAll(dto.getNumerosSerie());
                    nav.commandeEnCoursProperty().set(false);
                });
            });

            // Reponse d'une verification : on traduit en message lisible et on l'ecrit
            // dans la property partagee, que le label du controller suit.
            mqtt.abonner(FrontendConfig.topicVerificationResult(FrontendConfig.CLIENT_ID), dto -> {
                String texte = (Boolean.TRUE.equals(dto.getValide()))
                        ? "Valide. Type : " + dto.getTypeLunette()
                        : "Numero inconnu ou contrefait.";
                Platform.runLater(() -> nav.resultatVerificationProperty().set(texte));
            });

            stage.setTitle("Lunettes connectees");
            nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
            stage.show();
        } catch (Exception e) {
            log.error("Demarrage frontend impossible", e);
        }
    }

    @Override
    public void stop() {
        if (mqtt != null) mqtt.deconnecter();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
