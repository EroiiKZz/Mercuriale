package fr.imero.mercuriale.update;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateTest {
	@TempDir
	Path dir;

	@Test
	void readsTheVersionFromTheServedFileName() {
		String name = UpdateJar.fileName("attachment; filename=\"mercuriale-3.0+1.21.1.jar\"");
		assertEquals("mercuriale-3.0+1.21.1.jar", name);
		assertEquals("3.0+1.21.1", UpdateJar.version(name));
		assertEquals("3.0", UpdateJar.version("mercuriale-3.0.jar"));
		assertNull(UpdateJar.version("chestradar-3.0.jar"));
		assertNull(UpdateJar.version("mercuriale-../../evil.jar"));
		assertNull(UpdateJar.fileName(null));
	}

	@Test
	void acceptsOnlyTheAdvertisedJar() throws IOException {
		Path good = jar("mercuriale", "3.0", "• ajoute la mise à jour\n");
		assertEquals("• ajoute la mise à jour", UpdateJar.notes(good, "3.0"));
		assertThrows(IOException.class, () -> UpdateJar.notes(good, "3.1"));
		assertThrows(IOException.class, () -> UpdateJar.notes(jar("vitrine", "3.0", ""), "3.0"));
		Path broken = Files.writeString(dir.resolve("broken.jar"), "<html>");
		assertThrows(IOException.class, () -> UpdateJar.notes(broken, "3.0"));
	}

	@Test
	void swapsTheJarOnlyOnceTheOldOneIsGone() throws IOException, InterruptedException {
		Path mods = Files.createDirectories(dir.resolve("mods"));
		Path old = Files.writeString(mods.resolve("mercuriale-2.1.jar"), "old");
		Path staged = Files.writeString(dir.resolve("mercuriale-3.0.jar"), "new");
		assertTrue(Swap.replace(old, staged, mods.resolve("mercuriale-3.0.jar"), 1L));
		assertFalse(Files.exists(old));
		assertEquals("new", Files.readString(mods.resolve("mercuriale-3.0.jar")));
		assertFalse(Files.exists(mods.resolve("mercuriale-3.0.jar.part")));
	}

	private Path jar(String id, String version, String notes) throws IOException {
		Path jar = dir.resolve(id + "-" + version + "-" + System.nanoTime() + ".jar");
		try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(jar))) {
			entry(zip, "fabric.mod.json", "{\"id\": \"" + id + "\", \"version\": \"" + version + "\"}");
			entry(zip, UpdateJar.NOTES, notes);
		}
		return jar;
	}

	private static void entry(ZipOutputStream zip, String name, String text) throws IOException {
		zip.putNextEntry(new ZipEntry(name));
		zip.write(text.getBytes(StandardCharsets.UTF_8));
		zip.closeEntry();
	}
}
