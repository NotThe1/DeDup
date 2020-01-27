
import java.nio.file.Paths;

/**
 * modified getColumnCount to return 1 less than actual, to hide last field in
 * the tables
 * 
 *
 */
public class MainTableModel extends MyTableModel {
	private static final long serialVersionUID = 1L;

	public MainTableModel() {
		super(new String[] { ACTION, NAME, DIRECTORY, SIZE, DATE, DUP, ID, HASH_KEY });
	}// Constructor

	public Long getStorageSum() {
		Long ans = 0l;
		for (int i = 0; i < this.getRowCount(); i++) {
			ans = ans + (Long) getValueAt(i, 2);
		} // for
		return ans;
	}// sun

	@Override
	public int getColumnCount() { // want to ignore the HASH column
		return columnCount - 1;
	}// getColumnCount

	public FileProfile getFileProfile(int rowNumber) {
		String filePath = Paths.get((String) getValueAt(rowNumber, 1), (String) getValueAt(rowNumber, 0)).toString();

		FileProfile fileStat = new FileProfile(filePath, (long) getValueAt(rowNumber, 2),
				(String) getValueAt(rowNumber, 3), (String) getValueAt(rowNumber, 6));
		return fileStat;
	}// getFileStat

	public void addRow(FileProfile subject) {
		addRow(subject, -1);
	}// addRow

	public void addRow(FileProfile subject, Integer fileID) {
		/* @formatter:off */
		Object[] rowData = new Object[] { false, subject.getFileName(), subject.getDirectory(), subject.getFileSize(),
				subject.getFileTime().toString(), false, fileID, subject.getHashKey() };
		addRow(rowData);
	}// addRow
	/* @formatter:on */

	public boolean isCellEditable(int row, int column) {
		return column == 0;
	}// isCellEditable

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex > this.getColumnCount() - 1) {
			String msg = String.format("[getColumnClass] Invalid column. columnIndex = %d, max Column = %d",
					columnIndex, this.getColumnCount());
			throw new IllegalArgumentException(msg);

		} // if
		switch (columnIndex) {
		case 0:// Action
			return Boolean.class;
		case 1: // Name
			return String.class;
		case 2: // Directory
			return String.class;
		case 3: // Size
			return Long.class;
		case 4: // Modified Date
			return String.class;
		case 5: // Duplicate
			return Boolean.class;
		case 6:// File ID
			return Integer.class;
		case 7: // Hash Key
			return String.class;
		default:
			return super.getColumnClass(columnIndex);
		}// switch
	}// getColumnClass

	public static final String ACTION = "Action";
	public static final String NAME = "Name";
	public static final String DIRECTORY = "Directory";
	public static final String SIZE = "Size";
	public static final String DATE = "ModifiedDate";
	public static final String DUP = "Dup";
	public static final String ID = "ID";
	public static final String HASH_KEY = "HashKey";

}// class SubjectTableModel
