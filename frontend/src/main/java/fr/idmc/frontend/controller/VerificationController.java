package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.serialization.MessageSerializer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.util.Map;

public class VerificationController {

    private static final int TIMEOUT_SECONDES = 10;

    private final Navigation nav;

    @FXML private TextField champSerial;
    @FXML private Label resultatLabel;
    @FXML private Label footerInfo;
    @FXML private Label statutConnexion;
    @FXML private Button btnVerifier;

    public VerificationController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());
        nav.bindStatutConnexion(statutConnexion);

        resultatLabel.textProperty().bind(nav.resultatVerificationProperty());

        // Bouton grise tant que le broker n'est pas joignable.
        if (btnVerifier != null) {
            btnVerifier.disableProperty().bind(nav.getMqtt().connecteProperty().not());
        }
    }

    @FXML
    private void verifier() {
        if (!nav.getMqtt().connecteProperty().get()) {
            nav.resultatVerificationProperty().set("Pas de connexion au broker, reessayez plus tard.");
            return;
        }

        String serial = champSerial.getText() == null ? "" : champSerial.getText().trim();
        if (serial.isEmpty()) {
            nav.resultatVerificationProperty().set("Saisis un numero de serie.");
            return;
        }

        String topicResult = FrontendConfig.topicSerialResult(serial);

        // Timeout court (10s) pour ne pas laisser l'utilisateur dans l'incertitude
        // si le backend ne repond pas.
        PauseTransition timeout = new PauseTransition(Duration.seconds(TIMEOUT_SECONDES));
        timeout.setOnFinished(e -> {
            nav.resultatVerificationProperty().set("Timeout : aucune reponse du backend.");
            nav.getMqtt().desabonner(topicResult);
        });

        try {
            nav.getMqtt().abonner(topicResult, payload -> {
                Map<String, String> champs = MessageSerializer.parser(payload);
                String result = champs.getOrDefault("RESULT", "");

                String texte = result.equals("invalid")
                        ? "Numero inconnu ou contrefait."
                        : "Valide. Type : " + result;

                Platform.runLater(() -> {
                    timeout.stop();
                    nav.resultatVerificationProperty().set(texte);
                });

                nav.getMqtt().desabonner(topicResult);
            });

            nav.getMqtt().publier(FrontendConfig.topicSerialCheck(serial), "");
            nav.resultatVerificationProperty().set("Verification en cours...");
            timeout.play();
        } catch (Exception e) {
            timeout.stop();
            nav.resultatVerificationProperty().set("Echec : " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }
}
