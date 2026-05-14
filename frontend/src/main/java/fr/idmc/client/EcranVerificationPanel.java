package fr.idmc.client;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EcranVerificationPanel {

    public VBox getView() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new javafx.geometry.Insets(40));

        // Header
        Label title = new Label("Portail d'Authentification");
        title.getStyleClass().add("page-title");
        
        Label subtitle = new Label("Saisissez le numéro de série pour vérifier la conformité du produit.");
        subtitle.getStyleClass().add("page-subtitle");

        // Search Bar (HBox)
        HBox searchBox = new HBox(10);
        searchBox.setAlignment(Pos.CENTER);
        
        TextField champNumeroSerie = new TextField();
        champNumeroSerie.setPromptText("Ex: SN-A1B2C-PEN");
        champNumeroSerie.setPrefWidth(350);
        champNumeroSerie.setStyle("-fx-padding: 12px; -fx-font-size: 16px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-color: #cbd5e1;");

        Button boutonVerifier = new Button("Auditer le produit");
        boutonVerifier.getStyleClass().add("primary-button");
        boutonVerifier.setStyle("-fx-padding: 12px 24px; -fx-font-size: 16px;");

        searchBox.getChildren().addAll(champNumeroSerie, boutonVerifier);

        // Result Card (Hidden by default)
        VBox resultCard = new VBox(15);
        resultCard.getStyleClass().add("card");
        resultCard.setAlignment(Pos.CENTER);
        resultCard.setVisible(false);
        resultCard.setPrefWidth(500);
        resultCard.setMaxWidth(500);
        resultCard.setStyle("-fx-padding: 30px; -fx-background-color: #ffffff;");

        Label statusIcon = new Label();
        statusIcon.setStyle("-fx-font-size: 48px;");

        Label resultTitle = new Label();
        resultTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        Label resultDetails = new Label();
        resultDetails.setStyle("-fx-font-size: 15px; -fx-text-fill: #475569;");

        resultCard.getChildren().addAll(statusIcon, resultTitle, resultDetails);

        // Action Logic
        boutonVerifier.setOnAction(e -> {
            String numeroSaisi = champNumeroSerie.getText().trim();

            if (numeroSaisi.isEmpty()) {
                champNumeroSerie.setStyle("-fx-padding: 12px; -fx-font-size: 16px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-color: #ef4444;");
                return;
            }
            champNumeroSerie.setStyle("-fx-padding: 12px; -fx-font-size: 16px; -fx-border-radius: 8px; -fx-background-radius: 8px; -fx-border-color: #cbd5e1;");

            boutonVerifier.setDisable(true);
            boutonVerifier.setText("Vérification...");
            resultCard.setVisible(false);

            new Thread(() -> {
                try {
                    Thread.sleep(800); // Simulate network/DB delay
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                javafx.application.Platform.runLater(() -> {
                    java.util.Optional<EcranHistoriquePanel.LunetteProduite> found = FrontEndDataStore.getInstance().getCommandes().stream()
                        .filter(lunette -> lunette.getNumeroSerie().equalsIgnoreCase(numeroSaisi))
                        .findFirst();

                    resultCard.setVisible(true);

                    if (found.isPresent()) {
                        statusIcon.setText("✅");
                        resultTitle.setText("Produit Authentique");
                        resultTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #10b981;");
                        
                        EcranHistoriquePanel.LunetteProduite lp = found.get();
                        resultDetails.setText("Modèle : " + lp.getModele() + "\n" +
                                              "Fabriqué le : " + lp.getDateFabrication() + "\n" +
                                              "Statut : " + lp.getStatut());
                        resultCard.setStyle("-fx-padding: 30px; -fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0; -fx-border-radius: 8px; -fx-border-width: 2px;");
                    } else {
                        statusIcon.setText("⚠️");
                        resultTitle.setText("Produit Inconnu");
                        resultTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: #ef4444;");
                        resultDetails.setText("Le numéro de série '" + numeroSaisi + "' n'existe pas dans la blockchain de l'usine.\nContrefaçon suspectée.");
                        resultCard.setStyle("-fx-padding: 30px; -fx-background-color: #fef2f2; -fx-border-color: #fecaca; -fx-border-radius: 8px; -fx-border-width: 2px;");
                    }
                    
                    boutonVerifier.setDisable(false);
                    boutonVerifier.setText("Auditer le produit");
                });
            }).start();
        });

        root.getChildren().addAll(title, subtitle, searchBox, resultCard);
        return root;
    }
}
