import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
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
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import javax.swing.text.StyledDocument;

//import identic.ActionTableModel;

public class DeDup {

	private AppLogger log = AppLogger.getInstance();
	private AdapterDeDup adapterDeDup = new AdapterDeDup();

	private DefaultListModel<File> targetListModel = new DefaultListModel<File>();
	private DefaultListModel<File> skipListModel = new DefaultListModel<File>();

	private TargetTableModel targetTableModel = new TargetTableModel();

	public static final AtomicInteger fileID = new AtomicInteger(0);
	private ConcurrentHashMap<String, Integer> hashIDs = new ConcurrentHashMap<String, Integer>();
	private ConcurrentHashMap<String, Integer> hashCounts = new ConcurrentHashMap<String, Integer>();
	private Meld meld;

	private static final int PROCESSORS = Runtime.getRuntime().availableProcessors();

	private DefaultComboBoxModel<String> modelTypeFiles = new DefaultComboBoxModel<String>();
	// private String targetTypesRegex;
	private Pattern patternTargets = null;

	private ButtonGroup mainGroup = new ButtonGroup();

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
	/*                                                   */

	private void doStart() {
		log.infof("%nStarted census. Available Processors = %d%n", PROCESSORS);

		List<Path> netSkipModel = getNetSkip();
		List<Path> netTargetModel = getNetTargets();
		for (Path path : netTargetModel) {
			File folder = path.toFile();
			log.infof("netTargetModel : %s%n", folder.getAbsolutePath());

			ForkJoinPool poolTakeCensus = new ForkJoinPool(PROCESSORS);
			CensusTaker censusTaker = new CensusTaker(folder, lblActiveTypeFile.getText(), patternTargets,
					netSkipModel);
			poolTakeCensus.execute(censusTaker);
			while (!poolTakeCensus.isQuiescent()) {
				// psuedo join
			} // while
		} // for Target

		targetTableModel.clear();
		hashCounts.clear();
		hashIDs.clear();
		// Meld.clear();
		fileID.set(0);

		log.infof("%nStarted meld. Available Processors = %d%n", PROCESSORS);

		for (Path path : netTargetModel) {
			File folder = path.toFile();
			// log.infof("netTargetModel : %s%n", folder.getAbsolutePath());

			ForkJoinPool poolMeld = new ForkJoinPool(PROCESSORS);
			meld = new Meld(folder, lblActiveTypeFile.getText(), targetTableModel, hashCounts, hashIDs, netSkipModel);

			// meld.compute();
			poolMeld.execute(meld);
			while (!poolMeld.isQuiescent()) {
				// psuedo join
			} // while
		} // for Target

		mainTable.setModel(targetTableModel);
		setTableColumns();

		analyzeMainTable();

		setActionButtonsState(true);
	}// doStart

	private void analyzeMainTable() {
		int hashIndex = targetTableModel.getColumnIndex(TargetTableModel.HASH_KEY);
		int dupIndex = targetTableModel.getColumnIndex(TargetTableModel.DUP);
		String hashKey;
		int uniqueCount = 0;
		int hashCount = 0;
		for (int row = 0; row < targetTableModel.getRowCount(); row++) {
			hashKey = (String) targetTableModel.getValueAt(row, hashIndex);

			hashCount = hashCounts.get(hashKey);
			if (hashCount == 1) {
				uniqueCount++;
			} else { // if (hashCount > 1)
				targetTableModel.setValueAt(true, row, dupIndex);
			} // if
		} // for
		int targetCount = targetTableModel.getRowCount();
		setMainButtonTitle(btnTargets, targetCount);
		int distinctCount = hashCounts.size();
		setMainButtonTitle(btnDistinct, distinctCount);
		setMainButtonTitle(btnUnique, uniqueCount);
		int duplicateCounts = distinctCount - uniqueCount;
		setMainButtonTitle(btnDuplicates, duplicateCounts);

		mainTable.updateUI();
	}// analyzeMainTable

	private void doPrintResults() {
	}

	private void doCopy() {
	}

	private void doMove() {
	}

	private void doDelete() {
	}

