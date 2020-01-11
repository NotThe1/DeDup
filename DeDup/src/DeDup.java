import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;

import javax.swing.JFrame;



public class DeDup {

	private JFrame frameDeDup;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DeDup window = new DeDup();
					window.frameDeDup.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}//try
			}//run
		});
	}//main

	/**
	 * Create the application.
	 */
	public Preferences getPreferences() {
		return Preferences.userNodeForPackage(DeDup.class).node(this.getClass().getSimpleName());
		
	}//getPreferences
	
	public void appClose() {
		Preferences myPrefs = getPreferences();
		Dimension dim = frameDeDup.getSize();
		myPrefs.putInt("Height", dim.height);
		myPrefs.putInt("Width", dim.width);
		Point point = frameDeDup.getLocation();
		myPrefs.putInt("LocX", point.x);
		myPrefs.putInt("LocY", point.y);

		
		myPrefs = null;
	}//appClose
	
	public void appInit() {
		Preferences myPrefs = getPreferences();
		frameDeDup.setSize(myPrefs.getInt("Width", 761), myPrefs.getInt("Height", 693));
		frameDeDup.setLocation(myPrefs.getInt("LocX", 100), myPrefs.getInt("LocY", 100));
		
		myPrefs = null;
		
		
	}//appInit
	
	
	public DeDup() {
		initialize();
		appInit();
	}//Constructor

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frameDeDup = new JFrame();
		frameDeDup.setTitle("DeDup -   version 0.0");

		frameDeDup.setBounds(100, 100, 450, 300);
		frameDeDup.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				appClose();
			}// windowClosing
		});
		frameDeDup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}//initialize

}//class DeDup
