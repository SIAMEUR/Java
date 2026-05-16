package fr.idmc.frontend.navigation;

import fr.idmc.frontend.service.MqttService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Un seul Navigation pour toute l'appli : il tient le Stage et le MqttService partages.
// Les controllers passent par "aller(...)" pour changer d'ecran.
public class Navigation {

    private static final Logger log = LoggerFactory.getLogger(Navigation.class);

    private final Stage stage;
    private final MqttService mqtt;
    private final String clientId;

    // Liste partagee des numeros de serie recus. Le LivraisonController la branche
    // a son ListView : tout ajout depuis le callback MQTT est repercute automatiquement.
    private final ObservableList<String> numerosSerieRecus = FXCollections.observableArrayList();

    // Resultat de la derniere verification. Le VerificationController binde son label
    // dessus : ecrire dans la property met a jour l'UI sans appel direct.
    private final StringProperty resultatVerification = new SimpleStringProperty("");

    // True entre l'envoi d'une commande et la reception de la livraison.
    // Sert a bloquer le bouton Commander (contrainte sujet : une seule commande par client)
    // et a afficher "Fabrication en cours" sur l'ecran Livraison.
    private final BooleanProperty commandeEnCours = new SimpleBooleanProperty(false);

    // Statut texte de la derniere commande (alimente par les messages MQTT
    // validated / status / delivery / cancelled / error). Lu par LivraisonController.
    private final StringProperty statutCommande = new SimpleStringProperty("");

    public Navigation(Stage stage, MqttService mqtt, String clientId) {
        this.stage = stage;
        this.mqtt = mqtt;
        this.clientId = clientId;
    }

    public MqttService getMqtt()                            { return mqtt; }
    public String getClientId()                             { return clientId; }
    public ObservableList<String> getNumerosSerieRecus()    { return numerosSerieRecus; }
    public StringProperty resultatVerificationProperty()    { return resultatVerification; }
    public BooleanProperty commandeEnCoursProperty()        { return commandeEnCours; }
    public StringProperty statutCommandeProperty()          { return statutCommande; }

    // Helper utilise par chaque controller pour lier son label de header
    // a l'etat de connexion du broker. Affiche "Connecte" en vert ou
    // "Deconnecte" en rouge selon la property connecte du MqttService.
    public void bindStatutConnexion(Label label) {
        Runnable maj = () -> {
            boolean ok = mqtt.connecteProperty().get();
            label.getStyleClass().removeAll("statut-connecte", "statut-deconnecte");
            label.getStyleClass().add(ok ? "statut-connecte" : "statut-deconnecte");
            label.setText(ok ? "● Connecte" : "● Deconnecte");
        };
        maj.run();
        mqtt.connecteProperty().addListener((obs, avant, apres) -> maj.run());
    }

    public void aller(String fxmlChemin) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlChemin));

            // On dit a FXMLLoader : pour instancier un controller, appelle son
            // constructeur avec Navigation en argument. Sinon il essaie le constructeur
            // vide et le controller n'a aucune dependance.
            loader.setControllerFactory(type -> {
                try {
                    return type.getDeclaredConstructor(Navigation.class).newInstance(this);
                } catch (Exception e) {
                    throw new RuntimeException("Impossible d'instancier " + type.getSimpleName(), e);
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add(
                getClass().getResource("/fr/idmc/frontend/css/style.css").toExternalForm()
            );
            stage.setScene(scene);
        } catch (Exception e) {
            log.error("Echec navigation vers {}", fxmlChemin, e);
        }
    }
}