	private void filterTargetTable(RowFilter filter) {
		if (mainTable.getRowCount() > 0) {
			TableRowSorter<TargetTableModel> tableRowSorter = new TableRowSorter<TargetTableModel>(targetTableModel);
			tableRowSorter.setRowFilter(filter);
			mainTable.setRowSorter(tableRowSorter);
		} // if rows
	}// filterTable

	private void doTargets() {
		mainTable.setRowSorter(null);
	}// doTargets

	private void doDistinct() {
		filterTargetTable(new DistinctFilter());
	}// doDistinct

	private void doUnique() {
		filterTargetTable(new UniqueFilter());
	}// doUnique

	private void doDuplicates() {
		filterTargetTable(new DuplicateFilter());
	}// doDuplicates

	//////////////////////////////////////////////
	private void setTableColumns() {
		mainTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
		for (int i = 0; i < mainTable.getColumnModel().getColumnCount(); i++) {
			TableColumn tc = mainTable.getColumnModel().getColumn(i);
			switch (i) {
			case 0: // Action
				tc.setMaxWidth(40);
				tc.setPreferredWidth(40);
				break;
			case 1: // Name
				break;
			case 2: // Directory
				tc.setMinWidth(120);
				tc.setPreferredWidth(220);
				break;
			case 3: // Size
				// tc.setMaxWidth(460);
				tc.setPreferredWidth(40);
				break;
			case 4: // Date
				// tc.setMaxWidth(100);
				tc.setPreferredWidth(40);
				break;
			case 5: // Dup
				tc.setMaxWidth(40);
				break;
			case 6: // ID
				tc.setMaxWidth(40);
				break;
			default:
				tc.setPreferredWidth(40);
			}// switch
		} // for each column
		mainTable.setRowSorter(new TableRowSorter<TargetTableModel>(targetTableModel));
	}// setTableColumns

	private void setupActionButtons() {
		setActionButtonsState(false);
		Component[] components = panelMWR2.getComponents();
		for (Component component : components) {
			if (component instanceof JToggleButton) {
				mainGroup.add((AbstractButton) component);
				setMainButtonTitle((AbstractButton) component, 0);
			} // if
		} // for
	}// setupActionButtons

	private void setActionButtonsState(boolean state) {
		btnCopy.setEnabled(state);
		btnMove.setEnabled(state);
		btnDelete.setEnabled(state);
	}// setActionButtons

	/*                                                   */
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

	private void setMainButtonTitle(AbstractButton button, int count) {
		String buttonName = button.getName();
		button.setText(String.format("%,d %s", count, buttonName));
	}// setMainTitle

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
			ArrayList<String> targetSuffixes = (ArrayList<String>) Files.readAllLines(Paths.get(typeFile));
			StringBuilder sb = new StringBuilder("(?)"); // case insensitive
			for (String targetSuffix : targetSuffixes) {
				targetSuffix.trim();
				sb.append(targetSuffix);
				sb.append("|");
			} // for each file type
			sb.deleteCharAt(sb.length() - 1); // remove trailing "|"

