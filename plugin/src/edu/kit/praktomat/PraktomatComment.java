package edu.kit.praktomat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class PraktomatComment {
	private final Path path;
	private final int offset;
	private final int length;
	private final String message;

	public PraktomatComment(final Path path, final int offset, final int length, final String message) {
		this.path = path;
		this.offset = offset;
		this.length = length;
		this.message = message;
	}

	public PraktomatComment edit(final String newMessage) {
		try {
			final String source = Files.readString(path, StandardCharsets.ISO_8859_1);
			final String before = source.substring(0, offset);
			final String after = source.substring(offset + length);
			final String newSource = before + newMessage + after;
			Files.writeString(path, newSource, StandardCharsets.ISO_8859_1);
		} catch (final IOException e) {
			Alert.error("Error editing: %s", path);
		}

		return new PraktomatComment(path, offset, length, newMessage);
	}

	public Path getPath() {
		return path;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public String getMessage() {
		return message;
	}
}