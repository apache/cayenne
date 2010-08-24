package org.apache.cayenne.modeler.init.platform;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.modeler.action.AboutAction;
import org.apache.cayenne.modeler.action.ActionManager;
import org.apache.cayenne.modeler.action.ConfigurePreferencesAction;
import org.apache.cayenne.modeler.action.ExitAction;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

public class OSXPlatformInitializer implements PlatformInitializer {

	@Inject
	protected ActionManager actionManager;

	public void initLookAndFeel() {

		// leave alone the look and feel. Presumably it is Aqua, since this
		// launcher can only be executed on Mac

		// configure special Mac menu handlers though...

		Application.getApplication().addAboutMenuItem();
		Application.getApplication().addPreferencesMenuItem();
		Application.getApplication().setEnabledAboutMenu(true);
		Application.getApplication().setEnabledPreferencesMenu(true);

		Application.getApplication().addApplicationListener(
				new MacEventsAdapter());
	}

	public void setupMenus(JFrame frame) {

		Set<Action> removeActions = new HashSet<Action>();
		removeActions.add(actionManager.getAction(ExitAction.class));
		removeActions.add(actionManager.getAction(AboutAction.class));
		removeActions.add(actionManager
				.getAction(ConfigurePreferencesAction.class));

		JMenuBar menuBar = frame.getJMenuBar();
		for (Component menu : menuBar.getComponents()) {
			if (menu instanceof JMenu) {
				JMenu jMenu = (JMenu) menu;

				Component[] menuItems = jMenu.getPopupMenu().getComponents();
				for (int i = 0; i < menuItems.length; i++) {

					if (menuItems[i] instanceof JMenuItem) {
						JMenuItem jMenuItem = (JMenuItem) menuItems[i];

						if (removeActions.contains(jMenuItem.getAction())) {
							jMenu.remove(jMenuItem);

							// this algorithm is pretty lame, but it works for
							// the current (as of 08.2010) menu layout
							if (i > 0
									&& i == menuItems.length - 1
									&& menuItems[i - 1] instanceof JPopupMenu.Separator) {
								jMenu.remove(i - 1);
							}
						}
					}
				}
			}
		}
	}

	class MacEventsAdapter extends ApplicationAdapter {

		public void handleAbout(ApplicationEvent e) {
			if (!e.isHandled()) {
				actionManager.getAction(AboutAction.class).showAboutDialog();
				e.setHandled(true);
			}
		}

		public void handlePreferences(ApplicationEvent e) {
			actionManager.getAction(ConfigurePreferencesAction.class)
					.showPreferencesDialog();
			e.setHandled(true);
		}

		public void handleQuit(ApplicationEvent e) {
			if (!e.isHandled()) {
				e.setHandled(true);
				actionManager.getAction(ExitAction.class).exit();
			}
		}
	}
}
