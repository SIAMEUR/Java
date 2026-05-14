package fr.idmc.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class DashboardApp extends Application {

    private StackPane contentArea;
    private Button btnAccueil;
    private Button btnCommande;
    private Button btnHistorique;
    private Button btnVerification;

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();

        // 1. Sidebar (Left)
        VBox sidebar = new VBox(10);
        sidebar.setPrefWidth(250);
        sidebar.getStyleClass().add("sidebar");

        Label appTitle = new Label("SMART\nGLASSES");
        appTitle.getStyleClass().add("sidebar-title");
        
        btnAccueil = new Button("Accueil");
        btnAccueil.getStyleClass().add("nav-button");
        btnAccueil.setMaxWidth(Double.MAX_VALUE);

        btnCommande = new Button("Commander");
        btnCommande.getStyleClass().add("nav-button");
        btnCommande.setMaxWidth(Double.MAX_VALUE);
        
        btnHistorique = new Button("Suivi de Production");
        btnHistorique.getStyleClass().add("nav-button");
        btnHistorique.setMaxWidth(Double.MAX_VALUE);
        
        btnVerification = new Button("Vérification");
        btnVerification.getStyleClass().add("nav-button");
        btnVerification.setMaxWidth(Double.MAX_VALUE);

        sidebar.getChildren().addAll(appTitle, btnAccueil, btnCommande, btnHistorique, btnVerification);

        // 2. Content Area (Center)
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        // Actions
        btnAccueil.setOnAction(e -> {
            setActiveButton(btnAccueil);
            setContent(new EcranAccueilPanel(
                () -> btnCommande.fire(), 
                () -> btnHistorique.fire(), 
                () -> btnVerification.fire()
            ).getView());
        });

        btnCommande.setOnAction(e -> {
            setActiveButton(btnCommande);
            setContent(new EcranCommandePanel().getView());
        });
        
        btnHistorique.setOnAction(e -> {
            setActiveButton(btnHistorique);
            setContent(new EcranHistoriquePanel().getView());
        });

        btnVerification.setOnAction(e -> {
            setActiveButton(btnVerification);
            setContent(new EcranVerificationPanel().getView());
        });

        // Default view
        btnAccueil.fire();

        Scene scene = new Scene(root, 1000, 700);
        String css = getClass().getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("Usine - Dashboard Pro");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setActiveButton(Button activeBtn) {
        btnAccueil.getStyleClass().remove("active");
        btnCommande.getStyleClass().remove("active");
        btnHistorique.getStyleClass().remove("active");
        btnVerification.getStyleClass().remove("active");
        activeBtn.getStyleClass().add("active");
    }

    private void setContent(Node node) {
        node.setOpacity(0);
        contentArea.getChildren().clear();
        contentArea.getChildren().add(node);
        
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(300), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
