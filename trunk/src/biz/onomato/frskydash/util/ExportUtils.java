package biz.onomato.frskydash.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;

import biz.onomato.frskydash.FrSkyServer;
import biz.onomato.frskydash.domain.Model;
import biz.onomato.frskydash.util.FileUtils.ExternalStorageState;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * utility for import/exporting configuration
 * 
 */
public class ExportUtils {

	/**
	 * export the given set of Model objects (with their channels etc)
	 * 
	 * @param model
	 * @param file
	 */
	public static void exportModelsToFile(Collection<Model> models, File file)
			throws Exception {
		// (Serialization)
		// check availability of sdcard
		if (FileUtils.getExternalStorageState() != ExternalStorageState.WRITABLE)
			throw new Exception("SDCARD not writable");
		// at this point the sdcard is available and writable
		// check if root exists, if not we can start making directories
		if (file.getParentFile() != null && !file.getParentFile().exists())
			file.getParentFile().mkdirs();
		// now for each type create a new file with all items on a separate line
		// init gson
		// Gson's @Expose
		// This feature provides a way where you can mark certain fields of your
		// objects to be excluded for consideration for serialization and
		// deserialization to JSON. To use this annotation, you must create Gson
		// by using new
		// GsonBuilder().excludeFieldsWithoutExposeAnnotation().create(). The
		// Gson instance created will exclude all fields in a class that are not
		// marked with @Expose annotation.
		Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
		// open the file to write in
		FileWriter writer = new FileWriter(file);
		// now iterate models and write model line per line
		for (Model model : models)
			// export models
			writer.write(gson.toJson(model)
					+ System.getProperty("line.separator"));
		// close file again //FIXME this should be done in final
		writer.close();
	}

	/**
	 * import models from file
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static void importModelsFromFile(File file) throws Exception {
		// check availability of sdcard
		if (FileUtils.getExternalStorageState() == ExternalStorageState.UNAVAILABLE)
			throw new Exception("SDCARD not available");
		// init json
		Gson gson = new Gson();
		// no need to continue if file doesn't exist
		if (!file.exists())
			throw new Exception("No export files found on SDCARD");
		// now per type
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = reader.readLine()) != null) {
			// (Deserialization)
			Model model = gson.fromJson(line, Model.class);
			// actually do something with this model (insert in DB?)
			FrSkyServer.saveModel(model);
		}
		// clean up FIXME this should be done in final
		reader.close();
	}

}
