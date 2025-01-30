import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class CopyCatV2 {
	private static JFileChooser dirChooser;
	private static List<File> toCopy = new ArrayList<>();
	private static long totalBytes = 0;
	private static final DecimalFormat speedFormat = new DecimalFormat("0.00");

	public static void main(String[] args) {
		try {
			File fromDir = openDir();
			if (fromDir == null) {
				System.err.println("Quellverzeichnis nicht ausgewählt.");
				return;
			}
			File toDir = openDir();
			if (toDir == null) {
				System.err.println("Zielverzeichnis nicht ausgewählt.");
				return;
			}

			scanFiles(fromDir);
			if (toCopy.isEmpty()) {
				System.out.println("Keine Dateien zum Kopieren gefunden.");
				return;
			}

			copyFiles(fromDir, toCopy, toDir);
			System.out.println(toCopy.size() + " Dateien kopiert.");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	private static File openDir() {
		dirChooser = new JFileChooser();
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = dirChooser.showOpenDialog(null);
		return result == JFileChooser.APPROVE_OPTION ? dirChooser.getSelectedFile() : null;
	}

	private static void scanFiles(File dir) {
		if (dir == null || !dir.exists())
			return;
		File[] files = dir.listFiles();
		if (files == null)
			return;

		for (File f : files) {
			if (f.isFile()) {
				toCopy.add(f);
				totalBytes += f.length();
			} else if (f.isDirectory()) {
				scanFiles(f);
			}
		}
	}

	private static void copyFiles(File fromDir, List<File> files, File toDir) {
		JFrame mainFrame = new JFrame("0%");
		JPanel panel = new JPanel();
		panel.setLayout(new java.awt.BorderLayout());

		JProgressBar bar = new JProgressBar(0, 100);
		JLabel speedLabel = new JLabel("<html>Speed: 0 MB/s | 0 files/s<br/>Time: 00:00:00</html>");
		panel.add(bar, java.awt.BorderLayout.CENTER);
		panel.add(speedLabel, java.awt.BorderLayout.SOUTH);

		mainFrame.add(panel);
		mainFrame.setSize(300, 100);
		mainFrame.setLocationRelativeTo(null);
		mainFrame.setVisible(true);

		AtomicLong copiedBytes = new AtomicLong(0);
		AtomicInteger filesCopied = new AtomicInteger(0);
		final long startTime = System.currentTimeMillis();

		// Timer für Updates
		Timer speedTimer = new Timer(1000, e -> {
			long elapsedMs = System.currentTimeMillis() - startTime;
			long bytes = copiedBytes.get();
			int fileCount = filesCopied.get();

			// Zeitformatierung
			long seconds = elapsedMs / 1000 % 60;
			long minutes = (elapsedMs / (1000 * 60)) % 60;
			long hours = (elapsedMs / (1000 * 60 * 60));
			String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);

			// Geschwindigkeitsberechnung
			double mbps = (bytes / (1024.0 * 1024)) / (elapsedMs / 1000.0);
			double filesPerSec = fileCount / (elapsedMs / 1000.0);

			speedLabel.setText(String.format("<html>Speed: %s MB/s | %s files/s<br/>Time: %s</html>",
					speedFormat.format(mbps),
					speedFormat.format(filesPerSec),
					time));
		});
		speedTimer.start();

		int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

		for (File file : files) {
			executor.submit(() -> {
				try {
					Path sourcePath = file.toPath();
					Path relativePath = fromDir.toPath().relativize(sourcePath);
					Path targetPath = toDir.toPath().resolve(relativePath);

					Files.createDirectories(targetPath.getParent());
					Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

					long copied = Files.size(sourcePath);
					copiedBytes.addAndGet(copied);
					filesCopied.incrementAndGet();

					SwingUtilities.invokeLater(() -> {
						int progress = (int) ((copiedBytes.get() * 100) / totalBytes);
						bar.setValue(progress);
						mainFrame.setTitle(progress + "%");
					});
				} catch (IOException ex) {
					System.err.println("Fehler beim Kopieren: " + file.getAbsolutePath());
					ex.printStackTrace();
				}
			});
		}

		executor.shutdown();
		try {
			if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
				executor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
		}

		speedTimer.stop();
		mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
	}
}