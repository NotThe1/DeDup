

import java.io.Serializable;

public class FileProfile implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static String fileSeparator = System.getProperties().getProperty("file.separator");

	private String filePath;
	private long fileSize;
	private String fileTime;
	private String hashKey;

//	public FileStat(String filePath, long fileSize, String fileTime) {
//		this.filePath = filePath;
//		this.fileSize = fileSize;
//		this.fileTime = fileTime;
//	}// Constructor

	public FileProfile(String filePath, long fileSize, String fileTime, String hashKey) {
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.fileTime = fileTime;
		this.hashKey = hashKey;
	}// Constructor

	public String getFilePath() {
		return filePath;
	}// getFilePath

	public String getFilePathString() {
		return filePath.toString();
	}// getFilePathString

	public long getFileSize() {
		return fileSize;
	}// getFileSize

	public String getFileTime() {
		return fileTime;
	}// getFileTime

	public String getHashKey() {
		return hashKey;
	}// getHashKey

	public void setHashKey(String hashKey) {
		this.hashKey = hashKey;
	}// setHashKey

	public String getFileName() {
		int loc = this.filePath.lastIndexOf(fileSeparator);
		return this.filePath.substring(loc + 1);
	}// getFileName

	public String getDirectory() {
		int loc = this.filePath.lastIndexOf(fileSeparator);
		return this.filePath.substring(0, loc);
	}// getDirectory

//	public static final String NOT_ON_LIST = "Not on list";

}// class FileStat
