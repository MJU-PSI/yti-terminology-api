package fi.csc.termed.search.service;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class JsonParserService {

	public JSONParser getJsonParser() {
		return jsonParser;
	}

	private JSONParser jsonParser;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public JsonParserService() {
		this.jsonParser = new JSONParser();
	}

	public String getJsonFileAsString(String filename) {
		try {
			return jsonParser.parse(new FileReader(getClass().getClassLoader().getResource(filename).getFile())).toString();
		} catch (IOException e) {
			log.error("Unable to read file: " + filename);
			return null;
		} catch (ParseException e) {
			log.error("Unable to parse file as JSON: " + filename);
			return null;
		}
	}
}
