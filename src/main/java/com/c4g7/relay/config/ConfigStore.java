package com.c4g7.relay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Reads and writes {@code relay.json}; writes go through a temp file so a crash never leaves half a config. */
public final class ConfigStore {
	private static final Logger LOGGER = LoggerFactory.getLogger("Relay");
	static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	private final Path file;

	public ConfigStore(Path directory) {
		this.file = directory.resolve("relay.json");
	}

	public Path file() {
		return this.file;
	}

	/**
	 * Loads the config. On first start it imports VelvetChat's config when one sits next to it, so
	 * existing windows, tabs and filters carry straight over.
	 */
	public RelayConfig load(Path velvetChatConfig) {
		if (Files.isRegularFile(this.file)) {
			try {
				RelayConfig config = GSON.fromJson(Files.readString(this.file, StandardCharsets.UTF_8), RelayConfig.class);
				if (config == null) {
					throw new JsonParseException("empty document");
				}
				config.sanitize();
				return config;
			} catch (IOException | RuntimeException broken) {
				LOGGER.error("{} could not be read, starting from defaults", this.file, broken);
				this.setAside();
				return RelayConfig.defaults();
			}
		}
		if (velvetChatConfig != null && Files.isRegularFile(velvetChatConfig)) {
			try {
				RelayConfig imported = VelvetChatImport.read(Files.readString(velvetChatConfig, StandardCharsets.UTF_8));
				LOGGER.info("Imported windows, tabs and filters from {}", velvetChatConfig);
				this.save(imported);
				return imported;
			} catch (IOException | RuntimeException failed) {
				LOGGER.warn("Could not import {}, starting from defaults", velvetChatConfig, failed);
			}
		}
		return RelayConfig.defaults();
	}

	public void save(RelayConfig config) {
		String document;
		try {
			document = GSON.toJson(config);
		} catch (RuntimeException failed) {
			LOGGER.error("Could not serialise the config; the file on disk is left alone", failed);
			return;
		}
		try {
			Files.createDirectories(this.file.getParent());
			Path temporary = this.file.resolveSibling(this.file.getFileName() + ".tmp");
			Files.writeString(temporary, document, StandardCharsets.UTF_8);
			try {
				Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException atomicUnsupported) {
				Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException failed) {
			LOGGER.error("Could not write {}", this.file, failed);
		}
	}

	private void setAside() {
		Path broken = this.file.resolveSibling(this.file.getFileName() + ".broken");
		try {
			Files.move(this.file, broken, StandardCopyOption.REPLACE_EXISTING);
			LOGGER.warn("The unreadable config was kept as {}", broken);
		} catch (IOException failed) {
			LOGGER.error("Could not set the unreadable config aside", failed);
		}
	}
}
