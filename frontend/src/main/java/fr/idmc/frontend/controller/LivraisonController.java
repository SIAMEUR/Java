package fr.idmc.frontend.controller;

import fr.idmc.frontend.navigation.Navigation;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class LivraisonController {

    private final Navigation nav;

    @FXML private Label statutLabel;
    @FXML private Label compteurLabel;
    @FXML private Label footerInfo;
    @FXML private Label statutConnexion;
    @FXML private ListView<String> liste;
    @FXML private ProgressIndicator spinner;

    public LivraisonController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());
        nav.bindStatutConnexion(statutConnexion);
        liste.setItems(nav.getNumerosSerieRecus());

        // Spinner visible uniquement pendant l'attente. managedProperty pour qu'il
        // ne reserve pas d'espace dans le layout quand il n'est pas affiche.
        spinner.visibleProperty().bind(nav.commandeEnCoursProperty());
        spinner.managedProperty().bind(nav.commandeEnCoursProperty());

        // Le statut texte est alimente directement par les handlers MQTT
        // du CommandeController (validated, status, delivery, cancelled, error).
        // Si aucun statut n'a encore ete defini, on affiche un message neutre.
        statutLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            String s = nav.statutCommandeProperty().get();
            if (s == null || s.isEmpty()) {
                int n = nav.getNumerosSerieRecus().size();
                return n == 0 ? "Aucune commande livree pour le moment." : "Commandes livrees.";
            }
            return s;
        }, nav.statutCommandeProperty(), nav.getNumerosSerieRecus()));

        compteurLabel.textProperty().bind(Bindings.createStringBinding(
                () -> nav.getNumerosSerieRecus().size() + " paire(s) recue(s)",
                nav.getNumerosSerieRecus()));

        // Ctrl+C : copie le serial selectionne dans le presse-papier.
        // Permet de coller dans l'ecran Verification sans avoir a retaper a la main.
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

        // Et aussi : double-clic copie directement (plus rapide que selection + Ctrl+C).
        liste.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
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
