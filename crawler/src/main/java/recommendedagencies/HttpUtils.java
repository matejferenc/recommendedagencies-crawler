package recommendedagencies;

import org.apache.http.HttpEntity;
import org.apache.http.HttpMessage;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class HttpUtils {

    private static Logger logger = LogManager.getLogger(HttpUtils.class);

    public static String get(String url) {
        String toReturn = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            response = httpClient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                toReturn = EntityUtils.toString(entity, "UTF8");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while getting URL: " + url, e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException("Error while closing response or httpClient for URL: " + url, e);
            }
        }

        return toReturn;
    }

    public static String postUrlEncodedData(String url, List<? extends NameValuePair> data) {
        String toReturn = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
            response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                toReturn = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            logger.error("error", e);
        } finally {
            try {
                response.close();
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return toReturn;
    }

    public static String postJson(String url, String json, Map<String, String> headers) {
        String toReturn = null;
        CloseableHttpClient httpclient = HttpClients.createDefault();
        CloseableHttpResponse response = null;
        try {
            HttpPost httpPost = new HttpPost(url);
            setHeaders(httpPost, headers);
            httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            response = httpclient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                toReturn = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            logger.error("error", e);
        } finally {
            try {
                response.close();
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return toReturn;
    }

    private static void setHeaders(HttpMessage httpMessage, Map<String, String> headers) {
        for (String header : headers.keySet()) {
            httpMessage.setHeader(header, headers.get(header));
        }
    }

}