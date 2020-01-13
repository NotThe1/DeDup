import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.StyledDocument;

public class DeDup {

	AppLogger log = AppLogger.getInstance();
	AdapterDeDup adapterDeDup = new AdapterDeDup();
	DefaultListModel<String> targetListModel = new DefaultListModel<String>();
	DefaultListModel<String> skipListModel = new DefaultListModel<String>();

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
				} // try
			}// run
		});
	}// main

	private void doAddFolder(DefaultListModel<String> listModel) {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(true);
		if (fc.showDialog(frameDeDup, "pick folder(s)") == JFileChooser.APPROVE_OPTION) {
			File[] files = fc.getSelectedFiles();
			for (File file : files) {
				listModel.addElement(file.getAbsolutePath());
			} // files			
		}//if
	}// doAddFolder

	private void doRemoveFolder(JList<String> list) {
		DefaultListModel<String> listModel = (DefaultListModel<String>) list.getModel();
		int index = list.getMaxSelectionIndex();
		while (index != -1) {
			listModel.remove(index);
			index = list.getMaxSelectionIndex();
		} // while
	}// doAddFolder

	private void doClearFolders(DefaultListModel<String> listModel, JButton button) {
		listModel.clear();
		button.setEnabled(false);
	}// doAddFolder

	private void searchInit() {

	}// searchInit

	/**
	 * Create the application.
	 */
	public Preferences getPreferences() {
		return Preferences.userNodeForPackage(DeDup.class).node(this.getClass().getSimpleName());

	}// getPreferences

	public void appClose() {
		Preferences myPrefs = getPreferences();
		Dimension dim = frameDeDup.getSize();
		myPrefs.putInt("Height", dim.height);
		myPrefs.putInt("Width", dim.width);
		Point point = frameDeDup.getLocation();
		myPrefs.putInt("LocX", point.x);
		myPrefs.putInt("LocY", point.y);
		myPrefs.putInt("splitPaneTargets.divederLoc", splitPaneTargets.getDividerLocation());
		myPrefs = null;
	}// appClose

	public void appInit() {
		StyledDocument styledDoc = textLog.getStyledDocument();
		textLog.setFont(new Font("Arial", Font.PLAIN, 15));
		log.setDoc(styledDoc);
		log.setTextPane(textLog, "Disk Utility Log");
		log.infof("Starting DeDup  ", "");
		log.addTimeStamp();

		Preferences myPrefs = getPreferences();
		frameDeDup.setSize(myPrefs.getInt("Width", 761), myPrefs.getInt("Height", 693));
		frameDeDup.setLocation(myPrefs.getInt("LocX", 100), myPrefs.getInt("LocY", 100));
		splitPaneTargets.setDividerLocation(myPrefs.getInt("splitPaneTargets.divederLoc", 150));
		myPrefs = null;

		listTargets.setModel(targetListModel);
		listSkip.setModel(skipListModel);

		btnTargetRemove.setEnabled(false);
		btnSkipRemove.setEnabled(false);

	}// appInit

	public DeDup() {
		initialize();
		appInit();
	}// Constructor

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
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		frameDeDup.getContentPane().setLayout(gridBagLayout);

		JPanel panel = new JPanel();
		GridBagConstraints gbc_panel = new GridBagConstraints();
		gbc_panel.fill = GridBagConstraints.BOTH;
		gbc_panel.gridx = 0;
		gbc_panel.gridy = 0;
		frameDeDup.getContentPane().add(panel, gbc_panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[] { 0, 0 };
		gbl_panel.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_panel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel.rowWeights = new double[] { 0.0, 1.0, 0.0, Double.MIN_VALUE };
		panel.setLayout(gbl_panel);

		JPanel panelTop = new JPanel();
		panelTop.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		GridBagConstraints gbc_panelTop = new GridBagConstraints();
		gbc_panelTop.insets = new Insets(0, 0, 5, 0);
		gbc_panelTop.fill = GridBagConstraints.BOTH;
		gbc_panelTop.gridx = 0;
		gbc_panelTop.gridy = 0;
		panel.add(panelTop, gbc_panelTop);
		GridBagLayout gbl_panelTop = new GridBagLayout();
		gbl_panelTop.columnWidths = new int[] { 0, 0, 0 };
		gbl_panelTop.rowHeights = new int[] { 0, 0 };
		gbl_panelTop.columnWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		gbl_panelTop.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panelTop.setLayout(gbl_panelTop);

		Component verticalStrut = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut = new GridBagConstraints();
		gbc_verticalStrut.insets = new Insets(0, 0, 0, 5);
		gbc_verticalStrut.gridx = 0;
		gbc_verticalStrut.gridy = 0;
		panelTop.add(verticalStrut, gbc_verticalStrut);
		
		JButton btnTest = new JButton("test");
		btnTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				
			}//actionPerformed
		});
		GridBagConstraints gbc_btnTest = new GridBagConstraints();
		gbc_btnTest.anchor = GridBagConstraints.NORTH;
		gbc_btnTest.gridx = 1;
		gbc_btnTest.gridy = 0;
		panelTop.add(btnTest, gbc_btnTest);

		JPanel panelMain = new JPanel();
		GridBagConstraints gbc_panelMain = new GridBagConstraints();
		gbc_panelMain.insets = new Insets(0, 0, 5, 0);
		gbc_panelMain.fill = GridBagConstraints.BOTH;
		gbc_panelMain.gridx = 0;
		gbc_panelMain.gridy = 1;
		panel.add(panelMain, gbc_panelMain);
		GridBagLayout gbl_panelMain = new GridBagLayout();
		gbl_panelMain.columnWidths = new int[] { 0, 0 };
		gbl_panelMain.rowHeights = new int[] { 0, 0 };
		gbl_panelMain.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMain.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panelMain.setLayout(gbl_panelMain);

		JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 0;
		gbc_tabbedPane.gridy = 0;
		panelMain.add(tabbedPane, gbc_tabbedPane);

		splitPaneTargets = new JSplitPane();
		splitPaneTargets.setOrientation(JSplitPane.VERTICAL_SPLIT);
		tabbedPane.addTab("Target Folders", null, splitPaneTargets, null);

		JPanel panelTargets = new JPanel();
		splitPaneTargets.setLeftComponent(panelTargets);
		GridBagLayout gbl_panelTargets = new GridBagLayout();
		gbl_panelTargets.columnWidths = new int[] { 0, 0, 0 };
		gbl_panelTargets.rowHeights = new int[] { 0, 0 };
		gbl_panelTargets.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelTargets.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panelTargets.setLayout(gbl_panelTargets);

		JPanel panelTargets1 = new JPanel();
		panelTargets1.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), "Folders to Search",
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_panelTargets1 = new GridBagConstraints();
		gbc_panelTargets1.insets = new Insets(0, 0, 0, 5);
		gbc_panelTargets1.fill = GridBagConstraints.BOTH;
		gbc_panelTargets1.gridx = 0;
		gbc_panelTargets1.gridy = 0;
		panelTargets.add(panelTargets1, gbc_panelTargets1);
		GridBagLayout gbl_panelTargets1 = new GridBagLayout();
		gbl_panelTargets1.columnWidths = new int[] { 0, 0 };
		gbl_panelTargets1.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gbl_panelTargets1.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_panelTargets1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelTargets1.setLayout(gbl_panelTargets1);

		Component horizontalStrut = Box.createHorizontalStrut(120);
		GridBagConstraints gbc_horizontalStrut = new GridBagConstraints();
		gbc_horizontalStrut.insets = new Insets(0, 0, 5, 0);
		gbc_horizontalStrut.gridx = 0;
		gbc_horizontalStrut.gridy = 0;
		panelTargets1.add(horizontalStrut, gbc_horizontalStrut);

		Component verticalStrut_2 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_2 = new GridBagConstraints();
		gbc_verticalStrut_2.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_2.gridx = 0;
		gbc_verticalStrut_2.gridy = 1;
		panelTargets1.add(verticalStrut_2, gbc_verticalStrut_2);

		JButton btnTargetAdd = new JButton("ADD");
		btnTargetAdd.addActionListener(adapterDeDup);
		btnTargetAdd.setActionCommand(BTN_TARGET_ADD);
		GridBagConstraints gbc_btnTargetAdd = new GridBagConstraints();
		gbc_btnTargetAdd.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTargetAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnTargetAdd.gridx = 0;
		gbc_btnTargetAdd.gridy = 2;
		panelTargets1.add(btnTargetAdd, gbc_btnTargetAdd);

		btnTargetRemove = new JButton("REMOVE");
		btnTargetRemove.addActionListener(adapterDeDup);
		btnTargetRemove.setActionCommand(BTN_TARGET_REMOVE);
		GridBagConstraints gbc_btnTargetRemove = new GridBagConstraints();
		gbc_btnTargetRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTargetRemove.insets = new Insets(0, 0, 5, 0);
		gbc_btnTargetRemove.gridx = 0;
		gbc_btnTargetRemove.gridy = 3;
		panelTargets1.add(btnTargetRemove, gbc_btnTargetRemove);

		Component verticalStrut_4 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_4 = new GridBagConstraints();
		gbc_verticalStrut_4.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_4.gridx = 0;
		gbc_verticalStrut_4.gridy = 4;
		panelTargets1.add(verticalStrut_4, gbc_verticalStrut_4);

		JButton btnTargetClear = new JButton("CLEAR");
		btnTargetClear.addActionListener(adapterDeDup);
		btnTargetClear.setActionCommand(BTN_TARGET_CLEAR);
		GridBagConstraints gbc_btnTargetClear = new GridBagConstraints();
		gbc_btnTargetClear.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTargetClear.gridx = 0;
		gbc_btnTargetClear.gridy = 5;
		panelTargets1.add(btnTargetClear, gbc_btnTargetClear);

		JScrollPane scrollPaneTargets = new JScrollPane();
		GridBagConstraints gbc_scrollPaneTargets = new GridBagConstraints();
		gbc_scrollPaneTargets.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneTargets.gridx = 1;
		gbc_scrollPaneTargets.gridy = 0;
		panelTargets.add(scrollPaneTargets, gbc_scrollPaneTargets);

		listTargets = new JList<String>();
		listTargets.addListSelectionListener(adapterDeDup);
		listTargets.setName(LIST_TARGETS);
		scrollPaneTargets.setViewportView(listTargets);

		JPanel panelSkip = new JPanel();
		splitPaneTargets.setRightComponent(panelSkip);
		GridBagLayout gbl_panelSkip = new GridBagLayout();
		gbl_panelSkip.columnWidths = new int[] { 0, 0, 0 };
		gbl_panelSkip.rowHeights = new int[] { 0, 0 };
		gbl_panelSkip.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelSkip.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panelSkip.setLayout(gbl_panelSkip);

		JPanel panelSkip1 = new JPanel();
		panelSkip1.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), "Folders to Skip",
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, null));
		GridBagConstraints gbc_panelSkip1 = new GridBagConstraints();
		gbc_panelSkip1.insets = new Insets(0, 0, 0, 5);
		gbc_panelSkip1.fill = GridBagConstraints.BOTH;
		gbc_panelSkip1.gridx = 0;
		gbc_panelSkip1.gridy = 0;
		panelSkip.add(panelSkip1, gbc_panelSkip1);
		GridBagLayout gbl_panelSkip1 = new GridBagLayout();
		gbl_panelSkip1.columnWidths = new int[] { 0, 0 };
		gbl_panelSkip1.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gbl_panelSkip1.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_panelSkip1.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelSkip1.setLayout(gbl_panelSkip1);

		Component horizontalStrut_1 = Box.createHorizontalStrut(120);
		GridBagConstraints gbc_horizontalStrut_1 = new GridBagConstraints();
		gbc_horizontalStrut_1.insets = new Insets(0, 0, 5, 0);
		gbc_horizontalStrut_1.gridx = 0;
		gbc_horizontalStrut_1.gridy = 0;
		panelSkip1.add(horizontalStrut_1, gbc_horizontalStrut_1);

		Component verticalStrut_3 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_3 = new GridBagConstraints();
		gbc_verticalStrut_3.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_3.gridx = 0;
		gbc_verticalStrut_3.gridy = 1;
		panelSkip1.add(verticalStrut_3, gbc_verticalStrut_3);

		JButton btnSkipAdd = new JButton("ADD");
		btnSkipAdd.addActionListener(adapterDeDup);
		btnSkipAdd.setActionCommand(BTN_SKIP_ADD);
		GridBagConstraints gbc_btnSkipAdd = new GridBagConstraints();
		gbc_btnSkipAdd.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSkipAdd.insets = new Insets(0, 0, 5, 0);
		gbc_btnSkipAdd.gridx = 0;
		gbc_btnSkipAdd.gridy = 2;
		panelSkip1.add(btnSkipAdd, gbc_btnSkipAdd);

		btnSkipRemove = new JButton("REMOVE");
		btnSkipRemove.addActionListener(adapterDeDup);
		btnSkipRemove.setActionCommand(BTN_SKIP_REMOVE);
		GridBagConstraints gbc_btnSkipRemove = new GridBagConstraints();
		gbc_btnSkipRemove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSkipRemove.insets = new Insets(0, 0, 5, 0);
		gbc_btnSkipRemove.gridx = 0;
		gbc_btnSkipRemove.gridy = 3;
		panelSkip1.add(btnSkipRemove, gbc_btnSkipRemove);

		Component verticalStrut_5 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_5 = new GridBagConstraints();
		gbc_verticalStrut_5.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_5.gridx = 0;
		gbc_verticalStrut_5.gridy = 4;
		panelSkip1.add(verticalStrut_5, gbc_verticalStrut_5);

		JButton btnSkipClear = new JButton("CLEAR");
		btnSkipClear.addActionListener(adapterDeDup);
		btnSkipClear.setActionCommand(BTN_SKIP_CLEAR);
		GridBagConstraints gbc_btnSkipClear = new GridBagConstraints();
		gbc_btnSkipClear.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnSkipClear.gridx = 0;
		gbc_btnSkipClear.gridy = 5;
		panelSkip1.add(btnSkipClear, gbc_btnSkipClear);

		JScrollPane scrollPaneSkip = new JScrollPane();
		GridBagConstraints gbc_scrollPaneSkip = new GridBagConstraints();
		gbc_scrollPaneSkip.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneSkip.gridx = 1;
		gbc_scrollPaneSkip.gridy = 0;
		panelSkip.add(scrollPaneSkip, gbc_scrollPaneSkip);

		listSkip = new JList<String>();
		listSkip.addListSelectionListener(adapterDeDup);
		listSkip.setName(LIST_SKIP);
		scrollPaneSkip.setViewportView(listSkip);
		splitPaneTargets.setDividerLocation(150);

		JScrollPane scrollLog = new JScrollPane();
		tabbedPane.addTab("Log", null, scrollLog, null);

		textLog = new JTextPane();
		scrollLog.setViewportView(textLog);

		JLabel lblNewLabel = new JLabel("Application Log");
		lblNewLabel.setForeground(new Color(34, 139, 34));
		lblNewLabel.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		scrollLog.setColumnHeaderView(lblNewLabel);

		JPanel panelStatus = new JPanel();
		GridBagConstraints gbc_panelStatus = new GridBagConstraints();
		gbc_panelStatus.fill = GridBagConstraints.BOTH;
		gbc_panelStatus.gridx = 0;
		gbc_panelStatus.gridy = 2;
		panel.add(panelStatus, gbc_panelStatus);
		GridBagLayout gbl_panelStatus = new GridBagLayout();
		gbl_panelStatus.columnWidths = new int[] { 0, 0 };
		gbl_panelStatus.rowHeights = new int[] { 0, 0 };
		gbl_panelStatus.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
		gbl_panelStatus.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panelStatus.setLayout(gbl_panelStatus);

		Component verticalStrut_1 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_1 = new GridBagConstraints();
		gbc_verticalStrut_1.gridx = 0;
		gbc_verticalStrut_1.gridy = 0;
		panelStatus.add(verticalStrut_1, gbc_verticalStrut_1);

		JMenuBar menuBar = new JMenuBar();
		frameDeDup.setJMenuBar(menuBar);
	}// initialize

	class AdapterDeDup implements ActionListener, ListSelectionListener {

		/* ActionListener */
		@Override
		public void actionPerformed(ActionEvent ActionEvent) {
			switch (ActionEvent.getActionCommand()) {
			case BTN_TARGET_ADD:
				doAddFolder(targetListModel);
				break;
			case BTN_TARGET_REMOVE:
				log.infof("Action Command = %s%n", ActionEvent.getActionCommand());
				doRemoveFolder(listTargets);
				break;
			case BTN_TARGET_CLEAR:
				log.infof("Action Command = %s%n", ActionEvent.getActionCommand());
				doClearFolders(targetListModel, btnTargetRemove);
				break;
			case BTN_SKIP_ADD:
				doAddFolder(skipListModel);
				break;
			case BTN_SKIP_REMOVE:
				log.infof("Action Command = %s%n", ActionEvent.getActionCommand());
				doRemoveFolder(listSkip);
				break;
			case BTN_SKIP_CLEAR:
				log.infof("Action Command = %s%n", ActionEvent.getActionCommand());
				doClearFolders(skipListModel, btnSkipRemove);
				break;
			default:
				log.warnf("[actionPerformed] switch at default: Action Command = %s%n", ActionEvent.getActionCommand());
			}// switch action Command
		}// actionPerformed

		/* ListSelectionListener */

		@Override
		public void valueChanged(ListSelectionEvent listSelectionEvent) {
			if (listSelectionEvent.getValueIsAdjusting() == false) {
				// listSelectionEvent.
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) listSelectionEvent.getSource();
				switch (list.getName()) {
				case LIST_TARGETS:
					btnTargetRemove.setEnabled(true);
					break;
				case LIST_SKIP:
					btnSkipRemove.setEnabled(true);
					break;
				default:
					log.warnf("[valueChanged] switch at default: name = %s%n", list.getName());
				}// switch name
			} // if adjusting
		}// valueChanged

	}// class adapterDeDup

	private JFrame frameDeDup;
	private JTextPane textLog;
	private JSplitPane splitPaneTargets;
	private JList<String> listTargets;
	private JList<String> listSkip;

	private final static String BTN_TARGET_ADD = "btnTargetAdd";
	private final static String BTN_TARGET_REMOVE = "btnTargetRemove";
	private final static String BTN_TARGET_CLEAR = "btnTargetClear";
	private final static String BTN_SKIP_ADD = "btnSkipAdd";
	private final static String BTN_SKIP_REMOVE = "btnSkipRemove";
	private final static String BTN_SKIP_CLEAR = "btnSkipClear";

	private final static String LIST_TARGETS = "listTargets";
	private final static String LIST_SKIP = "listSkip";
	private JButton btnTargetRemove;
	private JButton btnSkipRemove;

}// class DeDup
