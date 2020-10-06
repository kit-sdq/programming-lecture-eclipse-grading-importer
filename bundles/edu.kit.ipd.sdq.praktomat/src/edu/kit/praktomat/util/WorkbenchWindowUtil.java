package edu.kit.praktomat.util;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

public final class WorkbenchWindowUtil {
	@SuppressWarnings("unchecked")
	public static List<IProject> getSelectedProjects(final IWorkbenchWindow window) {
		// TODO Generalize to `IResource`?
		// See https://wiki.eclipse.org/FAQ_How_do_I_access_the_active_project%3F.
		final ISelection selection = window.getSelectionService().getSelection();

		if (!(selection instanceof IStructuredSelection)) {
			return Collections.emptyList();
		}

		final IStructuredSelection structured = (IStructuredSelection) selection;
		return StreamSupport.<Object>stream(structured.spliterator(), false).map(WorkbenchWindowUtil::toProject)
				.flatMap(Optional<IProject>::stream).collect(Collectors.toList());
	}

	private static Optional<IProject> toProject(final Object item) {
		if (!(item instanceof IAdaptable)) {
			return Optional.empty();
		}

		final IAdaptable adaptable = (IAdaptable) item;
		final IProject project = adaptable.<IProject>getAdapter(IProject.class);
		return Optional.ofNullable(project);
	}

	private WorkbenchWindowUtil() {
	}
}
