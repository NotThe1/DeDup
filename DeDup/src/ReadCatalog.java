

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Set;

public class ReadCatalog {
	

	public static void main(String[] args) {
		new ReadCatalog().doIt();

	}//main
	
	public void doIt() {
//		Path path = Paths.get("C:\\Temp\\DeDupTest");
		Path path = Paths.get("E:\\Meta_Pictures\\2018 Europe");
		try {
			Files.walkFileTree(path, new MyWalker());
		} catch (IOException ioe) {
			System.out.printf("[TreeWalkTest.doIt] %s%n", ioe.getMessage());
		} // try

	}//doIt 
	
	
	@SuppressWarnings("unchecked")
	public HashMap<String, FileProfile> getCatalog(File catalogFile, String listName) {
		HashMap<String, FileProfile> ans = new HashMap<String, FileProfile>();
		try {
			FileInputStream fis = new FileInputStream(catalogFile);
			ObjectInputStream ois = new ObjectInputStream(fis);
			ans = (HashMap<String, FileProfile>) ois.readObject();
			ois.close();
			fis.close();
		} catch (ClassNotFoundException cnfe) {
			System.err.printf("Could not get Catalog class for : " + catalogFile.getParentFile().toString());
		} catch (FileNotFoundException fnfe) {
			System.err.printf("Could not get catalog for : " + catalogFile.getParentFile().toString());
		} catch (IOException ioe) {
			System.err.printf("IOException reading %s%n%s%n", catalogFile.getParentFile().toString(), ioe.getMessage());
		} // try
		return ans;
	}// getCatalog

	class MyWalker implements FileVisitor<Path> {
		
		@Override
		public FileVisitResult postVisitDirectory(Path arg0, IOException arg1) throws IOException {
			// TODO Auto-generated method stub
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path arg0, BasicFileAttributes arg1) throws IOException {
			File catalogFile = new File(arg0.toString(),".deDup.Pictures");
			HashMap<String, FileProfile> entries =  getCatalog(catalogFile, "listName");
			System.out.printf("%n%n     %s%n",arg0.toString() + "\\.deDup.Pictures");
			Set<String> keys = entries.keySet();
			for (String key:keys) {
				FileProfile fp = entries.get(key);
			String msg = String.format("%s | %s | %,d \t %s%n", fp.getHashKey(),fp.getFileTime(),fp.getFileSize(),fp.getFilePathString());
				System.out.print(msg);
			}//for
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path arg0, BasicFileAttributes arg1) throws IOException {
			// TODO Auto-generated method stub
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFileFailed(Path arg0, IOException arg1) throws IOException {
			// TODO Auto-generated method stub
			return FileVisitResult.CONTINUE;
		}
		
	}
}//class ReadCatalog
