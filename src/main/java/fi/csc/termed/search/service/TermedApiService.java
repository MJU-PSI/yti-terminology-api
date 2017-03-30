package fi.csc.termed.search.service;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by jmlehtin on 28/3/2017.
 */

@Service
public class TermedApiService {

	@Value("${api.user}")
	private String API_USER;

	@Value("${api.pw}")
	private String API_PW;

	@Value("${api.host.url}")
	private String API_HOST_URL;

	@Value("${api.node.get.all.urlContext}")
	private String GET_ALL_NODES_URL_CONTEXT;

	@Value("${api.node.get.one.urlContext}")
	private String GET_ONE_NODE_URL_CONTEXT;

	@Value("${api.eventHook.register.urlContext}")
	private String API_REGISTER_LISTENER_URL_CONTEXT;

	@Value("${api.eventHook.delete.urlContext}")
	private String API_DELETE_LISTENER_URL_CONTEXT;

	private HttpClient apiClient;

	private JsonParserService jsonParserService;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public TermedApiService(JsonParserService jsonParserService) {
		this.jsonParserService = jsonParserService;
		this.apiClient = HttpClientBuilder.create().build();
	}

	private String getAuthHeader() {
		return "Basic " + new String(Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes()));
	}

	public boolean deleteChangeListener(String hookId) {
		HttpDelete deleteReq = new HttpDelete(API_HOST_URL + MessageFormat.format(API_DELETE_LISTENER_URL_CONTEXT, hookId));
		deleteReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try {
			HttpResponse resp = apiClient.execute(deleteReq);
			if(resp.getStatusLine().getStatusCode() < 200 || resp.getStatusLine().getStatusCode() >= 400) {
				log.error("Response code: " + resp.getStatusLine().getStatusCode());
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			deleteReq.releaseConnection();
		}
		return true;
	}

	public String registerChangeListener() {
		HttpPost registerReq = new HttpPost(API_HOST_URL + API_REGISTER_LISTENER_URL_CONTEXT);
		registerReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
		try {
			HttpResponse resp = apiClient.execute(registerReq);
			if(resp.getStatusLine().getStatusCode() >= 200 && resp.getStatusLine().getStatusCode() < 400) {
				BufferedReader rd = new BufferedReader(
						new InputStreamReader(resp.getEntity().getContent()));

				StringBuffer result = new StringBuffer();
				String line = "";
				while ((line = rd.readLine()) != null) {
					result.append(line);
				}
				return result.toString().replace("<string>", "").replace("</string>", "");
			} else {
				log.error("Response code: " + resp.getStatusLine().getStatusCode());
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			registerReq.releaseConnection();
		}
	}

	public Map<String, String> fetchAllNodes() {
		Map<String, String> indexDocs = new HashMap<>();
		HttpGet getConceptsReq = new HttpGet(API_HOST_URL + GET_ALL_NODES_URL_CONTEXT);
		try {
			getConceptsReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
			HttpResponse resp = apiClient.execute(getConceptsReq);
			if (resp.getStatusLine().getStatusCode() == 200) {
				String respStr = EntityUtils.toString(resp.getEntity());
				JSONArray docs = (JSONArray) jsonParserService.getJsonParser().parse(respStr);

				Iterator it = docs.iterator();
				int fetched = 0;
				while (it.hasNext()) {
					JSONObject doc = (JSONObject) it.next();
					if(doc != null && doc.get("id") != null) {
						indexDocs.put(String.valueOf(doc.get("id")), doc.toJSONString());
						fetched++;
					}
				}
				log.info("Fetched " + fetched + " nodes from termed API");
			} else {
				log.warn("Fetching nodes failed with code: " + resp.getStatusLine().getStatusCode());
				return null;
			}
		} catch (IOException e) {
			log.error("Fetching nodes failed");
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			log.error("Fetching nodes failed");
			e.printStackTrace();
			return null;
		} finally {
			getConceptsReq.releaseConnection();
		}
		return indexDocs;
	}

	public String fetchNode(String graphId, String nodeId) {
		if(graphId != null && nodeId != null) {
			HttpGet getConceptReq = new HttpGet(API_HOST_URL + MessageFormat.format(GET_ONE_NODE_URL_CONTEXT, graphId, nodeId));
			try {
				getConceptReq.setHeader(HttpHeaders.AUTHORIZATION, getAuthHeader());
				HttpResponse resp = apiClient.execute(getConceptReq);
				if (resp.getStatusLine().getStatusCode() == 200) {
					return EntityUtils.toString(resp.getEntity());
				} else {
					log.warn("Fetching node failed with code: " + resp.getStatusLine().getStatusCode());
					log.warn("URL: " + getConceptReq.getURI().toString());
					return null;
				}
			} catch (IOException e) {
				log.error("Fetching node failed");
				e.printStackTrace();
				return null;
			} finally {
				getConceptReq.releaseConnection();
			}
		}
		log.error("GraphId or NodeId not supplied for fetching node data from termed API");
		return null;
	}

}
