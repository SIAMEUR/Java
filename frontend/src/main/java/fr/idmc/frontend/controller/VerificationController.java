package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.serialization.MessageSerializer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.util.Map;

public class VerificationController {

    private final Navigation nav;

    @FXML private TextField champSerial;
    @FXML private Label resultatLabel;
    @FXML private Label footerInfo;

    public VerificationController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());
        // Le label resultat suit la property partagee dans Navigation.
        // Cette property est mise a jour soit par cette classe (au clic Verifier)
        // soit par le handler MQTT enregistre dans verifier().
        resultatLabel.textProperty().bind(nav.resultatVerificationProperty());
    }

    @FXML
    private void verifier() {
        String serial = champSerial.getText() == null ? "" : champSerial.getText().trim();
        if (serial.isEmpty()) {
            nav.resultatVerificationProperty().set("Saisis un numero de serie.");
            return;
        }

        String topicResult = FrontendConfig.topicSerialResult(serial);

        try {
            // On s'abonne au topic de reponse pour CE serial avant de publier.
            nav.getMqtt().abonner(topicResult, payload -> {
                Map<String, String> champs = MessageSerializer.parser(payload);
                String result = champs.getOrDefault("RESULT", "");

                String texte = result.equals("invalid")
                        ? "Numero inconnu ou contrefait."
                        : "Valide. Type : " + result;

                Platform.runLater(() -> nav.resultatVerificationProperty().set(texte));

                // Une fois la reponse recue on se desabonne : le verifie est ponctuel,
                // on n'a plus besoin d'ecouter ce topic.
                nav.getMqtt().desabonner(topicResult);
            });

            // Publication sur serials/{serial}/check. Le backend lit le serial
            // directement depuis le topic donc le payload peut etre vide.
            nav.getMqtt().publier(FrontendConfig.topicSerialCheck(serial), "");
            nav.resultatVerificationProperty().set("Verification en cours...");
        } catch (Exception e) {
            nav.resultatVerificationProperty().set("Echec : " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }
}
