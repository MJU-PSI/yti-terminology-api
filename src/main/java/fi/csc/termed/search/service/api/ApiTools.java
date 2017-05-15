package fi.csc.termed.search.service.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public abstract class ApiTools {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	private final HttpClient apiClient;
	private final JsonParser gsonParser;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	public ApiTools() {
		this.apiClient = HttpClientBuilder.create().build();
		this.gsonParser = new JsonParser();
	}

	String getAuthHeader() {
		return "Basic " + Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes());
	}

	List<JsonObject> fetchJsonObjectsInArrayFromUrl(String url) {
		List<JsonObject> allObjects = new ArrayList<>();
		HttpGet getObjectsReq = new HttpGet(url);
		try {
			getObjectsReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
			HttpResponse resp = apiClient.execute(getObjectsReq);
			if (resp.getStatusLine().getStatusCode() == 200) {
				JsonArray docs = gsonParser.parse(EntityUtils.toString(resp.getEntity())).getAsJsonArray();
				Iterator<JsonElement> it = docs.iterator();
				int fetched = 0;
				while (it.hasNext()) {
					JsonElement docElem = it.next();
					if(docElem.isJsonObject()) {
						allObjects.add(docElem.getAsJsonObject());
						fetched++;
					}
				}
				log.info("Fetched " + fetched + " objects from termed API from url " + url);
			} else {
				log.warn("Fetching objects failed with code: " + resp.getStatusLine().getStatusCode());
				return null;
			}
		} catch (IOException e) {
			log.error("Fetching objects failed");
			e.printStackTrace();
			return null;
		} finally {
			getObjectsReq.releaseConnection();
		}
		return allObjects;
	}

	JsonObject fetchJsonObjectFromUrl(String url) {
		if(url != null) {
			HttpGet getRequest = new HttpGet(url);
			try {
				getRequest.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
				HttpResponse resp = apiClient.execute(getRequest);
				if (resp.getStatusLine().getStatusCode() == 200) {
					String respStr = EntityUtils.toString(resp.getEntity());
					JsonObject obj = gsonParser.parse(respStr).getAsJsonObject();
					if(obj != null) {
						return obj;
					} else {
						log.error("Unable to parse response JSON from " + url);
						return null;
					}
				} else {
					log.warn("Fetching JSON from " + url + " failed with code: " + resp.getStatusLine().getStatusCode());
					return null;
				}
			} catch (IOException e) {
				log.error("Fetching JSON failed");
				e.printStackTrace();
				return null;
			} finally {
				getRequest.releaseConnection();
			}
		}
		return null;
	}

	public enum VocabularyType {
		TerminologicalVocabulary,
		Vocabulary
	}

}
