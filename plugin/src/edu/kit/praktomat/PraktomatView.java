package edu.kit.praktomat;

import static edu.kit.praktomat.util.PathUtil.fromPath;
import static edu.kit.praktomat.util.PathUtil.toPath;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

// "Set system library" build path/libraries

public class PraktomatView extends ViewPart {
	private static final int STYLE = SWT.BORDER;

	private Composite parent;
	private Tree tree;

	@Override
	public void createPartControl(final Composite parent) {
		final IToolBarManager toolBar = getViewSite().getActionBars().getToolBarManager();
		createToolBar(toolBar);

		this.parent = parent;
		createTree();

		handleReload();
	}

	private void createToolBar(final IToolBarManager toolBar) {
		final ImageDescriptor reloadIcon = PlatformUI.getWorkbench().getSharedImages()
				.getImageDescriptor(ISharedImages.IMG_ELCL_SYNCED);

		final IAction reloadAction = new Action("Reload", reloadIcon) {
			@Override
			public void run() {
				handleReload();
			}
		};

		toolBar.add(reloadAction);
	}

	private void createTree() {
		tree = new Tree(parent, STYLE);
		tree.addListener(SWT.MouseDoubleClick, this::handleDoubleClick);

		tree.addListener(SWT.MeasureItem, event -> {
			// Override with empty body to get expected behavior,
			// just as things should be.
			// https://www.eclipse.org/forums/index.php/t/257325/
		});
		
		// TODO
		final DragSource dragSource = new DragSource(tree, DND.DROP_COPY | DND.DROP_MOVE);
		
		dragSource.addDragListener(new DragSourceAdapter() {
			@Override
			public void dragSetData(final DragSourceEvent event) {
				event.data = tree.getSelection()[0].getText();
			}
		});
	}

	private void handleDoubleClick(final Event event) {
		// TODO Is there really not constant for this?
		if (event.button != 1) {
			return;
		}

		// TODO Seems like there should be a better way
		// http://www.java2s.com/Code/Java/SWT-JFace-Eclipse/DetectMouseDownInSWTTreeItem.htm
		// event.item == event.data == null
		final TreeItem item = tree.getItem(new Point(event.x, event.y));

		if (item == null) {
			return;
		}

		final Object data = item.getData();

		if (data instanceof PraktomatComments) {
			final PraktomatComments comments = (PraktomatComments) data;
			handleEditMessage(comments);
		} else if (data instanceof PraktomatComment) {
			final PraktomatComment comment = (PraktomatComment) data;
			handleShowComment(comment);
		}
	}

	private void handleEditMessage(final PraktomatComments comments) {
		final Shell shell = new Shell();
		final InputDialog dialog = new InputDialog(shell, "Edit message", "Edit all occurences of this message.",
				comments.getMessage(), null);
		final int returnCode = dialog.open();

		if (returnCode != InputDialog.OK) {
			return;
		}

		final String newMessage = dialog.getValue();
		comments.edit(newMessage);

		// TODO reuse PraktomatComments from comments.edit(..)?
		// Or reload everything?
		handleReload();
	}

	private void handleShowComment(final PraktomatComment comment) {
		// TODO duplicate code (3)
		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IPath path = fromPath(toPath(root.getLocation()).relativize(comment.getPath()));
		final IFile file = root.getFile(path);

		final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		final ITextEditor editor;

		try {
			editor = (ITextEditor) IDE.openEditor(page, file);
		} catch (final PartInitException e) {
			e.printStackTrace();
			return;
		}

		// final IDocument document =
		// editor.getDocumentProvider().getDocument(editor.getEditorInput());
		// editor.selectAndReveal(comment.getOffset(), comment.getLength());
		editor.setHighlightRange(comment.getOffset(), comment.getLength(), true);
	}

	private void handleReload() {
		tree.dispose();
		createTree();

		final IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		final IProject[] projects = root.getProjects();

		final Stream<PraktomatComments> results = Arrays.stream(projects).flatMap(this::parseComments)
				.collect(groupingBy(PraktomatComment::getMessage, toList())).entrySet().stream()
				.map(entry -> new PraktomatComments(entry.getKey(), entry.getValue()));

		results.forEach(comments -> {
			final TreeItem commentsItem = new TreeItem(tree, STYLE);
			commentsItem.setData(comments);
			commentsItem.setText(comments.getMessage());
			
			comments.getComments().stream().forEach(comment -> {
				final IPath path = fromPath(toPath(root.getLocation()).relativize(comment.getPath()));
				final IFile file = root.getFile(path);

				final TreeItem commentItem = new TreeItem(commentsItem, STYLE);
				commentItem.setData(comment);
				commentItem.setText(file.getFullPath().toString());
			});
		});

		parent.layout(true, true);
	}

	private Stream<PraktomatComment> parseComments(final IProject project) {
		final Path path = toPath(project.getLocation());

		try (final Stream<Path> nodes = Files.walk(path)) {
			return nodes.flatMap(this::parseComments).collect(toList()).stream();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		return Stream.empty();
	}

	private Stream<PraktomatComment> parseComments(final Path path) {
		if (!path.toString().endsWith(".java")) {
			return Stream.empty();
		}

		try {
			final String source = Files.readString(path, StandardCharsets.ISO_8859_1);
			return parseComments(path, source);
		} catch (IOException e) {
			e.printStackTrace();
			return Stream.empty();
		}
	}

	private Stream<PraktomatComment> parseComments(final Path path, final String source) {
		final Matcher matcher = Pattern.compile("//\\| *([^\\n\\r(\\r\\n))]+)").matcher(source);

		return matcher.results().map(result -> {
			final int group = 1;
			final int offset = result.start(group);
			final int length = result.end(group) - offset;
			final String message = result.group(group);
			return new PraktomatComment(path, offset, length, message);
		});
	}

	@Override
	public void setFocus() {
		tree.setFocus();
	}
}
