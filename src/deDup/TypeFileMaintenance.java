package deDup;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import appLogger.AppLogger;

public class TypeFileMaintenance extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private AppLogger log = AppLogger.getInstance();
	AdapterTypeFileMaintenance adapterTypeFileMaintenance = new AdapterTypeFileMaintenance();

	private DefaultComboBoxModel<String> modelTypeFiles = new DefaultComboBoxModel<String>();
	private DefaultListModel<String> modelTypes = new DefaultListModel<String>();

	private String newListName = EMPTY_STRING;

	/**
	 * Launch the application.
	 */
	// public static void main(String[] args) {
	// try {
	// TypeFileMaintenance dialog = new TypeFileMaintenance();
	// dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	// dialog.setVisible(true);
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }// main

	private void loadTypeList() {
		String activeTypeFile = (String) modelTypeFiles.getSelectedItem();
		Path typeFilePath = getTypeFilePath(activeTypeFile);
		lblActiveList.setText(activeTypeFile);
		modelTypes.removeAllElements();
		ArrayList<String> types;
		try {
			types = (ArrayList<String>) Files.readAllLines(typeFilePath, StandardCharsets.UTF_8);
			for (String type : types) {
				String type1 = type.replaceFirst("\\uFEFF", ""); // BOM
				modelTypes.addElement(type1.trim().toUpperCase());
			} // for
		} catch (IOException e) {
			lblActiveList.setText("Failed to load");
			log.infof("Failed to read type file : %s%n", typeFilePath.toString());
		} // try
	}// loadTypeList

	private void doNew(String listName) {
		if (modelTypeFiles.getIndexOf(listName) == -1) {
			modelTypeFiles.addElement(listName);
			modelTypeFiles.setSelectedItem(listName);
			modelTypes.removeAllElements();
			lblActiveList.setText(listName);
			lblActiveList.setForeground(Color.RED);
			newListName = listName;
		} else {
			JOptionPane.showMessageDialog(this, "Duplicate List Name: " + listName);
		} //if

	}// doNew

	private void doChangeList(String actionCommand, int stateChange) {
		switch (actionCommand) {
		case CB_TYPE_FILE:
			if (stateChange == ItemEvent.SELECTED) {
				if (!newListName.equals(EMPTY_STRING)) {
					switch (JOptionPane.showConfirmDialog(null, "Do you want to Save new List: " + newListName)) {
					case JOptionPane.OK_OPTION:
						String currentListName = (String) modelTypeFiles.getSelectedItem();
						String newListName1 = newListName;
						newListName = EMPTY_STRING;
						doSave(newListName1);
						modelTypeFiles.setSelectedItem(currentListName);
						lblActiveList.setForeground(Color.BLUE);
						break;

					case JOptionPane.CANCEL_OPTION:
						modelTypeFiles.setSelectedItem(newListName);
						return;

					case JOptionPane.NO_OPTION:
						modelTypeFiles.removeElement(newListName);
						newListName = EMPTY_STRING;
						lblActiveList.setForeground(Color.BLUE);
						break;

					default:
						log.warnf("[itemStateChanged.showConfirmDialog] in default Bad return ", "");
					}// switch

				} // if
				loadTypeList();
			} // if
			break;
		default:
			log.warnf("[AdapterTypeFileMaintenance.itemStateChanged] switch at default: actionCommand = %s%n",
					actionCommand);
		}// switch
	}// doChangeList

	private void doListDelete() {
		String targetList = (String) modelTypeFiles.getSelectedItem();
		Path typeFilePath = getTypeFilePath(targetList);
		try {
			Files.deleteIfExists(typeFilePath);
			modelTypeFiles.removeElement(targetList);
			// loadTypeList();
		} catch (IOException e) {
			log.errorf("Failed to Remove: %s%n%s%n", targetList, e.getMessage());
		} // try
	}// doListDelete

	private void doSave(String listName) {
		Vector<String> vector = new Vector<String>();
		for (int i = 0; i < modelTypes.getSize(); i++) {
			vector.add(modelTypes.get(i));
		} // for
		String fileName = deDup.getAppDataDirectory() + listName + DeDup.TYPEFILE;
		// System.out.println(fileName);
		try {
			Files.write(Paths.get(fileName), vector, StandardOpenOption.CREATE);
			deDup.loadTypeFiles(modelTypeFiles);
			modelTypeFiles.setSelectedItem(listName);
		} catch (IOException e) {
			log.errorf("Failed Saving type File: %s%n%s%n", listName, e.getMessage());
			e.printStackTrace();
		} // try
		txtItem.setText(EMPTY_STRING);
	}// save

	private void doAddItem() {
		String item = txtItem.getText().trim().toUpperCase();
		if (!modelTypes.contains(item)) {
			modelTypes.addElement(item);
		} // if
		txtItem.setText(EMPTY_STRING);
	}// doAddItem

	private int doRemoveItems() {
		DefaultListModel<String> listModel = (DefaultListModel<String>) listTypes.getModel();
		int index = listTypes.getMaxSelectionIndex();
		while (index != -1) {
			listModel.remove(index);
			index = listTypes.getMaxSelectionIndex();
		} // while
		return listModel.getSize();
	}// doRemoveItems

	private String getListName(String title) {
		String listName = JOptionPane.showInputDialog(this, "Enter list name");
		if (listName != null) {
			listName = listName.replaceAll(SPACE, EMPTY_STRING);
			if (listName.equals(EMPTY_STRING)) {
				listName = null;
			} // inner if
		} // outer if
		return listName;
	}// getistName

	private Path getTypeFilePath(String listName) {
		// String activeTypeFile = (String) modelTypeFiles.getSelectedItem();
		String listPath = deDup.getAppDataDirectory() + (String) modelTypeFiles.getSelectedItem() + DeDup.TYPEFILE;
		return Paths.get(listPath);
	}// getTypeFilePath

	private void appClose() {
		this.dispose();
	}//

	/**
	 * Create the dialog.
	 */
	private void appInit() {
		// File[] files = deDup.getTypeFiles();
		cbTypeFiles.setModel(modelTypeFiles);
		deDup.loadTypeFiles(modelTypeFiles);
		cbTypeFiles.addItemListener(adapterTypeFileMaintenance);
		listTypes.setModel(modelTypes);
		loadTypeList();
		btnRemoveItems.setEnabled(false);
		lblActiveList.setForeground(Color.BLUE);
	}// appInit

	// public TypeFileMaintenance() {
	// initialize();
	// }//constructor

	public TypeFileMaintenance(JFrame frame, DeDup deDup) {
		super(frame, "Type File Mainenance", true);
		this.deDup = deDup;
		initialize();
		appInit();
	}// constructor

	private void initialize() {
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		// setModal(true);
		setBounds(100, 100, 321, 595);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		GridBagLayout gbl_contentPanel = new GridBagLayout();
		gbl_contentPanel.columnWidths = new int[] { 120, 0, 0 };
		gbl_contentPanel.rowHeights = new int[] { 0, 0 };
		gbl_contentPanel.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_contentPanel.rowWeights = new double[] { 1.0, Double.MIN_VALUE };
		contentPanel.setLayout(gbl_contentPanel);

		JPanel panelLeft = new JPanel();
		panelLeft.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		GridBagConstraints gbc_panelLeft = new GridBagConstraints();
		gbc_panelLeft.insets = new Insets(0, 0, 0, 5);
		gbc_panelLeft.fill = GridBagConstraints.BOTH;
		gbc_panelLeft.gridx = 0;
		gbc_panelLeft.gridy = 0;
		contentPanel.add(panelLeft, gbc_panelLeft);
		GridBagLayout gbl_panelLeft = new GridBagLayout();
		gbl_panelLeft.columnWidths = new int[] { 0, 0 };
		gbl_panelLeft.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0 };
		gbl_panelLeft.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_panelLeft.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		panelLeft.setLayout(gbl_panelLeft);

		cbTypeFiles = new JComboBox<String>();
		cbTypeFiles.setActionCommand(CB_TYPE_FILE);
		GridBagConstraints gbc_comboBox = new GridBagConstraints();
		gbc_comboBox.insets = new Insets(0, 0, 5, 0);
		gbc_comboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_comboBox.gridx = 0;
		gbc_comboBox.gridy = 0;
		panelLeft.add(cbTypeFiles, gbc_comboBox);

		Component verticalStrut = Box.createVerticalStrut(40);
		GridBagConstraints gbc_verticalStrut = new GridBagConstraints();
		gbc_verticalStrut.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut.gridx = 0;
		gbc_verticalStrut.gridy = 1;
		panelLeft.add(verticalStrut, gbc_verticalStrut);

		btnAddItems = new JButton("Add Item");
		btnAddItems.addActionListener(adapterTypeFileMaintenance);
		btnAddItems.setActionCommand(BTN_ADD_ITEMS);
		GridBagConstraints gbc_btnAddItems = new GridBagConstraints();
		gbc_btnAddItems.insets = new Insets(0, 0, 5, 0);
		gbc_btnAddItems.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnAddItems.gridx = 0;
		gbc_btnAddItems.gridy = 2;
		panelLeft.add(btnAddItems, gbc_btnAddItems);

		txtItem = new JTextField();
		GridBagConstraints gbc_txtItem = new GridBagConstraints();
		gbc_txtItem.insets = new Insets(0, 0, 5, 0);
		gbc_txtItem.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtItem.gridx = 0;
		gbc_txtItem.gridy = 3;
		panelLeft.add(txtItem, gbc_txtItem);
		txtItem.setColumns(10);

		Component verticalStrut_1 = Box.createVerticalStrut(30);
		GridBagConstraints gbc_verticalStrut_1 = new GridBagConstraints();
		gbc_verticalStrut_1.insets = new Insets(0, 0, 5, 0);
		gbc_verticalStrut_1.gridx = 0;
		gbc_verticalStrut_1.gridy = 4;
		panelLeft.add(verticalStrut_1, gbc_verticalStrut_1);

		btnRemoveItems = new JButton("Remove Item(s)");
		btnRemoveItems.addActionListener(adapterTypeFileMaintenance);
		btnRemoveItems.setActionCommand(BTN_REMOVE_ITEMS);
		GridBagConstraints gbc_btnRemoveItems = new GridBagConstraints();
		gbc_btnRemoveItems.fill = GridBagConstraints.HORIZONTAL;
		gbc_btnRemoveItems.gridx = 0;
		gbc_btnRemoveItems.gridy = 5;
		panelLeft.add(btnRemoveItems, gbc_btnRemoveItems);

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_scrollPane = new GridBagConstraints();
		gbc_scrollPane.fill = GridBagConstraints.BOTH;
		gbc_scrollPane.gridx = 1;
		gbc_scrollPane.gridy = 0;
		contentPanel.add(scrollPane, gbc_scrollPane);

		listTypes = new JList<String>();
		listTypes.addListSelectionListener(adapterTypeFileMaintenance);
		listTypes.setName(LIST_TYPES);
		scrollPane.setViewportView(listTypes);

		lblActiveList = new JLabel("<NONE>");
		lblActiveList.setForeground(Color.BLUE);
		lblActiveList.setFont(new Font("Times New Roman", Font.PLAIN, 16));
		lblActiveList.setHorizontalAlignment(SwingConstants.CENTER);
		scrollPane.setColumnHeaderView(lblActiveList);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		JButton btnOK = new JButton("OK");
		btnOK.addActionListener(adapterTypeFileMaintenance);
		btnOK.setActionCommand(BTN_OK);
		buttonPane.add(btnOK);
		getRootPane().setDefaultButton(btnOK);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnLists = new JMenu("Lists");
		menuBar.add(mnLists);

		JMenuItem mnuListsNew = new JMenuItem("New ...");
		mnuListsNew.addActionListener(adapterTypeFileMaintenance);
		mnuListsNew.setActionCommand(MNU_LISTS_NEW);
		mnLists.add(mnuListsNew);

		JMenuItem mnuListsSave = new JMenuItem("Save");
		mnuListsSave.addActionListener(adapterTypeFileMaintenance);
		mnuListsSave.setActionCommand(MNU_LISTS_SAVE);
		mnLists.add(mnuListsSave);

		JMenuItem mnuListsSaveAs = new JMenuItem("Save As ...");
		mnuListsSaveAs.addActionListener(adapterTypeFileMaintenance);
		mnuListsSaveAs.setActionCommand(MNU_LISTS_SAVE_AS);
		mnLists.add(mnuListsSaveAs);

		JSeparator separator = new JSeparator();
		mnLists.add(separator);

		JMenuItem mnuListsDelete = new JMenuItem("Delete");
		mnuListsDelete.addActionListener(adapterTypeFileMaintenance);
		mnuListsDelete.setActionCommand(MNU_LISTS_DELETE);
		mnLists.add(mnuListsDelete);

		JSeparator separator_1 = new JSeparator();
		mnLists.add(separator_1);

		JMenuItem mnuListsExit = new JMenuItem("Exit");
		mnuListsExit.addActionListener(adapterTypeFileMaintenance);
		mnuListsExit.setActionCommand(MNU_LISTS_EXIT);
		mnLists.add(mnuListsExit);

	}// initialize

	// ---------------------------------------------------------

	class AdapterTypeFileMaintenance implements ActionListener, ListSelectionListener, ItemListener {
		/* ActionListener */
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			switch (actionEvent.getActionCommand()) {

			case MNU_LISTS_NEW:
				String listName = getListName("New ListName");
				if (listName != null) {
					doNew(listName.trim());
				} // if
				break;
			case MNU_LISTS_SAVE:
				doSave((String) cbTypeFiles.getSelectedItem());
				break;
			case MNU_LISTS_SAVE_AS:
				listName = getListName("Save List As ");
				if (listName != null) {
					doSave(listName.trim());
				} // if
				break;
			case MNU_LISTS_DELETE:
				doListDelete();
				break;
			case MNU_LISTS_EXIT:
				//
				break;

			case BTN_ADD_ITEMS:
				doAddItem();
				break;
			case BTN_REMOVE_ITEMS:
				if (doRemoveItems() == 0) {
					btnRemoveItems.setEnabled(false);
				} // if
				break;

			case BTN_OK:
				appClose();
				break;
			// case BTN_CANCEL:
			// //
			// break;
			default:
				log.warnf("[AdapterTypeFileMaintenance.actionPerformed] switch at default: Action Command = %s%n",
						actionEvent.getActionCommand());
			}// switch action Command
		}// actionPerformed

		/* ListSelectionListener */
		@Override
		public void itemStateChanged(ItemEvent itemEvent) {
			String actionCommand = ((JComboBox<?>) itemEvent.getSource()).getActionCommand();
			doChangeList(actionCommand, itemEvent.getStateChange());
		}// itemStateChanged

		/* ItemListener */
		@Override
		public void valueChanged(ListSelectionEvent listSelectionEvent) {
			if (listSelectionEvent.getValueIsAdjusting() == false) {
				@SuppressWarnings("unchecked")
				JList<String> list = (JList<String>) listSelectionEvent.getSource();
				switch (list.getName()) {
				case LIST_TYPES:
					btnRemoveItems.setEnabled(!list.isSelectionEmpty());
					break;
				default:
					log.warnf("[AdapterTypeFileMaintenance.valueChanged] switch at default: name = %s%n",
							list.getName());
				}// switch name
			} // if adjusting
		}// valueChanged

	}// class AdapterTypeFileMaintenance

	// ---------------------------------------------------------
	private final JPanel contentPanel = new JPanel();
	private JTextField txtItem;
	private DeDup deDup;
	private JComboBox<String> cbTypeFiles;

	private final static String EMPTY_STRING = "";
	private final static String SPACE = " ";
	// private final static String NAME_SEPARATOR = File.separator;

	private final static String MNU_LISTS_NEW = "mnuListsNew";
	private final static String MNU_LISTS_SAVE = "mnuListsSave";
	private final static String MNU_LISTS_SAVE_AS = "mnuListsSaveAs";
	private final static String MNU_LISTS_DELETE = "mnuListsDelete";
	private final static String MNU_LISTS_EXIT = "mnuListsExit";

	private final static String BTN_ADD_ITEMS = "btnAddItems";
	private final static String BTN_REMOVE_ITEMS = "btnRemoveItems";

	private final static String BTN_OK = "btnOK";
	// private final static String BTN_CANCEL = "btnCancel";

	private final static String CB_TYPE_FILE = "cbTypeFile";
	private final static String LIST_TYPES = "listTypes";

	private JButton btnRemoveItems;
	private JList<String> listTypes;
	private JLabel lblActiveList;
	private JButton btnAddItems;

}// class TypeFileMaintenance
