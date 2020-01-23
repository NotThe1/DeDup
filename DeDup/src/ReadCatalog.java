

import java.io.File;
import java.io.FileInputStream;
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
	
	FileProfile fileProfile;

	public static void main(String[] args) {
		new ReadCatalog().doIt();

	}//main
	
	public void doIt() {
		Path path = Paths.get("C:\\Temp\\DeDupTest");
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
		} catch (Exception e) {
			System.err.printf("Could not get catalog for : " + catalogFile.getParentFile().toString());
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
