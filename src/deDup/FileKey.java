package deDup;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileKey {

	private static final String algorithm = "SHA-1"; // 160 bits
	// String algorithm = "MD5"; // 128 bits
	// String algorithm = "SHA-256"; // 256 bits

	public static String getID(String filePath) {

		try {
			FileInputStream inputStream = new FileInputStream(filePath);
			MessageDigest messageDigest = MessageDigest.getInstance(algorithm);
			byte[] bytesBuffer = new byte[BUFF_SIZE];
			int bytesRead = -1;
			while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
				messageDigest.update(bytesBuffer, 0, bytesRead);
			} // while
			inputStream.close();

			byte[] hashedBytes = messageDigest.digest();

			StringBuilder sb = new StringBuilder();
			for (byte b : hashedBytes) {
				sb.append(String.format("%02X", b));
			} // for

			return sb.toString();

		} catch (NoSuchAlgorithmException | IOException nsae) {
			nsae.printStackTrace();
		} // try

		return null;
	}// getID

	private static final int BUFF_SIZE = 1024;

}// class FileKey
