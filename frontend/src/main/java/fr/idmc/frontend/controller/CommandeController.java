package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.model.MessageDto;
import fr.idmc.frontend.model.MessageDto.LunettesItem;
import fr.idmc.frontend.navigation.Navigation;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CommandeController {

    private final Navigation nav;

    @FXML private Spinner<Integer> qtyChatgpt;
    @FXML private Spinner<Integer> qtyLeChat;
    @FXML private Spinner<Integer> qtyBanana;
    @FXML private Spinner<Integer> qtyClaude;
    @FXML private Label statutLabel;

    public CommandeController(Navigation nav) {
        this.nav = nav;
    }

    // Appelee automatiquement par FXMLLoader apres injection des @FXML.
    @FXML
    private void initialize() {
        qtyChatgpt.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        qtyLeChat.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        qtyBanana.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        qtyClaude.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
    }

    @FXML
    private void envoyerCommande() {
        List<LunettesItem> items = new ArrayList<>();
        ajouterSiPositif(items, "CHATGPT", qtyChatgpt.getValue());
        ajouterSiPositif(items, "LE_CHAT", qtyLeChat.getValue());
        ajouterSiPositif(items, "BANANA",  qtyBanana.getValue());
        ajouterSiPositif(items, "CLAUDE",  qtyClaude.getValue());

        if (items.isEmpty()) {
            statutLabel.setText("Selectionne au moins une paire de lunettes.");
            return;
        }

        MessageDto dto = new MessageDto();
        dto.setClientId(nav.getClientId());
        dto.setCommandeId(UUID.randomUUID().toString());
        dto.setLunettes(items);

        try {
            nav.getMqtt().publier(FrontendConfig.TOPIC_COMMANDES, dto);
            statutLabel.setText("Commande envoyee. Reponse a venir dans 'Mes lunettes'.");
        } catch (Exception e) {
            statutLabel.setText("Echec de l'envoi : " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }

    private static void ajouterSiPositif(List<LunettesItem> items, String type, int qty) {
        if (qty > 0) items.add(new LunettesItem(type, qty));
    }
}
