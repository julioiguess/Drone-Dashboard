// Edited by Samuel Culp

/*This service file is for handling reading and writing java objects to json files. 
	1.	JsonDataStore Constructor creates ObjectMapper then registers
		JavaTimeModule to support LocalDatetime. 
	2.	LoadList: creates a file in the data directory
			-Checks if the file exists
			-if not found returns empty arrList
			-creates JavaType as list
			-converts JSON to list
			-returns
	3.  saveList: creates a file in the data directory
			-retrieves the parent directory
			-creates the directory if it don’t exist
			-converts the list to JSON*/


package services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonDataStore {
	
	private final ObjectMapper mapper; // converts json to java objects and also back to json
	
	public JsonDataStore() {
		mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule()); //JavaTimeModule is for LocalDateTime
	}
	
	//Loads a list of objects from a JSON file
	
	public <T> List<T> loadList( String filename, Class<T> modelClass)
					throws IOException {
		
		File file = new File ("data", filename); // looks for data file
		
		if (!file.exists()) {
			return new ArrayList<>();
		}
		
		JavaType listType = mapper.getTypeFactory().constructCollectionType(
				List.class, modelClass);
		
		return mapper.readValue(file,  listType);
	}
	
	// save list of objects to a json file
	
	public <T> void saveList( String filename, List<T> items)
					throws IOException {
		
		File file = new File("data", filename); // Looks for data folder
		
		File parent = file.getParentFile();
		
		if (parent != null) {
			parent.mkdirs();
		}
		
		mapper.writerWithDefaultPrettyPrinter().writeValue(file,  items);
	}
	
	
}
