package fr.imero.mercuriale;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class AeternumFiles {
	private static final long POLL_MS = 500L;
	private static final long SHUTDOWN_WAIT_MS = 1000L;
	private static final String THREAD = "Aeternum fichiers (mercuriale)";

	private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(task -> {
		Thread thread = new Thread(task, THREAD);
		thread.setDaemon(true);
		thread.setPriority(Thread.MIN_PRIORITY);
		return thread;
	});

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			WORKER.shutdown();
			try {
				WORKER.awaitTermination(SHUTDOWN_WAIT_MS, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}, THREAD + " arrêt"));
	}

	private AeternumFiles() {
	}

	public static void watch(Runnable poll) {
		WORKER.scheduleWithFixedDelay(guarded(poll), POLL_MS, POLL_MS, TimeUnit.MILLISECONDS);
	}

	public static void run(Runnable task) {
		if (!WORKER.isShutdown()) {
			WORKER.execute(guarded(task));
		}
	}

	public static long modified(Path path) {
		try {
			return Files.isRegularFile(path) ? Files.getLastModifiedTime(path).toMillis() : 0L;
		} catch (Exception e) {
			return 0L;
		}
	}

	private static Runnable guarded(Runnable task) {
		return () -> {
			try {
				task.run();
			} catch (RuntimeException e) {
				Mercuriale.LOGGER.warn("[mercuriale] tâche de fichier en échec", e);
			}
		};
	}
}
