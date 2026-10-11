package fr.imero.mercuriale.update;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.imero.mercuriale.Mercuriale;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class UpdateJar {
	public static final String NOTES = "mercuriale-changes.txt";

	private static final String MANIFEST = "fabric.mod.json";
	private static final Pattern NAME = Pattern.compile("^" + Mercuriale.MOD_ID + "-([0-9][\\w.+-]*)\\.jar$");
	private static final Pattern ATTACHMENT = Pattern.compile("filename=\"([^\"]+)\"");

	private UpdateJar() {
	}

	public static String fileName(String contentDisposition) {
		Matcher matcher = ATTACHMENT.matcher(contentDisposition == null ? "" : contentDisposition);
		return matcher.find() ? matcher.group(1) : null;
	}

	public static String version(String fileName) {
		Matcher matcher = NAME.matcher(fileName == null ? "" : fileName);
		return matcher.matches() ? matcher.group(1) : null;
	}

	public static String notes(Path jar, String version) throws IOException {
		try (ZipFile zip = new ZipFile(jar.toFile())) {
			String manifest = read(zip, MANIFEST);
			if (manifest == null) {
				throw new IOException("no " + MANIFEST + " in " + jar.getFileName());
			}
			JsonObject root = JsonParser.parseString(manifest).getAsJsonObject();
			if (!Mercuriale.MOD_ID.equals(string(root, "id")) || !version.equals(string(root, "version"))) {
				throw new IOException(jar.getFileName() + " is not " + Mercuriale.MOD_ID + " " + version);
			}
			String notes = read(zip, NOTES);
			return notes == null ? "" : notes.strip();
		}
	}

	private static String read(ZipFile zip, String name) throws IOException {
		ZipEntry entry = zip.getEntry(name);
		if (entry == null) {
			return null;
		}
		try (InputStream in = zip.getInputStream(entry)) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private static String string(JsonObject object, String name) {
		JsonElement element = object.get(name);
		return element == null || !element.isJsonPrimitive() ? "" : element.getAsString();
	}
}
