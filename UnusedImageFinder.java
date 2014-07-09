import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.swing.*;

/**
 * What the program does:
 * - Lets FrameMaker users quickly find all images in their project folder that are 
 *   no longer used and can be deleted from their computer and/or source control. 
 *   (Right now my team does this using an annoying combination of a batch file and 
 *   a slow Excel file.)
 *
 * How it does it:
 * - It assumes you have a folder with all of a book's images in it, and that this 
 *   folder is directly inside the project folder.
 * - You specify the folder that has all the images in it.
 * - You paste in a FrameMaker-generated list of exported graphic references (i.e.,
 *   the list of used images). Each line in this list will look like this: 
 *   "Graphics/screenshot.png @ 120 dpi 100" [minus the quotes]
 * - When you click Show Unused Images, a new text area appears that shows all the 
 *   images in the specified image folder that are NOT mentioned in the list of 
 *   used images.
 *
 * How you can test it:
 * - Select any non-empty folder on your machine (e.g., "YourName/Home/").
 * - Click Show Unused Images. All the regular files in your specified folder will 
 *   appear (e.g., "MyFile.txt").
 * - Type something like "Home/MyFile.txt @" in the FrameMaker's List area.
 * - Click Show Unused Images again. The file should disappear from the Used Images
 *   area.
 */


public class UnusedImageFinder extends JPanel {
	
	private static JFrame frame;
	private static UnusedImageFinder mainPanel;
	private static JPanel resultsPanel;
	private JTextArea usedImagesArea;
	private static JTextArea unusedImagesArea;
	private JTextField dirBox;
	private String projectFolder;
	private ArrayList<String> allImages = new ArrayList<String>(); 
	private ArrayList<String> usedImages =  new ArrayList<String>();
	private ArrayList<String> unusedImages =  new ArrayList<String>();
	private BufferedReader bReader;
	
	private static void createAndShowGUI() {
		// Set the look and feel.
		initLookAndFeel();
		
		// Create and set up the window.
		frame = new JFrame("Unused Image Finder");
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Create and set up the main panel, and add it to the window's content
		// pane.
		mainPanel = new UnusedImageFinder();
		frame.getContentPane().add(mainPanel);
		
		// Make window just big enough for the components, then show it.
		frame.pack();
		frame.setVisible(true);
		
		// Create a panel that will show the results, but do not display it
		// yet. 
		resultsPanel = createResultsPanel();
	}
	
	
	/** Use the system's look and feel. */
	private static void initLookAndFeel() {
		try {
			 UIManager.setLookAndFeel(
					 UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception ex) {
			System.err.println("Could not apply the requested look and feel.");
		}
	}
	
	
	/** Creates the main panel with all initially visible components. */
	private UnusedImageFinder () {
		// Set the layout for the main panel.
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Create and configure the panel that will hold the first two GUI 
		// components.
		JPanel panel1 = new JPanel();
		panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));
		panel1.setBorder(BorderFactory.createTitledBorder("Images Folder"));
		
		// Create the text box that will display the path to the image folder
		// and add it to the first panel.
		dirBox = new JTextField("<Select the images folder.>", 35);
		dirBox.setEditable(false);
		panel1.add(dirBox);
		
		// Add a filler to separate the text box from the next component.
		Dimension dim = new Dimension(5, 0);
		panel1.add(new Box.Filler(dim, dim, dim));
		
		// Create the button for browsing to the image folder and add it to
		// the first panel. Also register it with its own ActionListener.
		JButton browseButton = new JButton("Browse...");
		panel1.add(browseButton);
		browseButton.addActionListener(new BrowseButtonListener());
		
		// Create and configure a second panel, which will hold the text box 
		// for pasting in the contents of FrameMaker's list of exported 
		// graphics.
		JPanel panel2 = new JPanel(new BorderLayout());
		panel2.setBorder(BorderFactory.createTitledBorder(
				"FrameMaker's List of Graphic References"));
		
		// Create the text box and add it to the second panel.
		usedImagesArea = new JTextArea(
				"<Paste list of imported graphic references here.>",
				5, 35);
		usedImagesArea.setLineWrap(true);
		JScrollPane scroller = new JScrollPane(usedImagesArea);
		scroller.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroller.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel2.add(scroller);

