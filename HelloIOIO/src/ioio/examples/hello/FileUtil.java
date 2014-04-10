package ioio.examples.hello;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Environment;

public class FileUtil {
	private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

	public static final String ROBOTAR_FOLDER = "rtsongs/";
	
	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	/* Checks if external storage is available to at least read */
	public static boolean isExternalStorageReadable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)
				|| Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			return true;
		}
		return false;
	}

	public static File getRobotarStorageDir(String robotarFolder) {
		// Get the directory for the user's public documents directory.
		// this required api level 19, therefore we put our robotarfolder only
		// at the root
		// Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS
		File file = new File(Environment.getExternalStorageDirectory(),
				robotarFolder);
		if (!file.mkdirs()) {
			LOG.error("Directory not created");
		}
		return file;
	}
}
