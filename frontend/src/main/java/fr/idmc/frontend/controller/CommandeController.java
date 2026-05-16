package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.serialization.MessageSerializer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

import java.util.LinkedHashMap;
import java.util.Map;
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

        // Bornes 0-9 imposees par le sujet (entre 0 inclus et 10 exclu).
        qtyChatgpt.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyLeChat.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyBanana.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyClaude.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));

        // Une seule commande a la fois : bouton grise tant qu'une est en cours.
        btnCommander.disableProperty().bind(nav.commandeEnCoursProperty());
        if (nav.commandeEnCoursProperty().get()) {
            statutLabel.setText("Une commande est deja en cours de fabrication.");
        }
    }

    @FXML
    private void envoyerCommande() {
        Map<String, Integer> lunettes = new LinkedHashMap<>();
        ajouterSiPositif(lunettes, "CHATGPT", qtyChatgpt.getValue());
        ajouterSiPositif(lunettes, "LE_CHAT", qtyLeChat.getValue());
        ajouterSiPositif(lunettes, "BANANA",  qtyBanana.getValue());
        ajouterSiPositif(lunettes, "CLAUDE",  qtyClaude.getValue());

        if (lunettes.isEmpty()) {
            statutLabel.setText("Selectionne au moins une paire de lunettes.");
            return;
        }

        // Identifiant unique de la commande, qui devient une partie du topic MQTT.
        String commandeId = UUID.randomUUID().toString();

        // Construction du payload au format maison.
        String payload = MessageSerializer.serialiserCommande(commandeId, lunettes);

        try {
            // On s'abonne aux topics de reponse AVANT de publier, sinon
            // la reponse du backend pourrait arriver avant qu'on ecoute.
            abonnerAuxReponses(commandeId);

            nav.getMqtt().publier(FrontendConfig.topicOrder(commandeId), payload);
            nav.commandeEnCoursProperty().set(true);
            nav.statutCommandeProperty().set("Commande envoyee, en attente de validation...");

            // On bascule direct sur l'ecran de suivi.
            nav.aller("/fr/idmc/frontend/fxml/livraison.fxml");
        } catch (Exception e) {
            statutLabel.setText("Echec de l'envoi : " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }

    // Abonne aux 5 topics de reponse pour CETTE commande.
    // Chaque handler s'execute dans le thread Paho donc on bascule sur le thread UI
    // avec Platform.runLater avant de toucher aux property et a la liste observable.
    private void abonnerAuxReponses(String commandeId) throws Exception {

        // validated : le backend a accepte la commande, payload vide
        nav.getMqtt().abonner(FrontendConfig.topicValidated(commandeId), payload ->
                Platform.runLater(() ->
                        nav.statutCommandeProperty().set("Commande validee, fabrication a venir...")
                )
        );

        // status : EN_ATTENTE / EN_FABRICATION / ...
        nav.getMqtt().abonner(FrontendConfig.topicStatus(commandeId), payload -> {
            Map<String, String> champs = MessageSerializer.parser(payload);
            String s = champs.getOrDefault("STATUS", "");
            Platform.runLater(() -> nav.statutCommandeProperty().set("Statut : " + s));
        });

        // cancelled : commande refusee par le validateur
        nav.getMqtt().abonner(FrontendConfig.topicCancelled(commandeId), payload -> {
            Map<String, String> champs = MessageSerializer.parser(payload);
            String raison = champs.getOrDefault("REASON", "raison inconnue");
            Platform.runLater(() -> {
                nav.statutCommandeProperty().set("Commande annulee : " + raison);
                nav.commandeEnCoursProperty().set(false);
            });
            cleanup(commandeId);
        });

        // error : erreur pendant la fabrication
        nav.getMqtt().abonner(FrontendConfig.topicError(commandeId), payload -> {
            Map<String, String> champs = MessageSerializer.parser(payload);
            String raison = champs.getOrDefault("REASON", "erreur inconnue");
            Platform.runLater(() -> {
                nav.statutCommandeProperty().set("Erreur de fabrication : " + raison);
                nav.commandeEnCoursProperty().set(false);
            });
            cleanup(commandeId);
        });

        // delivery : on a recu les lunettes, c'est fini
        nav.getMqtt().abonner(FrontendConfig.topicDelivery(commandeId), payload -> {
            Map<String, String> champs = MessageSerializer.parser(payload);
            String valeur = champs.getOrDefault("LUNETTES", "");
            var recues = MessageSerializer.parserLivraison(valeur);

            Platform.runLater(() -> {
                for (var r : recues) {
                    nav.getNumerosSerieRecus().add(r.serial);
                }
                nav.statutCommandeProperty().set("Commande livree.");
                nav.commandeEnCoursProperty().set(false);
            });
            cleanup(commandeId);
        });
    }

    // On se desabonne des 5 topics quand la commande est terminee (delivery,
    // cancelled ou error). Evite que la map de handlers grossisse a l'infini.
    private void cleanup(String commandeId) {
        nav.getMqtt().desabonner(FrontendConfig.topicValidated(commandeId));
        nav.getMqtt().desabonner(FrontendConfig.topicStatus(commandeId));
        nav.getMqtt().desabonner(FrontendConfig.topicCancelled(commandeId));
        nav.getMqtt().desabonner(FrontendConfig.topicError(commandeId));
        nav.getMqtt().desabonner(FrontendConfig.topicDelivery(commandeId));
    }

    private static void ajouterSiPositif(Map<String, Integer> map, String type, int qty) {
        if (qty > 0) map.put(type, qty);
    }
}
