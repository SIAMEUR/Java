package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.model.MessageDto;
import fr.idmc.frontend.navigation.Navigation;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class VerificationController {

    private final Navigation nav;

    @FXML private TextField champSerial;
    @FXML private Label resultatLabel;

    public VerificationController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        // Binding unidirectionnel : tout changement de la property se reflete dans le label.
        // La property est ecrite a la fois ici (clic Verifier) et par le handler MQTT (App).
        resultatLabel.textProperty().bind(nav.resultatVerificationProperty());
    }

    @FXML
    private void verifier() {
        String serial = champSerial.getText() == null ? "" : champSerial.getText().trim();
        if (serial.isEmpty()) {
            nav.resultatVerificationProperty().set("Saisis un numero de serie.");
            return;
        }

        MessageDto dto = new MessageDto();
        dto.setClientId(nav.getClientId());
        dto.setNumeroSerie(serial);

        try {
            nav.getMqtt().publier(FrontendConfig.TOPIC_VERIFICATION, dto);
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
