import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * main class to go thru each folder to:
 *  1) Make a catalog file if needed the catalog file contains
 *     the FileProfile
 */
public class CensusTaker extends RecursiveAction {
	private static final long serialVersionUID = 1L;
	private AppLogger log = AppLogger.getInstance();
	private Pattern patternFileType = Pattern.compile("\\.([^.]+$)");
	private File folder;
	private String listName;
	private Pattern patternTargets;
	private List<Path> skipListModel;
	// private DefaultListModel<File> skipListModel;

	private File catalogFile;
//	private FileProfile fileProfile;
	private static Format myFormat = new SimpleDateFormat("yyyy-MM-dd  HH:mm:ss");
	private HashMap<String, FileProfile> catalogOriginal;
	private HashMap<String, FileProfile> catalogCurrent;
	// private HashMap<String, String> catalogOriginal;
	// private HashMap<String, String> catalogCurrent;

	public CensusTaker(File folder, String listName, Pattern patternTargets, List<Path> skipListModel) {
		this.folder = folder;
		this.listName = listName;
		this.patternTargets = patternTargets;
		this.skipListModel = skipListModel;
		classInit();
	}// Constructor

	private void classInit() {
		this.catalogFile = new File(folder, ".deDup." + listName);
		this.catalogOriginal = getCatalog(catalogFile, listName);
		this.catalogCurrent = new HashMap<String, FileProfile>();

	}// classInit

	@Override
	protected void compute() {
		if (skipListModel.contains(folder.toPath())) {
			log.infof("[CensusTaker.compute] skipping: %s%n", folder);
			return;
		} // if we need to skip

		/* Find all the sub directories in this directory			*/
		File[] directories = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File fileContent) {
				return fileContent.isDirectory();
			}// accept
		});
		
		/* process any & all sub directories	*/
		if (directories != null) {
			for (File directory : directories) {
				CensusTaker censusTaker = new CensusTaker(directory, listName, patternTargets, skipListModel);
				censusTaker.fork();
			} // for each directory
		} // if directories

		/* Find all the files in this directory			*/
		File[] files = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File fileContent) {
				return fileContent.isFile();
			}// accept
		});

		/* Process any & all files in this directory */
		if (files != null) {
			for (File file : files) {
				if (isTarget(file)) {
					processTargetFile(file);
				} // if target
			} // for each directory

			if (catalogCurrent.isEmpty()) {
				try {
					Files.deleteIfExists(catalogFile.toPath());
				} catch (IOException e) {
					log.warnf("Did not delete : %s%n", catalogFile.toPath());
					e.printStackTrace();
				} // try
			} else {
				saveCatalog(catalogFile, listName, catalogCurrent);
			} // if empty catalog
		} // if files not null
	}// compute

	private boolean isTarget(File file) {
		String fileName = file.getName();
		Matcher matcher = patternFileType.matcher(fileName);
		String fileType = matcher.find() ? matcher.group(1).toUpperCase().trim() : NONE;
		// String fileType = matcher.find()? matcher.group(1).toLowerCase():NONE;
		matcher = patternTargets.matcher(fileType);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		} // if
	}// isTarget

	private void processTargetFile(File file) {
		String fileName = file.getName();
		String hash;
		if (catalogOriginal.containsKey(fileName)) {
			catalogCurrent.put(fileName, catalogOriginal.get(fileName));
		} else {
			String filePath = file.getAbsolutePath();
			long fileSize = file.length();
			String lastModified = myFormat.format(file.lastModified());

			hash = FileKey.getID(file.getAbsolutePath());
			if (hash == null) {
				log.errorf("Could not generate hash for %s%n", file.getAbsolutePath());
			} // if hash is null
			catalogCurrent.put(fileName, new FileProfile(filePath, fileSize, lastModified, hash));
		} // if
	}// processTargetFile

	/* Catalog File Object Serialization */

	@SuppressWarnings("unchecked")
	public HashMap<String, FileProfile> getCatalog(File catalogFile, String listName) {
		HashMap<String, FileProfile> ans = new HashMap<String, FileProfile>();
		try {
			FileInputStream fis = new FileInputStream(catalogFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			ans = (HashMap<String, FileProfile>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			// log.infof("Could not get catalog for : " +
			// catalogFile.getParentFile().toString());
		} // try
		return ans;
	}// getCatalog

	public void saveCatalog(File catalogFile, String listName, HashMap<String, FileProfile> catalog) {
		try {
			FileOutputStream fos = new FileOutputStream(catalogFile);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(catalog);
			fos.close();
			oos.close();
		} catch (Exception e) {
			log.infof("Could not write catalog for : " + catalogFile.getParentFile().toString());
		} // try
		return;
	}// getCatalog

	private final static String NONE = "<none>";
}// class CensusTaker
