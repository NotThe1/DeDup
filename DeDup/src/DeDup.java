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
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

	private AppLogger log = AppLogger.getInstance();
	private AdapterDeDup adapterDeDup = new AdapterDeDup();
	private DefaultListModel<File> targetListModel = new DefaultListModel<File>();
	private DefaultListModel<File> skipListModel = new DefaultListModel<File>();

	private DefaultComboBoxModel<String> modelTypeFiles = new DefaultComboBoxModel<String>();
	private String targetTypesRegex;

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

	private void doAddFolder(DefaultListModel<File> listModel) {
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(true);
		if (fc.showDialog(frameDeDup, "pick folder(s)") == JFileChooser.APPROVE_OPTION) {
			File[] files = fc.getSelectedFiles();
			for (File file : files) {
				listModel.addElement(file);
			} // files
		} // if
	}// doAddFolder

	private int doRemoveFolder(JList<File> list) {
		DefaultListModel<File> listModel = (DefaultListModel<File>) list.getModel();
		int index = list.getMaxSelectionIndex();
		while (index != -1) {
			listModel.remove(index);
			index = list.getMaxSelectionIndex();
		} // while
		return listModel.getSize();
	}// doAddFolder

	private void doClearFolders(DefaultListModel<File> listModel, JButton button) {
		listModel.clear();
		button.setEnabled(false);
	}// doAddFolder

	private List<Path> getNetTargets() {
		ArrayList<PathAndCount> pacTargets = getPacTargets();

		// Order descending by the number of names in the path
		Collections.sort(pacTargets);

		// progress thru the Targets looking/setting isChild flag
		int index = 0;
		while (index != pacTargets.size()) {
			PathAndCount candidate = pacTargets.get(index++);
			for (int i = index; i < pacTargets.size(); i++) {
				candidate.testIsChild(pacTargets.get(i));
			} // for
		} // while

		return pacTargets.stream().filter(s -> s.isChild() == false).map(s -> s.path).collect(Collectors.toList());
	}// getNetTargets

	// create and load array with contents of targetList
	private ArrayList<PathAndCount> getPacTargets() {
		ArrayList<PathAndCount> pacTargets = new ArrayList<PathAndCount>();
		for (int i = 0; i < targetListModel.getSize(); i++) {
			pacTargets.add(i, new PathAndCount(targetListModel.getElementAt(i).toPath()));
		} // for
		return pacTargets;
	}// getPacTargets

	private List<Path> getNetSkip() {
		// create and load array with contents of skipList
		ArrayList<PathAndCount> pacSkips = new ArrayList<PathAndCount>();
		for (int i = 0; i < skipListModel.getSize(); i++) {
			pacSkips.add(i, new PathAndCount(skipListModel.getElementAt(i).toPath()));
		} // for

		// progress thru the Skips looking for children in Targets
		ArrayList<PathAndCount> pacTargets = getPacTargets();

		int targetCount = pacTargets.size();
		for (PathAndCount pacSkip : pacSkips) {
			for (int i = 0; i < targetCount; i++) {
				pacSkip.testIsChild(pacTargets.get(i));
			} // for
		} // for each

		return pacSkips.stream().filter(s -> s.isChild() == true).map(s -> s.path).collect(Collectors.toList());
	}// getNetSkip

	public String getAppDataDirectory() {
		String folder = System.getProperty("java.io.tmpdir");
		return folder.replace("Temp", "DeDup");
	}// getAppDataDirectory

	private void loadTargetRegex() {
		String activeTypeFile = (String) modelTypeFiles.getSelectedItem();
		String typeFile = getAppDataDirectory() + (String) modelTypeFiles.getSelectedItem() + TYPEFILE;
		lblActiveTypeFile.setText(activeTypeFile);

		try {
			ArrayList<String> targetSuffixes = (ArrayList) Files.readAllLines(Paths.get(typeFile));
			StringBuilder sb = new StringBuilder("(?)"); // case insensitive
			for (String targetSuffix : targetSuffixes) {
				targetSuffix.trim();
				sb.append(targetSuffix);
				sb.append("|");
			} // for each file type
			sb.deleteCharAt(sb.length() - 1); // remove trailing "|"

			targetTypesRegex = sb.toString();
		} catch (Exception e) {
			lblActiveTypeFile.setText("Failed to load target types");
			log.infof("Failed to read type file : %s%n", typeFile);
		} // try

	}// loadTargetRegex

	private void doEditTypeFiles() {
		String activeList = (String) modelTypeFiles.getSelectedItem();
		TypeFileMaintenance tfm = new TypeFileMaintenance(frameDeDup,this);
		tfm.setLocationRelativeTo(frameDeDup);
		tfm.setVisible(true);
		loadTypeFiles(modelTypeFiles);
		if (modelTypeFiles.getIndexOf(activeList)!= -1) {
			modelTypeFiles.setSelectedItem(activeList);
		}//if
	}//doEditTypeFiles
	
	public void loadTypeFiles(DefaultComboBoxModel<String> model) {
		File[] files = getTypeFiles();

		model.removeAllElements();
		String element = "";
		for (File file : files) {
			element = file.getName().replace(TYPEFILE, EMPTY_STRING);
			model.addElement(element);
		} // for each file
	}// loadTypeFiles

	public File[] getTypeFiles() {
		String appDataDirectory = getAppDataDirectory();
		File appDirectory = new File(appDataDirectory);

		if (!Files.exists(appDirectory.toPath())) {
			log.infof("appDataDirectory: %s does not exits, creatining it%n", appDataDirectory);
			try {
				Files.createDirectories(appDirectory.toPath());
			} catch (Exception e) {
				log.errorf("Failed to create appDataDirectory: %s%n", appDataDirectory);
			} // try
		} // if exists

		File[] files = appDirectory.listFiles(new ListFilter(TYPEFILE));

		if (files == null | files.length == 0) {
			for (String file : INITIAL_LISTFILES) {
				try {
					InputStream inStream = this.getClass().getResourceAsStream(file);
					Path targetPath = Paths.get(appDataDirectory, file);
					Files.copy(inStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					log.errorf("[initializeTypeFiles] file %s %n   %s%n", file, e.getMessage());
				} // try
			} // for each file
			files = appDirectory.listFiles(new ListFilter(TYPEFILE));
		} // if need to initialize
		return files;
	}// initializeTypeFiles

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

		myPrefs.put("TypeFile", (String) modelTypeFiles.getSelectedItem());

		myPrefs = null;
	}// appClose

	public void appInit() {
		StyledDocument styledDoc = textLog.getStyledDocument();
		textLog.setFont(new Font("Arial", Font.PLAIN, 15));
		log.setDoc(styledDoc);
		log.setTextPane(textLog, "Disk Utility Log");
		log.infof("Starting DeDup  ", "");
		log.addTimeStamp();

		loadTypeFiles(modelTypeFiles);
		cbTypeFiles.addItemListener(adapterDeDup);

		Preferences myPrefs = getPreferences();
		frameDeDup.setSize(myPrefs.getInt("Width", 761), myPrefs.getInt("Height", 693));
		frameDeDup.setLocation(myPrefs.getInt("LocX", 100), myPrefs.getInt("LocY", 100));
		splitPaneTargets.setDividerLocation(myPrefs.getInt("splitPaneTargets.divederLoc", 150));

		modelTypeFiles.setSelectedItem(myPrefs.get("TypeFile", "Pictures"));

		myPrefs = null;

		loadTargetRegex();

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

				List<Path> targetNet = getNetTargets();
				log.addNL(2);
				log.info("TargetNet");
				for (Path path : targetNet) {
					log.infof(" %s%n", path.toString());
				} // for

				List<Path> skipNet = getNetSkip();
				log.addNL(1);
				log.info("SkipNet");
				for (Path path : skipNet) {
					log.infof(" %s%n", path.toString());
				} // for

			}// actionPerformed
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
		gbl_panelTargets.columnWidths = new int[] {150, 0, 0};
		gbl_panelTargets.rowHeights = new int[] { 0, 0, 0 };
		gbl_panelTargets.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelTargets.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		panelTargets.setLayout(gbl_panelTargets);

		JPanel panelTargets1 = new JPanel();
		panelTargets1.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), "Folders to Search",
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_panelTargets1 = new GridBagConstraints();
		gbc_panelTargets1.anchor = GridBagConstraints.NORTH;
		gbc_panelTargets1.insets = new Insets(0, 0, 5, 5);
		gbc_panelTargets1.fill = GridBagConstraints.HORIZONTAL;
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
		gbc_scrollPaneTargets.gridheight = 2;
		gbc_scrollPaneTargets.insets = new Insets(0, 0, 5, 0);
		gbc_scrollPaneTargets.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneTargets.gridx = 1;
		gbc_scrollPaneTargets.gridy = 0;
		panelTargets.add(scrollPaneTargets, gbc_scrollPaneTargets);

		listTargets = new JList<File>();
		listTargets.addListSelectionListener(adapterDeDup);
		listTargets.setName(LIST_TARGETS);
		scrollPaneTargets.setViewportView(listTargets);

		JPanel panel_1 = new JPanel();
		panel_1.setBorder(new TitledBorder(new LineBorder(new Color(0, 0, 0), 1, true), "Type Lists",
				TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.insets = new Insets(0, 0, 0, 5);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 1;
		panelTargets.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[] { 0, 0 };
		gbl_panel_1.rowHeights = new int[] { 0, 0, 0, 0 };
		gbl_panel_1.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panel_1.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panel_1.setLayout(gbl_panel_1);

		cbTypeFiles = new JComboBox<String>(modelTypeFiles);
		cbTypeFiles.setActionCommand(CB_TYPE_FILE);
		// cbTypeFiles.addItemListener(adapterDeDup);
		GridBagConstraints gbc_cbTypeFiles = new GridBagConstraints();
		gbc_cbTypeFiles.insets = new Insets(0, 0, 5, 0);
		gbc_cbTypeFiles.fill = GridBagConstraints.HORIZONTAL;
		gbc_cbTypeFiles.gridx = 0;
		gbc_cbTypeFiles.gridy = 0;
		panel_1.add(cbTypeFiles, gbc_cbTypeFiles);

		Component verticalStrut_6 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_6 = new GridBagConstraints();
		gbc_verticalStrut_6.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_6.gridx = 0;
		gbc_verticalStrut_6.gridy = 1;
		panel_1.add(verticalStrut_6, gbc_verticalStrut_6);

		JButton btnEditTypeFiles = new JButton("Edit");
		btnEditTypeFiles.addActionListener(adapterDeDup);
		btnEditTypeFiles.setActionCommand(BTN_EDIT_TYPEFILES);
		GridBagConstraints gbc_btnEditTypeFiles = new GridBagConstraints();
		gbc_btnEditTypeFiles.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnEditTypeFiles.gridx = 0;
		gbc_btnEditTypeFiles.gridy = 2;
		panel_1.add(btnEditTypeFiles, gbc_btnEditTypeFiles);

		JPanel panelSkip = new JPanel();
		splitPaneTargets.setRightComponent(panelSkip);
		GridBagLayout gbl_panelSkip = new GridBagLayout();
		gbl_panelSkip.columnWidths = new int[] { 150, 0, 0 };
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

		listSkip = new JList<File>();
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
		gbl_panelStatus.columnWidths = new int[] { 0, 0, 0 };
		gbl_panelStatus.rowHeights = new int[] { 0, 0 };
		gbl_panelStatus.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelStatus.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		panelStatus.setLayout(gbl_panelStatus);

		Component verticalStrut_1 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_1 = new GridBagConstraints();
		gbc_verticalStrut_1.insets = new Insets(0, 0, 0, 5);
		gbc_verticalStrut_1.gridx = 0;
		gbc_verticalStrut_1.gridy = 0;
		panelStatus.add(verticalStrut_1, gbc_verticalStrut_1);

		lblActiveTypeFile = new JLabel("New label");
		lblActiveTypeFile.setForeground(Color.BLUE);
		lblActiveTypeFile.setFont(new Font("Times New Roman", Font.BOLD, 18));
		lblActiveTypeFile.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblActiveTypeFile = new GridBagConstraints();
		gbc_lblActiveTypeFile.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblActiveTypeFile.gridx = 1;
		gbc_lblActiveTypeFile.gridy = 0;
		panelStatus.add(lblActiveTypeFile, gbc_lblActiveTypeFile);

		JMenuBar menuBar = new JMenuBar();
		frameDeDup.setJMenuBar(menuBar);
	}// initialize

	// ---------------------------------------------------------
	// ---------------------------------------------------------

	static class ListFilter implements FilenameFilter {
		String suffix;

		public ListFilter(String suffix) {
			this.suffix = suffix;
		}// Constructor

		@Override
		public boolean accept(File arg0, String arg1) {
			if (arg1.endsWith(this.suffix)) {
				return true;
			} // if
			return false;
		}// accept
	}// ListFilter

	// ---------------------------------------------------------

	class PathAndCount implements Comparable<PathAndCount> {

		public Path path;
		public boolean isChild = false;

		public PathAndCount(Path path) {
			this.path = path;
		}// Constructor

		public String toString() {
			return path.toString();
		}// toString

		public int getCount() {
			return path.getNameCount();
		}// getCount

		public boolean testIsChild(PathAndCount pac) {
			this.isChild = this.isChild | this.path.startsWith(pac.path);
			return this.isChild;
		}// testIsChild

		public boolean isChild() {
			return this.isChild;
		}// isChild

		@Override // Descending sort
		public int compareTo(PathAndCount PandC) {
			return PandC.getCount() - this.getCount();
		}// compareTo Collections.sort(list)

	}// class PathAndCount

	// ---------------------------------------------------------

	class AdapterDeDup implements ActionListener, ListSelectionListener, ItemListener {

		/* ActionListener */
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			switch (actionEvent.getActionCommand()) {
			case BTN_TARGET_ADD:
				doAddFolder(targetListModel);
				break;
			case BTN_TARGET_REMOVE:
				log.infof("Action Command = %s%n", actionEvent.getActionCommand());
				if (doRemoveFolder(listTargets) == 0) {
					btnTargetRemove.setEnabled(false);
				} // if
				break;
			case BTN_TARGET_CLEAR:
				log.infof("Action Command = %s%n", actionEvent.getActionCommand());
				doClearFolders(targetListModel, btnTargetRemove);
				break;
			case BTN_SKIP_ADD:
				doAddFolder(skipListModel);
				break;
			case BTN_SKIP_REMOVE:
				log.infof("Action Command = %s%n", actionEvent.getActionCommand());
				if (doRemoveFolder(listSkip) == 0) {
					btnSkipRemove.setEnabled(false);
				} // if
				break;
			case BTN_SKIP_CLEAR:
				log.infof("Action Command = %s%n", actionEvent.getActionCommand());
				doClearFolders(skipListModel, btnSkipRemove);
				break;
			case BTN_EDIT_TYPEFILES:
				doEditTypeFiles();
				break;
			default:
				log.warnf("[actionPerformed] switch at default: Action Command = %s%n", actionEvent.getActionCommand());
			}// switch action Command
		}// actionPerformed

		/* ListSelectionListener */

		@Override
		public void valueChanged(ListSelectionEvent listSelectionEvent) {
			if (listSelectionEvent.getValueIsAdjusting() == false) {
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) listSelectionEvent.getSource();

				switch (list.getName()) {
				case LIST_TARGETS:
					btnTargetRemove.setEnabled(!list.isSelectionEmpty());
					break;
				case LIST_SKIP:
					btnSkipRemove.setEnabled(!list.isSelectionEmpty());
					break;
				default:
					log.warnf("[valueChanged] switch at default: name = %s%n", list.getName());
				}// switch name
			} // if adjusting
		}// valueChanged

		/* ItemListener */

		@Override
		public void itemStateChanged(ItemEvent itemEvent) {
			String actionCommand = ((JComboBox) itemEvent.getSource()).getActionCommand();
			switch (actionCommand) {
			case CB_TYPE_FILE:
				if (itemEvent.getStateChange() == ItemEvent.SELECTED) {
					loadTargetRegex();
				} // if
				break;
			default:
				log.warnf("[itemStateChanged] switch at default: actionCommand = %s%n", actionCommand);
			}// switch

		}// itemStateChanged

	}// class adapterDeDup

	// ---------------------------------------------------------

	private JFrame frameDeDup;
	private JTextPane textLog;
	private JSplitPane splitPaneTargets;
	private JList<File> listTargets;
	private JList<File> listSkip;
	private JButton btnTargetRemove;
	private JButton btnSkipRemove;
	private JComboBox cbTypeFiles;

	private final static String BTN_TARGET_ADD = "btnTargetAdd";
	private final static String BTN_TARGET_REMOVE = "btnTargetRemove";
	private final static String BTN_TARGET_CLEAR = "btnTargetClear";
	private final static String BTN_SKIP_ADD = "btnSkipAdd";
	private final static String BTN_SKIP_REMOVE = "btnSkipRemove";
	private final static String BTN_SKIP_CLEAR = "btnSkipClear";
	private final static String BTN_EDIT_TYPEFILES = "btnEditTypeFiles";

	private final static String CB_TYPE_FILE = "cbTypeFile";

	private final static String EMPTY_STRING = "";

	private final static String LIST_TARGETS = "listTargets";
	private final static String LIST_SKIP = "listSkip";

	public final static String TYPEFILE = ".typeFile";
	private final static String PATH_SEPARATOR = File.separator;
	/* @formatter:off */
	private static final String[] INITIAL_LISTFILES = new String[] { "VB" + TYPEFILE, "Music" + TYPEFILE,
			"MusicAndPictures" + TYPEFILE, "Pictures" + TYPEFILE };
	private JLabel lblActiveTypeFile;

	/* @formatter:on */

}// class DeDup
