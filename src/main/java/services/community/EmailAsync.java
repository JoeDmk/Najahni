package services.community;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EmailAsync {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    private EmailAsync() {}

    public static void run(Runnable job) {
        EXEC.submit(job);
    }

    // Call this when app closes if you want (optional)
    public static void shutdown() {
        EXEC.shutdown();
    }
}
