package fr.idmc.frontend.controller;

import fr.idmc.frontend.navigation.Navigation;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class LivraisonController {

    private final Navigation nav;

    @FXML private ListView<String> liste;

    public LivraisonController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        // La ListView se branche directement sur la liste partagee.
        // Tout ajout ailleurs (callback MQTT) sera reflete ici automatiquement.
        liste.setItems(nav.getNumerosSerieRecus());

        // Ctrl+C : copie le serial selectionne dans le presse-papier.
        KeyCombination ctrlC = new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN);
        liste.setOnKeyPressed(e -> {
            if (ctrlC.match(e)) {
                String selection = liste.getSelectionModel().getSelectedItem();
                if (selection != null) {
                    ClipboardContent content = new ClipboardContent();
                    content.putString(selection);
                    Clipboard.getSystemClipboard().setContent(content);
                }
            }
        });
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }
}
