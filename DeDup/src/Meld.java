import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicInteger;

public class Meld extends RecursiveAction {
	private static final long serialVersionUID = 1L;

	private AppLogger log = AppLogger.getInstance();

	private File folder;
	private String listName;
	private TargetTableModel targetTableModel;
	private static final AtomicInteger fileID = new AtomicInteger(0);
	private ConcurrentHashMap<String, Integer> hashIDs;
	private ConcurrentHashMap<String, Integer> hashCounts;
	private List<Path> skipListModel;
	private static final Object idLock = new Object();

	public Meld(File folder, String listName, TargetTableModel targetTableModel,
			ConcurrentHashMap<String, Integer> hashCounts, ConcurrentHashMap<String, Integer> hashIDs,
			List<Path> skipListModel) {
		this.folder = folder;
		this.listName = listName;
		this.targetTableModel = targetTableModel;
		this.hashCounts = hashCounts;
		this.hashIDs = hashIDs;
		this.skipListModel = skipListModel;

	}// Constructor

	@SuppressWarnings("unchecked")
	@Override
	protected void compute() {
		if (skipListModel.contains(folder.toPath())) {
			log.infof("[Meld.compute] skipping: %s%n", folder);
			return;
		} // if we need to skip

		/* Find all the sub directories in this directory */
		File[] directories = folder.listFiles(new FileFilter() {
			@Override
			public boolean accept(File fileContent) {
				return fileContent.isDirectory();
			}// accept
		});

		/* process any & all sub directories */
		if (directories != null) {
			for (File directory : directories) {
				new Meld(folder, listName, targetTableModel, hashCounts, hashIDs, skipListModel).fork();
			} // for each directory
		} // if directories

		/* Find the catalog in this directory */

		File catalogFile = new File(folder, ".deDup." + listName);
		HashMap<String, FileProfile> catalog = new HashMap<String, FileProfile>();
		try {
			FileInputStream fis = new FileInputStream(catalogFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			catalog = (HashMap<String, FileProfile>) ois.readObject();
			ois.close();
			fis.close();
		} catch (Exception e) {
			// log.infof("Could not get catalog for : " +
			// catalogFile.getParentFile().toString());
		} // try

		if (catalog.isEmpty()) {
			return;
		} // if

		// Set<String> keys = profiles.keySet();
		String hashKey;
		long size = -1;
		int ID = -1;
		Collection<FileProfile> profiles = catalog.values();
		for (FileProfile profile : profiles) {
			hashKey = profile.getHashKey();
			synchronized (idLock) {
				if (!hashCounts.contains(hashKey)) {
					hashCounts.put(hashKey, 0);
					hashIDs.put(hashKey, fileID.getAndIncrement());
				} // if new hashKey
				
				hashCounts.put(hashKey, hashCounts.get(hashKey)+ 1);
				targetTableModel.addRow(profile, hashIDs.get(hashKey));
			} // synchronized (idLock)

		} // for each

	}// compute

}// class Meld
