package fr.idmc.client;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class FrontEndDataStore {
    private static FrontEndDataStore instance;
    private final ObservableList<EcranHistoriquePanel.LunetteProduite> commandes;

    private FrontEndDataStore() {
        commandes = FXCollections.observableArrayList(
            new EcranHistoriquePanel.LunetteProduite("Vision Pro X", "SN-9812A-VPX", "2026-05-14 02:14", "Terminé"),
            new EcranHistoriquePanel.LunetteProduite("Lite Shade", "SN-4451B-LTS", "2026-05-13 18:30", "Terminé")
        );
    }

    public static FrontEndDataStore getInstance() {
        if (instance == null) {
            instance = new FrontEndDataStore();
        }
        return instance;
    }

    public ObservableList<EcranHistoriquePanel.LunetteProduite> getCommandes() {
        return commandes;
    }

    public void ajouterCommande(String modele) {
        String randomId = UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        EcranHistoriquePanel.LunetteProduite nouvelleLunette = new EcranHistoriquePanel.LunetteProduite(
            modele, 
            "SN-" + randomId + "-PEN", 
            date, 
            "En cours de fabrication..."
        );
        
        commandes.add(0, nouvelleLunette); // Add at the top

        // Simulate backend processing taking 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            javafx.application.Platform.runLater(() -> {
                int index = commandes.indexOf(nouvelleLunette);
                if (index != -1) {
                    commandes.set(index, new EcranHistoriquePanel.LunetteProduite(
                        nouvelleLunette.getModele(),
                        nouvelleLunette.getNumeroSerie(),
                        nouvelleLunette.getDateFabrication(),
                        "Terminé ✅"
                    ));
                }
            });
        }).start();
    }
}
