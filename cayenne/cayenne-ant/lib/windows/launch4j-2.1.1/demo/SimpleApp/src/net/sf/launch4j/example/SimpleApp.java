/*
 	launch4j :: Cross-platform Java application wrapper for creating Windows native executables
 	Copyright (C) 2005 Grzegorz Kowal

	This program is free software; you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation; either version 2 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program; if not, write to the Free Software
	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package net.sf.launch4j.example;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

public class SimpleApp extends JFrame {
    public SimpleApp(String[] args) {
        super("Java Application");
        final int inset = 100;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setBounds (inset, inset,
				screenSize.width - inset * 2, screenSize.height - inset * 2);

		JMenu menu = new JMenu("File");
		menu.add(new JMenuItem("Open"));
		menu.add(new JMenuItem("Save"));
		JMenuBar mb = new JMenuBar();
		mb.setOpaque(true);
		mb.add(menu);
		setJMenuBar(mb);

		this.addWindowListener(new WindowAdapter() {
	    	public void windowClosing(WindowEvent e) {
				System.exit(0);
		}});
		setVisible(true);

		StringBuffer sb = new StringBuffer("Java version: ");
		sb.append(System.getProperty("java.version"));
		sb.append("\nJava home: ");
		sb.append(System.getProperty("java.home"));
		sb.append("\nCurrent dir: ");
		sb.append(System.getProperty("user.dir"));
		if (args.length > 0) {
			sb.append("\nArgs: ");
			for (int i = 0; i < args.length; i++) {
				sb.append(args[i]);
				sb.append(' ');
			}
		}
		JOptionPane.showMessageDialog(this,
				sb.toString(),
				"Info",
				JOptionPane.INFORMATION_MESSAGE);
    }

	public static void setLAF() {
		JFrame.setDefaultLookAndFeelDecorated(true);
		Toolkit.getDefaultToolkit().setDynamicLayout(true);
		System.setProperty("sun.awt.noerasebackground","true");
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch (Exception e) {
			System.err.println("Failed to set LookAndFeel");
		}
	}

   	public static void main(String[] args) {
   		setLAF();
		new SimpleApp(args);
	}
}
