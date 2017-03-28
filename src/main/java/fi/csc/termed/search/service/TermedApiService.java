package fi.csc.termed.search.service;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
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

	@Value("${search.index.document.importUrl}")
	private String importUrl;

	@Value("${api.eventHook.register.urlContext}")
	private String apiRegisterListenerUrl;

	@Value("${api.eventHook.delete.urlContext}")
	private String apiDeleteListenerUrl;

	private HttpClient apiClient;

	private JsonParserService jsonParserService;

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Autowired
	public TermedApiService(JsonParserService jsonParserService) {
		this.jsonParserService = jsonParserService;
		this.apiClient = getApiHttpClient();
	}

	private String getAuthHeader() {
		return "Basic " + new String(Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes()));
	}

	public boolean deleteChangeListener(String hookId) {
		HttpDelete deleteReq = new HttpDelete(API_HOST_URL + apiDeleteListenerUrl + hookId);
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
		HttpPost registerReq = new HttpPost(API_HOST_URL + apiRegisterListenerUrl);
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
				return result.toString();
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

	public Map<String, String> fetchConceptDocuments() {
		Map<String, String> indexDocs = new HashMap<>();
		HttpGet getConceptsReq = new HttpGet(API_HOST_URL + importUrl);
		try {
			String encAuth = Base64.getEncoder().encodeToString((API_USER + ":" + API_PW).getBytes());
			String authHeader = "Basic " + new String(encAuth);
			getConceptsReq.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
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
				log.info("Fetched " + fetched + " document from termed API");
			} else {
				log.warn("Fetching documents for index failed with code: " + resp.getStatusLine().getStatusCode());
				return null;
			}
		} catch (IOException e) {
			log.error("Batch document import failed");
			e.printStackTrace();
			return null;
		} catch (ParseException e) {
			log.error("Batch document import failed");
			e.printStackTrace();
			return null;
		} finally {
			getConceptsReq.releaseConnection();
		}
		return indexDocs;
	}

	private HttpClient getApiHttpClient() {
		HttpClientBuilder b = HttpClientBuilder.create();

		// setup a Trust Strategy that allows all certificates.
		//
		SSLContext sslContext = null;
		try {
			sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
				public boolean isTrusted(X509Certificate[] arg0, String arg1) {
					return true;
				}
			}).build();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		b.setSSLContext(sslContext);

		// don't check Hostnames, either.
		//      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
		HostnameVerifier hostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier();

		// here's the special part:
		//      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
		//      -- and create a Registry, to register it.
		//
		SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslSocketFactory)
				.build();

		// now, we create connection-manager using our Registry.
		//      -- allows multi-threaded use
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
		b.setConnectionManager( connMgr);

		// finally, build the HttpClient;
		//      -- done!

		return b.build();
	}
}
