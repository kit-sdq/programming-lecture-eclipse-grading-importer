package edu.kit.praktomat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import edu.kit.praktomat.util.PathUtil;
import edu.kit.praktomat.util.WorkbenchWindowUtil;

public class TerminalHandler extends AbstractHandler {
	private static final Path PROJECT_SOLUTION_PATH = Paths.get("solution");
	private static final Path PACKAGE_PATH = Paths.get("edu", "kit", "informatik");
	private static final Path TERMINAL_PATH = Paths.get("Terminal.java");

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		final List<IProject> projects = WorkbenchWindowUtil.getSelectedProjects(window);

		if (projects.isEmpty()) {
			Alert.error("Please select a project.");
			return null;
		}

		projects.stream().forEach(this::fixTerminal);
		return null;
	}

	private void fixTerminal(final IProject project) {
		final Path projectPath = PathUtil.toPath(project.getLocation());
		final Path solutionPath = projectPath.resolve(PROJECT_SOLUTION_PATH);

		if (!Files.isDirectory(solutionPath)) {
			Alert.error("Missing '%s' in '%s'.", PROJECT_SOLUTION_PATH, projectPath);
			return;
		}

		final Path packagePath = solutionPath.resolve(PACKAGE_PATH);
		final Path terminalPath = packagePath.resolve(TERMINAL_PATH);

		if (Files.exists(terminalPath)) {
			return;
		}

		try {
			Files.createDirectories(packagePath);
			final Path terminal = Paths.get("C:\\\\Users\\Marvin\\Desktop\\Terminal.java");
			Files.copy(terminal, terminalPath);
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (final CoreException e) {
			e.printStackTrace();
		}
	}
}
