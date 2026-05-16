package fr.idmc.frontend.navigation;

import fr.idmc.frontend.service.MqttService;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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

    public Navigation(Stage stage, MqttService mqtt, String clientId) {
        this.stage = stage;
        this.mqtt = mqtt;
        this.clientId = clientId;
    }

    public MqttService getMqtt()                            { return mqtt; }
    public String getClientId()                             { return clientId; }
    public ObservableList<String> getNumerosSerieRecus()    { return numerosSerieRecus; }
    public StringProperty resultatVerificationProperty()    { return resultatVerification; }

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
            Scene scene = new Scene(root, 800, 600);
            scene.getStylesheets().add(
                getClass().getResource("/fr/idmc/frontend/css/style.css").toExternalForm()
            );
            stage.setScene(scene);
        } catch (Exception e) {
            log.error("Echec navigation vers {}", fxmlChemin, e);
        }
    }
}
