import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class CopyCat {
	private static JFileChooser dirChooser;
	private static List<File> toCopy = new ArrayList<File>();

	public static void main(String[] args) {
		try {
			File fromDir = openDir();
			System.out.println(fromDir.getName());
			File toDir = openDir();
			System.out.println(toDir.getName());
			scanFiles(fromDir);
			copyFiles(toCopy, toDir);
			System.out.println(toCopy.size() + " Files Copyed");
		} catch (Exception e) {
		}
		System.exit(0);
	}

	// Get the directory to copy from/to
	private static File openDir() {
		dirChooser = new JFileChooser();
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		dirChooser.showOpenDialog(null);
		return dirChooser.getSelectedFile();
	}

	// Scan Files to add for copy
	private static void scanFiles(File dir) {
		if (!dir.exists() || dir.listFiles() == null)
			return;
		for (File f : dir.listFiles()) {
			if (!f.exists())
				return;
			if (f.isFile())
				toCopy.add(f);
			if (f.isDirectory())
				scanFiles(f);
		}
	}

	/*
	 * GUI for the progress bar
	 * and the copy function
	 */
	private static void copyFiles(List<File> files, File to) {
		JFrame mainFrame = new JFrame("0%");
		JPanel jPanel = new JPanel();
		JProgressBar bar = new JProgressBar();
		bar.setValue(0);
		jPanel.add(bar);
		mainFrame.add(jPanel);
		mainFrame.setSize(250, 75);
		mainFrame.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width / 2,
				Toolkit.getDefaultToolkit().getScreenSize().height / 2);
		mainFrame.setVisible(true);
		for (int i = 0; i < toCopy.size(); i++) {
			String s = to.getPath() + "\\" + toCopy.get(i).getParentFile().getName() + "." + toCopy.get(i).getName();
			int pro = (int) ((double) i / toCopy.size() * 100);
			mainFrame.setTitle(String.valueOf(pro + "%"));
			bar.setValue(pro);
			try {
				Files.copy(toCopy.get(i).toPath(), new File(s).toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				System.err.println("Copy Error " + e.getStackTrace());
			}

		}
		mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
	}
}
