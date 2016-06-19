package aditi.geography;

//import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by administrator on 6/14/16.
 */
public class SSLUtils {

    private static String TAG = "SSLUtils";
    public static HttpsURLConnection conn = null;

    private static TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
    };

    public static InputStream invokeHttpsApi(String urlString) throws Exception {

        HttpsURLConnection conn = null;
        InputStream inputStream = null;

        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
        tmf.init(trustStore);

        SSLContext context = SSLContext.getInstance("SSL");
        context.init(null, trustAllCerts, null);
        URL url = new URL(urlString);
        conn = (HttpsURLConnection) url.openConnection();
        conn.setSSLSocketFactory(context.getSocketFactory());
        //Log.d(TAG, "Result from https connection is " + conn.getResponseMessage());
        inputStream = conn.getInputStream();
        return inputStream;

    }

    public static void parseJsonCountriesOutput(InputStream inputStream,
                                                Map<String, String> countryCapitalMap,
                                                Map<String, String> currencyMap)
            throws Exception {


        //Convert stream to string
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line + "\n");
        }

        JSONArray jsonarray = new JSONArray(sb.toString());
        String countryName;
        String currency = "";
        JSONArray subArray;

        String val = "";

        for (int i = 0; i < jsonarray.length(); i++) {
            JSONObject jsonobject = jsonarray.getJSONObject(i);
            countryName = jsonobject.getString("name");

            subArray = jsonobject.getJSONArray("currencies");
            if (subArray.length() > 0) {
                currency = subArray.getString(0);
            }
            if (currencyMap.get(currency) != null) {
                currency = currencyMap.get(currency);
                val = String.format(
                        "The capital of %s is %s . Currency used is %s",
                        countryName, jsonobject.getString("capital"), currency);
            } else {
                val = String.format(
                        "The capital of %s is %s ",
                        countryName, jsonobject.getString("capital"));
            }
            countryCapitalMap.put(countryName, val);
        }
    }
}
