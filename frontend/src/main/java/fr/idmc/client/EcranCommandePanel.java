package fr.idmc.client;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

public class EcranCommandePanel {

    private Label orderStatusLabel;

    public VBox getView() {
        VBox root = new VBox(25);
        root.setAlignment(Pos.TOP_LEFT);

        // Header
        Label title = new Label("Catalogue de produits");
        title.getStyleClass().add("page-title");
        
        Label subtitle = new Label("Sélectionnez les modèles de lunettes connectées à produire.");
        subtitle.getStyleClass().add("page-subtitle");

        orderStatusLabel = new Label("");
        
        // Product Grid
        GridPane productGrid = new GridPane();
        productGrid.setHgap(30);
        productGrid.setVgap(30);

        productGrid.add(createProductCard("Vision Pro X", "Réalité augmentée avancée avec écran micro-OLED. Parfait pour les pros.", "499€", "glass1.jpg"), 0, 0);
        productGrid.add(createProductCard("Lite Shade", "Monture titane ultra-légère. Idéal pour un usage quotidien discret.", "199€", "glass2.jpg"), 1, 0);
        productGrid.add(createProductCard("Sport Tech", "Aérodynamique, capteurs biométriques intégrés pour athlètes.", "299€", "glass3.jpg"), 0, 1);
        productGrid.add(createProductCard("Classic Glass", "Le look rétro indémodable équipé d'une puce connectée IA.", "249€", "glass4.jpg"), 1, 1);

        ScrollPane scrollPane = new ScrollPane(productGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("scroll-pane");

        root.getChildren().addAll(title, subtitle, orderStatusLabel, scrollPane);
        return root;
    }

    private VBox createProductCard(String name, String description, String price, String imageFileName) {
        VBox card = new VBox(15);
        card.getStyleClass().add("product-card");
        
        // Image View
        ImageView imageView = new ImageView();
        try {
            Image img = new Image(getClass().getResourceAsStream("images/" + imageFileName));
            imageView.setImage(img);
            imageView.setFitWidth(280);
            imageView.setFitHeight(160);
            imageView.setPreserveRatio(true);
        } catch (Exception e) {
            System.err.println("Could not load image: " + imageFileName);
        }

        StackPane imageBox = new StackPane(imageView);
        imageBox.setPrefHeight(160);
        imageBox.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8px;");

        // Info
        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px; -fx-pref-height: 40px;");

        // Bottom row (Price + Stepper + Button)
        HBox bottomRow = new HBox(15);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        
        Label priceLabel = new Label(price);
        priceLabel.getStyleClass().add("product-price");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Quantity Stepper
        HBox stepper = new HBox();
        stepper.setAlignment(Pos.CENTER);
        stepper.getStyleClass().add("quantity-stepper");

        Button minusBtn = new Button("-");
        minusBtn.getStyleClass().add("quantity-btn");
        
        Label qtyLabel = new Label("1");
        qtyLabel.getStyleClass().add("quantity-label");
        
        Button plusBtn = new Button("+");
        plusBtn.getStyleClass().add("quantity-btn");

        // State holder for quantity (array trick since it must be final for lambda)
        final int[] quantity = {1};

        minusBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                qtyLabel.setText(String.valueOf(quantity[0]));
            }
        });

        plusBtn.setOnAction(e -> {
            if (quantity[0] < 10) {
                quantity[0]++;
                qtyLabel.setText(String.valueOf(quantity[0]));
            }
        });

        stepper.getChildren().addAll(minusBtn, qtyLabel, plusBtn);

        Button orderBtn = new Button("Commander");
        orderBtn.getStyleClass().add("primary-button");
        
        orderBtn.setOnAction(e -> {
            int quantite = quantity[0];
            orderStatusLabel.setText("Commande confirmée : " + quantite + "x " + name + " en cours de fabrication.");
            orderStatusLabel.getStyleClass().remove("status-error");
            if (!orderStatusLabel.getStyleClass().contains("status-success")) {
                orderStatusLabel.getStyleClass().add("status-success");
            }
            
            // Add to our mock store multiple times
            for (int i = 0; i < quantite; i++) {
                FrontEndDataStore.getInstance().ajouterCommande(name);
            }
            
            // Reset quantity to 1 after order
            quantity[0] = 1;
            qtyLabel.setText("1");
        });

        bottomRow.getChildren().addAll(priceLabel, spacer, stepper, orderBtn);

        card.getChildren().addAll(imageBox, nameLabel, descLabel, bottomRow);
        return card;
    }
}
