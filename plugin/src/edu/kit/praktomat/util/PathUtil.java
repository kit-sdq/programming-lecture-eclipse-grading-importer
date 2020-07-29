package edu.kit.praktomat.util;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IPath;

public final class PathUtil {
	public static Path toPath(final IPath path) {
		return path.toFile().toPath();
	}
	
	public static IPath fromPath(final Path path) {
		return new org.eclipse.core.runtime.Path(path.toString());
	}

	public static void deleteRecursively(final Path path) throws IOException {
		FileUtils.deleteDirectory(path.toFile());
	}

	public static void copyRecursively(final Path source, final Path destination) throws IOException {
		FileUtils.copyDirectory(source.toFile(), destination.toFile());
	}

	private PathUtil() {
	}
}
