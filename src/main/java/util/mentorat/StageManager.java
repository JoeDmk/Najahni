package util.mentorat;

import javafx.stage.Stage;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class StageManager {
    private static final Set<Stage> stages = Collections.synchronizedSet(new HashSet<>());

    public static void addStage(Stage stage) {
        stages.add(stage);
    }

    public static void removeStage(Stage stage) {
        stages.remove(stage);
    }

    public static void closeAllStages() {
        for (Stage stage : new HashSet<>(stages)) {
            if (stage != null) {
                stage.close();
            }
        }
        stages.clear();
    }
}

