package edu.kit.praktomat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public final class Alert {
	public static void error(final String format, final Object... args) {
		error(String.format(format, args));
	}

	public static void error(final String message) {
		final Shell shell = new Shell();
		final MessageBox alert = new MessageBox(shell, SWT.ICON_ERROR | SWT.OK);
		alert.setText("Praktomat Eclipse Plugin");
		alert.setMessage(message);
		alert.open();
	}

	private Alert() {
	}
}