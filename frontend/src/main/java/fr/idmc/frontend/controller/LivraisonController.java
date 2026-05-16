package fr.idmc.frontend.controller;

import fr.idmc.frontend.navigation.Navigation;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;

public class LivraisonController {

    private final Navigation nav;

    @FXML private Label statutLabel;
    @FXML private Label compteurLabel;
    @FXML private Label footerInfo;
    @FXML private ListView<String> liste;
    @FXML private ProgressIndicator spinner;

    public LivraisonController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());
        liste.setItems(nav.getNumerosSerieRecus());

        // Spinner visible uniquement pendant l'attente. managedProperty pour qu'il
        // ne reserve pas d'espace dans le layout quand il n'est pas affiche.
        spinner.visibleProperty().bind(nav.commandeEnCoursProperty());
        spinner.managedProperty().bind(nav.commandeEnCoursProperty());

        // Le statut suit la property "commande en cours" : affiche un message
        // de patience pendant la fabrication, puis le total recu apres.
        statutLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            if (nav.commandeEnCoursProperty().get()) {
                return "Fabrication en cours, patientez quelques secondes...";
            }
            int n = nav.getNumerosSerieRecus().size();
            return n == 0 ? "Aucune commande livree pour le moment." : "Commandes livrees.";
        }, nav.commandeEnCoursProperty(), nav.getNumerosSerieRecus()));

        compteurLabel.textProperty().bind(Bindings.createStringBinding(
                () -> nav.getNumerosSerieRecus().size() + " paire(s) recue(s)",
                nav.getNumerosSerieRecus()));
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }
}
