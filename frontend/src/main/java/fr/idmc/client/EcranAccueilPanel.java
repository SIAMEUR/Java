package fr.idmc.client;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class EcranAccueilPanel {

    private final Runnable onCommandeClick;
    private final Runnable onSuiviClick;
    private final Runnable onVerificationClick;

    public EcranAccueilPanel(Runnable onCommandeClick, Runnable onSuiviClick, Runnable onVerificationClick) {
        this.onCommandeClick = onCommandeClick;
        this.onSuiviClick = onSuiviClick;
        this.onVerificationClick = onVerificationClick;
    }

    public VBox getView() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);

        // Illustration / Icone
        Label logo = new Label("SMART GLASSES FACTORY");
        logo.setStyle("-fx-font-size: 32px; -fx-font-weight: 900; -fx-text-fill: #1e293b;");

        // Introduction Text
        Label welcomeText = new Label("Bienvenue sur le portail de commande et suivi de l'usine.");
        welcomeText.setStyle("-fx-font-size: 18px; -fx-text-fill: #64748b;");

        // Feature cards
        HBox featureCards = new HBox(30);
        featureCards.setAlignment(Pos.CENTER);

        VBox card1 = createFeatureCard("Commander", "Parcourez notre catalogue et lancez la fabrication.", "#3b82f6", onCommandeClick);
        VBox card2 = createFeatureCard("Suivi", "Consultez en temps réel les numéros de série fabriqués.", "#10b981", onSuiviClick);
        VBox card3 = createFeatureCard("Vérification", "Vérifiez l'authenticité d'une paire avec son numéro.", "#8b5cf6", onVerificationClick);

        featureCards.getChildren().addAll(card1, card2, card3);

        root.getChildren().addAll(logo, welcomeText, featureCards);
        return root;
    }

    private VBox createFeatureCard(String title, String description, String headerColorHex, Runnable onClick) {
        VBox card = new VBox(15);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(220);
        card.setPrefHeight(180);
        card.setStyle("-fx-cursor: hand;");
        
        card.setOnMouseEntered(e -> card.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(37,99,235,0.2), 20, 0, 0, 8); -fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-cursor: hand; -fx-scale-x: 1; -fx-scale-y: 1;"));
        
        card.setOnMouseClicked(e -> {
            if (onClick != null) {
                onClick.run();
            }
        });

        // Modern Colored Bar
        javafx.scene.layout.Region colorBar = new javafx.scene.layout.Region();
        colorBar.setPrefHeight(6);
        colorBar.setMaxWidth(Double.MAX_VALUE);
        colorBar.setStyle("-fx-background-color: " + headerColorHex + "; -fx-background-radius: 4px;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748b; -fx-text-alignment: center;");

        card.getChildren().addAll(colorBar, titleLabel, descLabel);
        return card;
    }
}
