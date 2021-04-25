package com.qtgl.iga.utils;

import org.apache.oltu.oauth2.client.HttpClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthClientResponse;
import org.apache.oltu.oauth2.client.response.OAuthClientResponseFactory;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;

import javax.net.ssl.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;


public class SSLConnectionClient implements HttpClient {

    public SSLConnectionClient() {
    }

    public <T extends OAuthClientResponse> T execute(
            OAuthClientRequest request, Map<String, String> headers,
            String requestMethod, Class<T> responseClass)
            throws OAuthSystemException, OAuthProblemException {

        String responseBody = null;
        URLConnection c = null;
        int responseCode = 0;
        try {
            URL url = new URL(request.getLocationUri());

            c = url.openConnection();
            responseCode = -1;

            HttpURLConnection connection = (HttpURLConnection) c;

            if (c instanceof HttpsURLConnection) {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new SecureRandom());
                ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                ((HttpsURLConnection) connection).setHostnameVerifier(new TrustAnyHostnameVerifier());
            } else {
                if (request.getHeaders() != null) {
                    for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                        connection.addRequestProperty(header.getKey(), header.getValue());
                    }
                }
            }

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.addRequestProperty(header.getKey(), header.getValue());
                }
            }

            if (!OAuthUtils.isEmpty(requestMethod)) {
                connection.setRequestMethod(requestMethod);
                if (requestMethod.equals(OAuth.HttpMethod.POST)) {
                    connection.setDoOutput(true);
                    OutputStream ost = connection.getOutputStream();
                    PrintWriter pw = new PrintWriter(ost);
                    pw.print(request.getBody());
                    pw.flush();
                    pw.close();
                }
            } else {
                connection.setRequestMethod(OAuth.HttpMethod.GET);
            }

            connection.connect();

            InputStream inputStream;
            responseCode = connection.getResponseCode();
            if (responseCode == 400) {
                inputStream = connection.getErrorStream();
            } else {
                inputStream = connection.getInputStream();
            }

            responseBody = OAuthUtils.saveStreamAsString(inputStream);
        } catch (Exception e) {
            throw new OAuthSystemException(e);
        }

        return OAuthClientResponseFactory.createCustomResponse(responseBody,
                c.getContentType(), responseCode, responseClass);
    }

    public void shutdown() {
        // Nothing to do here
    }

    static class TrustAnyTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {

        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[]{};
        }
    }

    static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}
