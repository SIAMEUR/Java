package fr.idmc.frontend.controller;

import fr.idmc.frontend.config.FrontendConfig;
import fr.idmc.frontend.navigation.Navigation;
import fr.idmc.frontend.serialization.MessageSerializer;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class CommandeController {

    private static final int TIMEOUT_SECONDES = 30;

    private final Navigation nav;

    @FXML private Spinner<Integer> qtyChatgpt;
    @FXML private Spinner<Integer> qtyLeChat;
    @FXML private Spinner<Integer> qtyBanana;
    @FXML private Spinner<Integer> qtyClaude;
    @FXML private Label statutLabel;
    @FXML private Label footerInfo;
    @FXML private Label statutConnexion;
    @FXML private Button btnCommander;

    public CommandeController(Navigation nav) {
        this.nav = nav;
    }

    @FXML
    private void initialize() {
        footerInfo.setText("Session : " + nav.getClientId());
        nav.bindStatutConnexion(statutConnexion);

        // Bornes 0-9 imposees par le sujet (entre 0 inclus et 10 exclu).
        qtyChatgpt.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyLeChat.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyBanana.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));
        qtyClaude.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, 0));

        // Une seule commande a la fois : bouton grise tant qu'une est en cours.
        // Et grise aussi quand le broker n'est pas joignable.
        btnCommander.disableProperty().bind(
                nav.commandeEnCoursProperty()
                        .or(nav.getMqtt().connecteProperty().not()));

        if (nav.commandeEnCoursProperty().get()) {
            statutLabel.setText("Une commande est deja en cours de fabrication.");
        }
    }

    @FXML
    private void envoyerCommande() {
        if (!nav.getMqtt().connecteProperty().get()) {
            statutLabel.setText("Pas de connexion au broker, reessayez plus tard.");
            return;
        }

        Map<String, Integer> lunettes = new LinkedHashMap<>();
        ajouterSiPositif(lunettes, "CHATGPT", qtyChatgpt.getValue());
        ajouterSiPositif(lunettes, "LE_CHAT", qtyLeChat.getValue());
        ajouterSiPositif(lunettes, "BANANA",  qtyBanana.getValue());
        ajouterSiPositif(lunettes, "CLAUDE",  qtyClaude.getValue());

        if (lunettes.isEmpty()) {
            statutLabel.setText("Selectionne au moins une paire de lunettes.");
            return;
        }

        String commandeId = UUID.randomUUID().toString();
        String payload = MessageSerializer.serialiserCommande(commandeId, lunettes);

        // Timeout : si on n'a pas recu delivery/cancelled/error apres N secondes,
        // on considere que le backend ne repond pas et on sort de l'etat d'attente.
        PauseTransition timeout = new PauseTransition(Duration.seconds(TIMEOUT_SECONDES));
        timeout.setOnFinished(e -> {
            if (nav.commandeEnCoursProperty().get()) {
                nav.statutCommandeProperty().set("Timeout : aucune reponse du backend apres "
                        + TIMEOUT_SECONDES + "s.");
                nav.commandeEnCoursProperty().set(false);
                cleanup(commandeId);
            }
        });

        try {
            abonnerAuxReponses(commandeId, timeout);
            nav.getMqtt().publier(FrontendConfig.topicOrder(commandeId), payload);
            nav.commandeEnCoursProperty().set(true);
            nav.statutCommandeProperty().set("Commande envoyee, en attente de validation...");
            timeout.play();
            nav.aller("/fr/idmc/frontend/fxml/livraison.fxml");
        } catch (Exception e) {
            timeout.stop();
            statutLabel.setText("Echec de l'envoi : " + e.getMessage());
        }
    }

    @FXML
    private void retourAccueil() {
        nav.aller("/fr/idmc/frontend/fxml/accueil.fxml");
    }

    // Abonne aux 5 topics de reponse pour CETTE commande.
    // Les handlers terminaux (delivery, cancelled, error) annulent le timeout.
    private void abonnerAuxReponses(String commandeId, PauseTransition timeout) throws Exception {

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
                timeout.stop();
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
                timeout.stop();
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
                timeout.stop();
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
    // cancelled, error ou timeout). Evite que la map de handlers grossisse a l'infini.
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
