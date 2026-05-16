package fr.idmc.frontend.controller;

import fr.idmc.frontend.navigation.Navigation;
import javafx.fxml.FXML;

public class AccueilController {

    private final Navigation nav;

    public AccueilController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void allerCommande() {
        nav.aller("/fr/idmc/frontend/fxml/commande.fxml");
    }

    @FXML
    private void allerLivraison() {
        nav.aller("/fr/idmc/frontend/fxml/livraison.fxml");
    }

    @FXML
    private void allerVerification() {
        nav.aller("/fr/idmc/frontend/fxml/verification.fxml");
    }
}
