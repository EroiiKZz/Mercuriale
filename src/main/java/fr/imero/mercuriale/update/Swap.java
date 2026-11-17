package fr.imero.mercuriale.update;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class Swap {
	private static final int ATTEMPTS = 30;
	private static final long WAIT_MS = 1000L;

	private Swap() {
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		ProcessHandle.of(Long.parseLong(args[0])).ifPresent(game -> game.onExit().join());
		replace(Path.of(args[1]), Path.of(args[2]), Path.of(args[3]), WAIT_MS);
	}

	static boolean replace(Path old, Path staged, Path target, long waitMs) throws IOException, InterruptedException {
		if (!delete(old, waitMs)) {
			return false;
		}
		Path part = target.resolveSibling(target.getFileName() + ".part");
		Files.copy(staged, part, StandardCopyOption.REPLACE_EXISTING);
		Files.move(part, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		return true;
	}

	private static boolean delete(Path file, long waitMs) throws InterruptedException {
		for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
			try {
				Files.deleteIfExists(file);
				return true;
			} catch (IOException locked) {
				Thread.sleep(waitMs);
			}
		}
		return false;
	}
}