			String targetTypesRegex = sb.toString();
			patternTargets = Pattern.compile(targetTypesRegex);
		} catch (Exception e) {
			lblActiveTypeFile.setText("Failed to load target types");
			log.infof("Failed to read type file : %s%n", typeFile);
			patternTargets = null;
		} // try

	}// loadTargetRegex

	private void doEditTypeFiles() {
		String activeList = (String) modelTypeFiles.getSelectedItem();
		TypeFileMaintenance tfm = new TypeFileMaintenance(frameDeDup, this);
		tfm.setLocationRelativeTo(frameDeDup);
		tfm.setVisible(true);
		loadTypeFiles(modelTypeFiles);
		if (modelTypeFiles.getIndexOf(activeList) != -1) {
			modelTypeFiles.setSelectedItem(activeList);
		} // if
	}// doEditTypeFiles

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

	private DefaultListModel<File> getListModel(String name) {
		File file = new File(getAppDataDirectory(), name);
		DefaultListModel<File> ans = new DefaultListModel<File>();
		try {
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			ans = (DefaultListModel<File>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			log.error("Could not get Target/Skip list");
		} // try
		return ans;
	}// saveListModel
		///////////////////////////////////////////////////////////

	private void saveListModel(DefaultListModel<File> listModel, String name) {
		File file = new File(getAppDataDirectory(), name);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(listModel);
			fos.close();
			oos.close();
		} catch (Exception e) {
			log.error("Could not save Target/Skip list");
		} // try
	}// saveListModel

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
		myPrefs.putInt("tabbedPaneSelectedIndex", tabbedPane.getSelectedIndex());

		myPrefs.put("TypeFile", (String) modelTypeFiles.getSelectedItem());

		myPrefs = null;

		saveListModel(targetListModel, LIST_TARGETS);
		saveListModel(skipListModel, LIST_SKIP);
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
		frameDeDup.setSize(786, 900);
		frameDeDup.setLocation(myPrefs.getInt("LocX", 100), myPrefs.getInt("LocY", 100));
		splitPaneTargets.setDividerLocation(myPrefs.getInt("splitPaneTargets.divederLoc", 150));
		tabbedPane.setSelectedIndex(myPrefs.getInt("tabbedPaneSelectedIndex", 2));

		modelTypeFiles.setSelectedItem(myPrefs.get("TypeFile", "Pictures"));

		myPrefs = null;

		targetListModel = getListModel(LIST_TARGETS);
		skipListModel = getListModel(LIST_SKIP);

		loadTargetRegex();

		listTargets.setModel(targetListModel);
		listSkip.setModel(skipListModel);

		btnTargetRemove.setEnabled(false);
		btnSkipRemove.setEnabled(false);

		setupActionButtons();
		// setTableColumns();

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

		frameDeDup.setBounds(100, 100, 695, 789);
		frameDeDup.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent arg0) {
				appClose();
			}// windowClosing
		});
		frameDeDup.setIconImage(Toolkit.getDefaultToolkit().getImage(DeDup.class.getResource("/kcmkwm.png")));

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
		gbl_panelTop.columnWidths = new int[] { 0, 0, 0, 0 };
		gbl_panelTop.rowHeights = new int[] { 0, 0 };
		gbl_panelTop.columnWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
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
				doStart();
			}// actionPerformed
		});
		GridBagConstraints gbc_btnTest = new GridBagConstraints();
		gbc_btnTest.insets = new Insets(0, 0, 0, 5);
		gbc_btnTest.anchor = GridBagConstraints.NORTH;
		gbc_btnTest.gridx = 1;
		gbc_btnTest.gridy = 0;
		panelTop.add(btnTest, gbc_btnTest);

		JButton btnTest1 = new JButton("Test 1");
		btnTest1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (mainTable.getRowCount() > 0) {
					// -------------------------------------------
					// RowFilter<Object,Object> ditinctFilter = new RowFilter<Object,Object>(){
					// AbstractSet<String> hashIDs = new HashSet<String>();
					// int hashIndex = targetTableModel.getColumnIndex(TargetTableModel.HASH_KEY);
					//
					// @Override
					// public boolean include(Entry<? extends Object, ? extends Object> entry) {
					// String hashID = (String) entry.getValue(hashIndex);
					// if (hashIDs.contains(hashID)) {
					// return false;
					// } else
					// hashIDs.add(hashID);
					// return true;
					// }//include
					// };
					// -------------------------------------------
					DistinctFilter filter = new DistinctFilter();

					TableRowSorter<TargetTableModel> tableRowSorter = new TableRowSorter<TargetTableModel>(
							targetTableModel);
					tableRowSorter.setRowFilter(filter);
					mainTable.setRowSorter(tableRowSorter);
				} // if rows
			}// actionPerformed
		});
		GridBagConstraints gbc_btnTest1 = new GridBagConstraints();
		gbc_btnTest1.anchor = GridBagConstraints.NORTH;
		gbc_btnTest1.gridx = 2;
		gbc_btnTest1.gridy = 0;
		panelTop.add(btnTest1, gbc_btnTest1);

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

		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
		gbc_tabbedPane.fill = GridBagConstraints.BOTH;
		gbc_tabbedPane.gridx = 0;
		gbc_tabbedPane.gridy = 0;
		panelMain.add(tabbedPane, gbc_tabbedPane);

		JPanel panelMajorWork = new JPanel();
		tabbedPane.addTab("  Main  ", null, panelMajorWork, null);
		GridBagLayout gbl_panelMajorWork = new GridBagLayout();
		gbl_panelMajorWork.columnWidths = new int[] { 150, 0, 0 };
		gbl_panelMajorWork.rowHeights = new int[] { 0, 0 };
		gbl_panelMajorWork.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelMajorWork.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panelMajorWork.setLayout(gbl_panelMajorWork);

		JPanel panelMajorWorkLeft = new JPanel();
		panelMajorWorkLeft.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		GridBagConstraints gbc_panelMajorWorkLeft = new GridBagConstraints();
		gbc_panelMajorWorkLeft.insets = new Insets(0, 0, 0, 5);
		gbc_panelMajorWorkLeft.fill = GridBagConstraints.BOTH;
		gbc_panelMajorWorkLeft.gridx = 0;
		gbc_panelMajorWorkLeft.gridy = 0;
		panelMajorWork.add(panelMajorWorkLeft, gbc_panelMajorWorkLeft);
		GridBagLayout gbl_panelMajorWorkLeft = new GridBagLayout();
		gbl_panelMajorWorkLeft.columnWidths = new int[] { 0, 0 };
		gbl_panelMajorWorkLeft.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_panelMajorWorkLeft.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMajorWorkLeft.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelMajorWorkLeft.setLayout(gbl_panelMajorWorkLeft);

		JPanel panelMWR0 = new JPanel();
		panelMWR0.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		GridBagConstraints gbc_panelMWR0 = new GridBagConstraints();
		gbc_panelMWR0.insets = new Insets(0, 0, 5, 0);
		gbc_panelMWR0.fill = GridBagConstraints.BOTH;
		gbc_panelMWR0.gridx = 0;
		gbc_panelMWR0.gridy = 0;
		panelMajorWorkLeft.add(panelMWR0, gbc_panelMWR0);
		GridBagLayout gbl_panelMWR0 = new GridBagLayout();
		gbl_panelMWR0.columnWidths = new int[] { 0, 0 };
		gbl_panelMWR0.rowHeights = new int[] { 20, 0, 10, 0, 5, 0 };
		gbl_panelMWR0.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMWR0.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelMWR0.setLayout(gbl_panelMWR0);

		JButton btnStart = new JButton("Start");
		btnStart.addActionListener(adapterDeDup);
		btnStart.setActionCommand(BTN_START);
		GridBagConstraints gbc_btnStart = new GridBagConstraints();
		gbc_btnStart.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnStart.insets = new Insets(0, 0, 5, 0);
		gbc_btnStart.gridx = 0;
		gbc_btnStart.gridy = 1;
		panelMWR0.add(btnStart, gbc_btnStart);

		JButton btnPrintResult = new JButton("Print Result");
		btnPrintResult.addActionListener(adapterDeDup);
		btnPrintResult.setActionCommand(BTN_PRINT_RESULTS);
		GridBagConstraints gbc_btnPrintResult = new GridBagConstraints();
		gbc_btnPrintResult.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnPrintResult.insets = new Insets(0, 0, 5, 0);
		gbc_btnPrintResult.gridx = 0;
		gbc_btnPrintResult.gridy = 3;
		panelMWR0.add(btnPrintResult, gbc_btnPrintResult);

		Component verticalStrut_17 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_17 = new GridBagConstraints();
		gbc_verticalStrut_17.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_17.gridx = 0;
		gbc_verticalStrut_17.gridy = 1;
		panelMajorWorkLeft.add(verticalStrut_17, gbc_verticalStrut_17);

		panelMWR2 = new JPanel();
		panelMWR2.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		GridBagConstraints gbc_panelMWR2 = new GridBagConstraints();
		gbc_panelMWR2.insets = new Insets(0, 0, 5, 0);
		gbc_panelMWR2.fill = GridBagConstraints.BOTH;
		gbc_panelMWR2.gridx = 0;
		gbc_panelMWR2.gridy = 2;
		panelMajorWorkLeft.add(panelMWR2, gbc_panelMWR2);
		GridBagLayout gbl_panelMWR2 = new GridBagLayout();
		gbl_panelMWR2.columnWidths = new int[] { 0, 0 };
		gbl_panelMWR2.rowHeights = new int[] { 20, 0, 20, 0, 10, 0, 10, 0, 10, 0, 5, 0 };
		gbl_panelMWR2.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMWR2.rowWeights = new double[] { 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				Double.MIN_VALUE };
		panelMWR2.setLayout(gbl_panelMWR2);

		lblTotalFiles = new JLabel("0000 Total Files");
		lblTotalFiles.setForeground(Color.BLUE);
		lblTotalFiles.setHorizontalAlignment(SwingConstants.CENTER);
		GridBagConstraints gbc_lblTotalFiles = new GridBagConstraints();
		gbc_lblTotalFiles.fill = GridBagConstraints.VERTICAL;
		gbc_lblTotalFiles.insets = new Insets(0, 0, 5, 0);
		gbc_lblTotalFiles.gridx = 0;
		gbc_lblTotalFiles.gridy = 1;
		panelMWR2.add(lblTotalFiles, gbc_lblTotalFiles);

		btnTargets = new JToggleButton("Targets");
		btnTargets.addActionListener(adapterDeDup);
		btnTargets.setName("Targets");
		btnTargets.setActionCommand(BTN_TARGETS);
		GridBagConstraints gbc_btnTargets = new GridBagConstraints();
		gbc_btnTargets.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnTargets.insets = new Insets(0, 0, 5, 0);
		gbc_btnTargets.gridx = 0;
		gbc_btnTargets.gridy = 3;
		panelMWR2.add(btnTargets, gbc_btnTargets);

		btnDistinct = new JToggleButton("Distinct");
		btnDistinct.addActionListener(adapterDeDup);
		btnDistinct.setName("Distinct");
		btnDistinct.setActionCommand(BTN_DISTINCT);
		GridBagConstraints gbc_btnDistinct = new GridBagConstraints();
		gbc_btnDistinct.anchor = GridBagConstraints.NORTH;
		gbc_btnDistinct.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDistinct.insets = new Insets(0, 0, 5, 0);
		gbc_btnDistinct.gridx = 0;
		gbc_btnDistinct.gridy = 5;
		panelMWR2.add(btnDistinct, gbc_btnDistinct);

		btnUnique = new JToggleButton("Unique");
		btnUnique.addActionListener(adapterDeDup);
		btnUnique.setName("Unique");
		btnUnique.setActionCommand(BTN_UNIQUE);
		GridBagConstraints gbc_btnUnique = new GridBagConstraints();
		gbc_btnUnique.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnUnique.insets = new Insets(0, 0, 5, 0);
		gbc_btnUnique.gridx = 0;
		gbc_btnUnique.gridy = 7;
		panelMWR2.add(btnUnique, gbc_btnUnique);

		btnDuplicates = new JToggleButton("Duplicates");
		btnDuplicates.addActionListener(adapterDeDup);
		btnDuplicates.setName("Duplicates");
		btnDuplicates.setActionCommand(BTN_DUPLICATES);
		GridBagConstraints gbc_btnDuplicates = new GridBagConstraints();
		gbc_btnDuplicates.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDuplicates.insets = new Insets(0, 0, 5, 0);
		gbc_btnDuplicates.gridx = 0;
		gbc_btnDuplicates.gridy = 9;
		panelMWR2.add(btnDuplicates, gbc_btnDuplicates);

		Component verticalStrut_21 = Box.createVerticalStrut(20);
		GridBagConstraints gbc_verticalStrut_21 = new GridBagConstraints();
		gbc_verticalStrut_21.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_21.gridx = 0;
		gbc_verticalStrut_21.gridy = 3;
		panelMajorWorkLeft.add(verticalStrut_21, gbc_verticalStrut_21);

		JPanel panelMWR3 = new JPanel();
		panelMWR3.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
		GridBagConstraints gbc_panelMWR3 = new GridBagConstraints();
		gbc_panelMWR3.fill = GridBagConstraints.BOTH;
		gbc_panelMWR3.gridx = 0;
		gbc_panelMWR3.gridy = 4;
		panelMajorWorkLeft.add(panelMWR3, gbc_panelMWR3);
		GridBagLayout gbl_panelMWR3 = new GridBagLayout();
		gbl_panelMWR3.columnWidths = new int[] { 0 };
		gbl_panelMWR3.rowHeights = new int[] { 20, 0, 10, 0, 10, 0, 5 };
		gbl_panelMWR3.columnWeights = new double[] { 1.0 };
		gbl_panelMWR3.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 };
		panelMWR3.setLayout(gbl_panelMWR3);

		btnCopy = new JButton("Copy");
		btnCopy.addActionListener(adapterDeDup);
		btnCopy.setActionCommand(BTN_COPY);
		GridBagConstraints gbc_btnCopy = new GridBagConstraints();
		gbc_btnCopy.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnCopy.insets = new Insets(0, 0, 5, 0);
		gbc_btnCopy.gridx = 0;
		gbc_btnCopy.gridy = 1;
		panelMWR3.add(btnCopy, gbc_btnCopy);

		btnMove = new JButton("Move");
		btnMove.addActionListener(adapterDeDup);
		btnMove.setActionCommand(BTN_MOVE);
		GridBagConstraints gbc_btnMove = new GridBagConstraints();
		gbc_btnMove.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnMove.insets = new Insets(0, 0, 5, 0);
		gbc_btnMove.gridx = 0;
		gbc_btnMove.gridy = 3;
		panelMWR3.add(btnMove, gbc_btnMove);

		btnDelete = new JButton("Delete");
		btnDelete.addActionListener(adapterDeDup);
		btnDelete.setActionCommand(BTN_DELETE);
		GridBagConstraints gbc_btnDelete = new GridBagConstraints();
		gbc_btnDelete.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnDelete.insets = new Insets(0, 0, 5, 0);
		gbc_btnDelete.gridx = 0;
		gbc_btnDelete.gridy = 5;
		panelMWR3.add(btnDelete, gbc_btnDelete);

		JPanel panelMajorWorkRight = new JPanel();
		panelMajorWorkRight.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		GridBagConstraints gbc_panelMajorWorkRight = new GridBagConstraints();
		gbc_panelMajorWorkRight.fill = GridBagConstraints.BOTH;
		gbc_panelMajorWorkRight.gridx = 1;
		gbc_panelMajorWorkRight.gridy = 0;
		panelMajorWork.add(panelMajorWorkRight, gbc_panelMajorWorkRight);
		GridBagLayout gbl_panelMajorWorkRight = new GridBagLayout();
		gbl_panelMajorWorkRight.columnWidths = new int[] { 0, 0 };
		gbl_panelMajorWorkRight.rowHeights = new int[] { 0, 0 };
		gbl_panelMajorWorkRight.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelMajorWorkRight.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		panelMajorWorkRight.setLayout(gbl_panelMajorWorkRight);

		JScrollPane scrollPaneMajor = new JScrollPane();
		GridBagConstraints gbc_scrollPaneMajor = new GridBagConstraints();
		gbc_scrollPaneMajor.fill = GridBagConstraints.BOTH;
		gbc_scrollPaneMajor.gridx = 0;
		gbc_scrollPaneMajor.gridy = 0;
		panelMajorWorkRight.add(scrollPaneMajor, gbc_scrollPaneMajor);

		mainTable = new JTable();
		scrollPaneMajor.setViewportView(mainTable);

		splitPaneTargets = new JSplitPane();
		splitPaneTargets.setOrientation(JSplitPane.VERTICAL_SPLIT);
		tabbedPane.addTab("Target Folders", null, splitPaneTargets, null);

		JPanel panelTargets = new JPanel();
		splitPaneTargets.setLeftComponent(panelTargets);
		GridBagLayout gbl_panelTargets = new GridBagLayout();
		gbl_panelTargets.columnWidths = new int[] { 150, 0, 0 };
		gbl_panelTargets.rowHeights = new int[] { 0, 0, 0 };
		gbl_panelTargets.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_panelTargets.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		panelTargets.setLayout(gbl_panelTargets);

		JPanel panelTargets1 = new JPanel();
		panelTargets1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null),
				"Folders to Search", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
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
		panel_1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null), "Type Lists",
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
		panelSkip1.setBorder(new TitledBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null),
				"Folders to Skip", TitledBorder.CENTER, TitledBorder.ABOVE_TOP, null, new Color(0, 0, 0)));
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
		tabbedPane.addTab("Application Log", null, scrollLog, null);

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
			case BTN_START:
				doStart();
				break;
			case BTN_PRINT_RESULTS:
				doPrintResults();
				break;

			case BTN_COPY:
				doCopy();
				break;
			case BTN_MOVE:
				doMove();
				break;
			case BTN_DELETE:
				doDelete();
				break;

			case BTN_TARGETS:
				doTargets();
				break;
			case BTN_DISTINCT:
				doDistinct();
				break;
			case BTN_UNIQUE:
				doUnique();
				break;
			case BTN_DUPLICATES:
				doDuplicates();
				break;

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

	private final static String BTN_START = "btnStart";
	private final static String BTN_PRINT_RESULTS = "btnPrintResults";

	private final static String BTN_COPY = "btnCopy";
	private final static String BTN_MOVE = "btnMove";
	private final static String BTN_DELETE = "btnDelete:";

	private final static String BTN_TARGETS = "btnTargets";
	private final static String BTN_DISTINCT = "btnDistinct";
	private final static String BTN_UNIQUE = "btnUnique";
	private final static String BTN_DUPLICATES = "btnDuplicates";
	private final static String BTN_EXCLUDED = "btnExcluded";

	private final static String CB_TYPE_FILE = "cbTypeFile";

	private final static String EMPTY_STRING = "";

	private final static String LIST_TARGETS = "listTargets";
	private final static String LIST_SKIP = "listSkip";

	public final static String TYPEFILE = ".typeFile";
	private final static String PATH_SEPARATOR = File.separator;

	private static final String[] INITIAL_LISTFILES = new String[] { "VB" + TYPEFILE, "Music" + TYPEFILE,
			"MusicAndPictures" + TYPEFILE, "Pictures" + TYPEFILE };
	private JLabel lblActiveTypeFile;
	private JTabbedPane tabbedPane;
	private JLabel lblTotalFiles;
	private JToggleButton btnTargets;
	private JToggleButton btnDistinct;
	private JToggleButton btnUnique;
	private JToggleButton btnDuplicates;
	private JButton btnDelete;
	private JButton btnMove;
	private JButton btnCopy;
	private JPanel panelMWR2;
	private JTable mainTable;
	// -------------------------------------------
	RowFilter<Object, Object> ditinctFilter = new RowFilter<Object, Object>() {
		AbstractSet<String> hashIDs = new HashSet<String>();
		int hashIndex = targetTableModel.getColumnIndex(TargetTableModel.HASH_KEY);

		@Override
		public boolean include(Entry<? extends Object, ? extends Object> entry) {
			String hashID = (String) entry.getValue(hashIndex);
			if (hashIDs.contains(hashID)) {
				return false;
			} else
				hashIDs.add(hashID);
			return true;
		}// include
	};

	// ============================================
	class DistinctFilter extends RowFilter {
		int hashIndex;
		AbstractSet<String> hashIDs;

		public DistinctFilter() {
			this.hashIDs = new HashSet<String>();
			this.hashIndex = targetTableModel.getColumnIndex(TargetTableModel.HASH_KEY);

		}// constructor

		@Override
		public boolean include(Entry entry) {
			String hashID = (String) entry.getValue(hashIndex);
			if (hashIDs.contains(hashID)) {
				return false;
			} else
				hashIDs.add(hashID);
			return true;
		}// include
	}// class DistinctFilter
		// -------------------------------------------

	class UniqueFilter extends RowFilter {
		int dupIndex;

		UniqueFilter() {
			dupIndex = targetTableModel.getColumnIndex(TargetTableModel.DUP);
		}// constructor

		@Override
		public boolean include(Entry entry) {
			return !((boolean) entry.getValue(dupIndex));
		}// include
	}// class UniqueFilter
		// -------------------------------------------

	class DuplicateFilter extends RowFilter {
		int dupIndex;

		DuplicateFilter() {
			dupIndex = targetTableModel.getColumnIndex(TargetTableModel.DUP);
		}// constructor

		@Override
		public boolean include(Entry entry) {
			return ((boolean) entry.getValue(dupIndex));
		}// include
	}// class DuplicateFilter
		// ============================================

}// class DeDup
