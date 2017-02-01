package pkg;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.swing.*;

/**
 * Generates a Window that displays the image, allowing for visual representation
 * of what the targeting software is doing. Additionally, contains features that allow
 * the filtering and swapping of the image that is displayed.
 * 
 * @version 1.31.2017
 * @author DavidRein
 */
public class ImageDisplay implements ActionListener {
	
	/**Window used to contain the GUI of the program*/
	private JFrame frame;
	/**Component that contains the image that is displayed to the window*/
	private ImageIcon imgIcon;
	
	/**Container for the drop-down menus at the top of the window*/
	private JMenuBar menuBar;
	/**Menu located at the top of the window that holds various selectable items*/
	private JMenu menu1 , menu2;
	/**Selectable RadioButtons located in the first menu*/
	private JRadioButtonMenuItem rbMenuItem1 , rbMenuItem2 , rbMenuItem3;
	/**Selectable RadioButtons located in the second menu*/
	private JRadioButtonMenuItem rbMenuItem4 , rbMenuItem5 , rbMenuItem6;
	/**CheckBox located in the first menu that allows the saving of frames*/
	private JCheckBoxMenuItem cbMenuItem1;
	/**MenuItem that allows a window to pop-up when selected*/
	private JMenuItem menuItem1;
	/**Contains groups of RadioButtons and handles the 'one selection' limit*/
	private ButtonGroup buttonGroup1 , buttonGroup2;
	
	/**Generates the window & the initial layout and state of the components*/
	public ImageDisplay() {
		frame = new JFrame("Roboviking Vision");
		imgIcon = new ImageIcon();
		
		menuBar = new JMenuBar();
		
		//The following lines initialize everything pertaining to the first menu
		menu1 = new JMenu("View");
		cbMenuItem1 = new JCheckBoxMenuItem("Save Images");
		buttonGroup1 = new ButtonGroup();
		rbMenuItem1 = new JRadioButtonMenuItem("Original");
		rbMenuItem2 = new JRadioButtonMenuItem("Binary");
		rbMenuItem3 = new JRadioButtonMenuItem("Processed");
		
		rbMenuItem1.setSelected(true); //selects the original image to be displayed by default
		
		//These following lines add the RadioButtons to a button group
		buttonGroup1.add(rbMenuItem1);
		buttonGroup1.add(rbMenuItem2);
		buttonGroup1.add(rbMenuItem3);
		
		//These following lines add the items to the first menu
		menu1.add(cbMenuItem1);
		menu1.addSeparator(); //Draws line between components in the menu
		menu1.add(rbMenuItem1);
		menu1.add(rbMenuItem2);
		menu1.add(rbMenuItem3);
		
		//These following lines allow for the items to execute something when selected
		cbMenuItem1.addActionListener(this);
		rbMenuItem1.addActionListener(this);
		rbMenuItem2.addActionListener(this);
		rbMenuItem3.addActionListener(this);
		
		//The following lines pertain to all items contained in the second menu
		menu2 = new JMenu("Source");
		menuItem1 = new JMenuItem("Edit Path");
		buttonGroup2 = new ButtonGroup();
		rbMenuItem4 = new JRadioButtonMenuItem("Camera Feed");
		rbMenuItem5 = new JRadioButtonMenuItem("Image Stream");
		rbMenuItem6 = new JRadioButtonMenuItem("Image File");
		
		rbMenuItem6.setSelected(true);
		
		buttonGroup2.add(rbMenuItem4);
		buttonGroup2.add(rbMenuItem5);
		buttonGroup2.add(rbMenuItem6);
		
		menu2.add(menuItem1);
		menu2.addSeparator();
		menu2.add(rbMenuItem4);
		menu2.add(rbMenuItem5);
		menu2.add(rbMenuItem6);
		
		menuItem1.addActionListener(this);
		rbMenuItem4.addActionListener(this);
		rbMenuItem5.addActionListener(this);
		rbMenuItem6.addActionListener(this);
		
		//The following lines add the menus to the menuBar, which is then added to the window
		menuBar.add(menu1);
		menuBar.add(menu2);
		frame.setJMenuBar(menuBar);
		
		setImage(ImageGrabber.DEFAULT_IMAGE); //sets the initial image that is displayed to be the default image
		
		//The following lines set the characteristics of the frame
		frame.getContentPane().add(new JLabel(imgIcon)); //Adds the image to be displayed in the window
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //Allows us to close the program using the red X button
		frame.setLocationRelativeTo(null); //Sets the initial location of the window to the center of the screen
		frame.setResizable(false); //No permission to resize image (since there would be problems
		frame.setVisible(true); //The window is now visible on the screen
	}
	
	/**
	 * Set a BufferedImage to display in the generated window. This can be used to switch the image being displayed.
	 * @param buffImg - desired BufferedImage to display
	 */
	public void setImage(BufferedImage buffImg) {
		imgIcon.setImage(buffImg); //changes the desired image to display
		frame.setSize(buffImg.getWidth(), buffImg.getHeight()); //matches the frame size to the image size
		frame.repaint(); //refreshes the window to display new image
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		//The following line allows for images to be saved if the check box is checked
		if(e.getSource().equals(cbMenuItem1)) TargetingComputer.SAVE_IMAGES = cbMenuItem1.isSelected();
		
		//The following lines allow for switching the image between filters
		if(e.getSource().equals(rbMenuItem1)) TargetingComputer.setImageType(TargetingComputer.k_ORIGINAL_IMAGE);
		if(e.getSource().equals(rbMenuItem2)) TargetingComputer.setImageType(TargetingComputer.k_BINARY_IMAGE);
		if(e.getSource().equals(rbMenuItem3)) TargetingComputer.setImageType(TargetingComputer.k_PROCESSED_IMAGE);
		
		//The following line allows a window for user input to pop-up and change the source via path
		if(e.getSource().equals(menuItem1)) ImageGrabber.SOURCE_PATH = JOptionPane.showInputDialog(new JFrame("TEMP"),
				"What is the new source?");
		
		//The following lines allow for switching the image source
		if(e.getSource().equals(rbMenuItem4)) ImageGrabber.setSourceType(ImageGrabber.k_CAMERA_FEED);
		if(e.getSource().equals(rbMenuItem5)) ImageGrabber.setSourceType(ImageGrabber.k_IMAGE_STREAM);
		if(e.getSource().equals(rbMenuItem6)) ImageGrabber.setSourceType(ImageGrabber.k_IMAGE_FILE);
	}
}
