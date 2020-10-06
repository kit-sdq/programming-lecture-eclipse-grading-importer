package edu.kit.praktomat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.kit.praktomat.util.PathUtil;
import edu.kit.praktomat.util.WorkbenchWindowUtil;

public class CleanupHandler extends AbstractHandler {
	private static final Path PROJECT_SOLUTION_PATH = Path.of("solution");
	private static final Path PROJECT_TEMP_PATH = Path.of(".praktomat-plugin-temp");

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		final List<IProject> projects = WorkbenchWindowUtil.getSelectedProjects(window);

		if (projects.isEmpty()) {
			Alert.error("Please select a project.");
			return null;
		}

		projects.stream().forEach(this::cleanup);
		return null;
	}

	private void cleanup(final IProject project) {
		final Path projectPath = PathUtil.toPath(project.getLocation());
		final Path solutionPath = projectPath.resolve(PROJECT_SOLUTION_PATH);

		if (!Files.isDirectory(solutionPath)) {
			Alert.error("Missing '%s' in '%s'.", PROJECT_SOLUTION_PATH, projectPath);
			return;
		}

		final Path tempPath = projectPath.resolve(PROJECT_TEMP_PATH);

		try (final Stream<Path> nodes = Files.walk(solutionPath)) {
			if (Files.exists(tempPath)) {
				PathUtil.deleteRecursively(tempPath);
			}

			Files.createDirectories(tempPath);
			nodes.forEach(node -> processNode(solutionPath, tempPath, node));
		} catch (final IOException | CleanupException e) {
			e.printStackTrace();
			return;
		}

		try {
			PathUtil.deleteRecursively(solutionPath);
			PathUtil.copyRecursively(tempPath, solutionPath);
			PathUtil.deleteRecursively(tempPath);
		} catch (final IOException e) {
			e.printStackTrace();
		}

		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}

	private static void processNode(final Path solution, final Path temp, final Path node) throws CleanupException {
		if (Files.isRegularFile(node)) {
			processFile(solution, temp, node);
		} else if (Files.isDirectory(node)) {
			processDirectory(solution, temp, node);
		} else {
			Alert.error("Skipping unexpected node '%s'.", node);
		}
	}

	private static void processFile(final Path solution, final Path temp, final Path file) throws CleanupException {
		final Path relative;

		if (file.toString().endsWith(".java")) {
			final String source;

			try {
				source = Files.readString(file, StandardCharsets.ISO_8859_1);
			} catch (final IOException e) {
				throw new CleanupException(e);
			}

			relative = getPath(source);
		} else {
			final Path parent = file.getParent();
			relative = solution.relativize(parent);
		}

		final Path tempParent = temp.resolve(relative);
		final Path tempFile = tempParent.resolve(file.getFileName());

		try {
			Files.createDirectories(tempParent);
			Files.copy(file, tempFile);
		} catch (final IOException e) {
			throw new CleanupException(e);
		}
	}

	@SuppressWarnings("deprecation")
	private static Path getPath(final String source) {
		final ASTParser parser = ASTParser.newParser(AST.JLS11);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(source.toCharArray());

		final CompilationUnit unit = (CompilationUnit) parser.createAST(null);
		final PackageDeclaration packageDeclaration = unit.getPackage();

		if (packageDeclaration == null) {
			return Path.of("");
		}

		final String packageName = packageDeclaration.getName().getFullyQualifiedName();
		return Path.of("", packageName.split("\\."));
	}

	private static void processDirectory(final Path solution, final Path temp, final Path directory) {
		final Path relative = solution.relativize(directory);
		final Path tempDirectory = temp.resolve(relative);

		try {
			Files.createDirectories(tempDirectory);
		} catch (final IOException e) {
			throw new CleanupException(e);
		}
	}
}