package fr.idmc.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class EcranHistoriquePanel {

    public VBox getView() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.TOP_LEFT);

        // Header
        Label title = new Label("Suivi de Production");
        title.getStyleClass().add("page-title");
        
        Label subtitle = new Label("Consultez l'état et les numéros de série des lunettes fabriquées par l'usine.");
        subtitle.getStyleClass().add("page-subtitle");

        // Table
        TableView<LunetteProduite> table = new TableView<>();
        table.getStyleClass().add("custom-table");
        
        Label emptyLabel = new Label("Aucune commande dans l'historique.");
        emptyLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-font-style: italic;");
        table.setPlaceholder(emptyLabel);
        
        TableColumn<LunetteProduite, String> colModele = new TableColumn<>("Modèle");
        colModele.setCellValueFactory(new PropertyValueFactory<>("modele"));
        colModele.setPrefWidth(200);

        TableColumn<LunetteProduite, String> colNumero = new TableColumn<>("Numéro de Série");
        colNumero.setCellValueFactory(new PropertyValueFactory<>("numeroSerie"));
        colNumero.setPrefWidth(250);

        TableColumn<LunetteProduite, String> colDate = new TableColumn<>("Date de Fabrication");
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateFabrication"));
        colDate.setPrefWidth(200);

        TableColumn<LunetteProduite, String> colStatus = new TableColumn<>("Statut");
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colStatus.setPrefWidth(150);

        table.getColumns().addAll(colModele, colNumero, colDate, colStatus);

        // Mock Data from Shared Store
        ObservableList<LunetteProduite> donnees = FrontEndDataStore.getInstance().getCommandes();
        table.setItems(donnees);

        root.getChildren().addAll(title, subtitle, table);
        return root;
    }

    // Inner class for the Table Model
    public static class LunetteProduite {
        private final String modele;
        private final String numeroSerie;
        private final String dateFabrication;
        private final String statut;

        public LunetteProduite(String modele, String numeroSerie, String dateFabrication, String statut) {
            this.modele = modele;
            this.numeroSerie = numeroSerie;
            this.dateFabrication = dateFabrication;
            this.statut = statut;
        }

        public String getModele() { return modele; }
        public String getNumeroSerie() { return numeroSerie; }
        public String getDateFabrication() { return dateFabrication; }
        public String getStatut() { return statut; }
    }
}