		// Create and configure a third panel, which will hold the button that 
		// displays the unused images.
		JPanel panel3 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		
		// Create the button and add it to the third panel.
		JButton unusedFilesButton = new JButton("Show Unused Images");
		unusedFilesButton.addActionListener(new ShowUnusedButtonListener());
		panel3.add(unusedFilesButton);
		
		// Add all three panels to the main panel.
		this.add(panel1);
		this.add(panel2);
		this.add(panel3);
	}
	
	
	/**
	 * When the user clicks the Browse button, this class creates a file
	 * browser so the user can select a folder. Then the names of all the files 
	 * in the folder are saved to a sorted list. 
	 */
	private class BrowseButtonListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			// Make sure you always start with an empty list.
			allImages.clear();
			
			// Create a file chooser and configure it to only show folders.
			JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			
			// If the user chooses a folder, save the names of all the files 
			// within it to an alphabetically sorted list of strings.
			if (fc.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
				File imageFolder = fc.getSelectedFile();
				dirBox.setText(imageFolder.getAbsolutePath());
				// Save name of the parent folder, which is assumed to contain the 
				// FrameMaker book file.
				projectFolder = fc.getSelectedFile().getName();
				// For each regular file in the folder, add it to the list.
				for (File file : imageFolder.listFiles()) {
					if (file.isFile()) {
						allImages.add(file.getName());
					}
				}
				// Sort the list so the unused image list ends up being alphabetical.
				Collections.sort(allImages, String.CASE_INSENSITIVE_ORDER);
			}
		}
	}
	
	
	/**
	 * When the user clicks the Show Unused Images button, this class creates 
	 * a list of unused images, adds the list to a text area, and then shows
	 * the area in the window.
	 */
	private class ShowUnusedButtonListener implements ActionListener {
		
		public void actionPerformed(ActionEvent e) {
			setUsedImages();
			setUnusedImages();
			showUnusedImages();
			usedImages.clear();
			unusedImages.clear();
		}
	}
	
	
	/** Save each line from the list of used images into a list. */
	private void setUsedImages() {
		StringReader sReader = new StringReader(usedImagesArea.getText());
		bReader = new BufferedReader(sReader);
		String line;
		try {
			while ((line = bReader.readLine()) != null) {
				usedImages.add(line);
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			try { 
				if (bReader != null) { bReader.close(); }
			}
			catch (IOException ex) { ex.printStackTrace(); }
		}
	}
	
	
	/** Save a list of all images that are not used. */
	private void setUnusedImages() {
		// For each file in allImages, check if it is mentioned in the list of
		// used images.
		for (String image : allImages) {
			boolean used = false;
			Pattern pattern = Pattern.compile("(?i)^\\Q"+ projectFolder + "/" + image + " \\E");
			for (String usedImage : usedImages) {
				usedImage = usedImage.toLowerCase();
				Matcher matcher = pattern.matcher(usedImage);
				// If the file is in the list, go to the next file in allImages.
				if (matcher.find()) {
					used = true;
					break;
				}
			}
			// If the file was never found in the list, add it to the list
			// of unused images.
			if (!used) {
				unusedImages.add(image);
			}
		}
	}
	
	
	/** Add the unused images to the results panel and show it. */
	private void showUnusedImages() {
		String list = "";
		if (unusedImages.size() > 0) {
			for (String unusedf : unusedImages) {
				list = list + unusedf + "\n";
			}
		} else {
			list = "<No unused images found.>";
		}
		unusedImagesArea.setText(list);
		mainPanel.add(resultsPanel);
		frame.pack();
	}
	
	
	/** Return the panel that will show the list of unused images. */
	private static JPanel createResultsPanel() {
		// Create a panel that will show the unused images.
		JPanel resultsPanel = new JPanel(new BorderLayout());
		resultsPanel.setBorder(BorderFactory.createTitledBorder(
				"Unused Images"));
		
		// Create and configure a text area that will contain the list of 
		// unused images.
		unusedImagesArea = new JTextArea(10, 30);
		unusedImagesArea.setLineWrap(true);
		JScrollPane scroller2 = new JScrollPane(unusedImagesArea);
		scroller2.setVerticalScrollBarPolicy(
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scroller2.setHorizontalScrollBarPolicy(
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		// Add the text area to the panel and return the panel.
		resultsPanel.add(scroller2);
		return resultsPanel;
	}
	
	public static void main(String[] args) {
		// Swing is not thread safe. So the application is run as a job within 
		// an event-dispatching thread.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}
}
