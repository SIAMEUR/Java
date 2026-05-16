package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.model.MessageDto;
import fr.idmc.frontend.model.MessageDto.LunettesItem;
import fr.idmc.frontend.navigation.Navigation;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
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
    @FXML private Label footerInfo;
    @FXML private Button btnCommander;

    public CommandeController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());

        // Bornes 0-9 imposees par le sujet (quantite entre 0 inclus et 10 exclu).
        qtyChatgpt.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyLeChat.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyBanana.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyClaude.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));

        // Sujet : "une seule commande en simultane par client". Tant qu'une commande
        // est en cours, le bouton est desactive et un message l'indique.
        btnCommander.disableProperty().bind(nav.commandeEnCoursProperty());
        if (nav.commandeEnCoursProperty().get()) {
            statutLabel.setText("Une commande est deja en cours de fabrication.");
        }
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
            nav.commandeEnCoursProperty().set(true);
            // On bascule direct sur l'ecran de suivi : c'est lui qui fait patienter
            // pendant la fabrication puis affiche les serials a la livraison.
            nav.aller("/fr/idmc/frontend/fxml/livraison.fxml");
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
