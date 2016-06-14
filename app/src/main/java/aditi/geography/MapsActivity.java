package aditi.geography;

import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    TextToSpeech tts;
    Map<String, String> hashMap = new HashMap<>();
    private String selectedState;
    Properties properties = new Properties();


    private static String TAG = "MapsActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        initMap();

        tts = new TextToSpeech(MapsActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.ERROR) {
                    Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });
    }

    @Override
    protected void onPause() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onPause();

    }

    @Override
    protected void onStop() {

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        super.onStop();

    }

    private void initMap() {
        String propFileUSA = "states.properties";
        InputStream is = getClass().getClassLoader().getResourceAsStream(propFileUSA);
        try {
            if (is != null) {
                properties.load(is);
            }
        } catch (Exception e) {
            Log.e(TAG, "Could not load prop file");
        }
        new LongRunningGetIO().execute();
    }

    private void background() {
        String urlString = "https://restcountries.eu/rest/v1/all";
        BufferedReader br = null;
        HttpsURLConnection con = null;
        try {
            Log.d(TAG, "Connecting to the rest endpoint");

            TrustManager[] trustAllCerts = new TrustManager[]{
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

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            String algorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(algorithm);
            tmf.init(trustStore);

            SSLContext context = SSLContext.getInstance("SSL");
            context.init(null, trustAllCerts, null);
            URL url = new URL(urlString);
            con =
                    (HttpsURLConnection) url.openConnection();
            con.setSSLSocketFactory(context.getSocketFactory());
            InputStream in = con.getInputStream();

            //Read json result
            br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }


            Log.d(TAG, "response code " + con.getResponseCode());

            JSONArray jsonarray = new JSONArray(sb.toString());
            String countryName;

            for (int i = 0; i < jsonarray.length(); i++) {
                JSONObject jsonobject = jsonarray.getJSONObject(i);

                countryName = jsonobject.getString("name");
                hashMap.put(countryName, String.format("The capital of %s is %s",
                        countryName, jsonobject.getString("capital")));
            }


        } catch (Exception e) {
            Log.e(TAG, "failed to connect to REST " + e.toString());

        } finally {
            if (br != null) {
                try {
                    br.close();
                    con.disconnect();
                } catch (Exception e) {
                    Log.e(TAG, "Unable to close stream", e);
                }
            }
        }


    }


    /*
    Convert text to speech
     */
    private void ConvertTextToSpeech() {
        if (properties.get(selectedState) != null) {
            tts.speak(String.format("This is %s.%s", selectedState, properties.get(selectedState)),
                    TextToSpeech.QUEUE_FLUSH, null);

        } else if (hashMap.get(selectedState) != null) {
            tts.speak(hashMap.get(selectedState), TextToSpeech.QUEUE_FLUSH, null);
        } else {
            tts.speak(selectedState, TextToSpeech.QUEUE_FLUSH, null);
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        googleMap.setOnMapClickListener(new OnMapClickListener() {

            @Override
            public void onMapClick(LatLng arg0) {


                // TODO Auto-generated method stub
                try {
                    Geocoder geo = new Geocoder(MapsActivity.this, Locale.getDefault());
                    List<Address> add = geo.getFromLocation(arg0.latitude, arg0.longitude, 1);
                    String selectedCountry;
                    if (add.size() > 0) {
                        selectedCountry = add.get(0).getCountryName();
                        selectedState = selectedCountry;

                        Log.d("country", selectedCountry);
                        //For usa go with states . All other countries - it gives the capital
                        if (selectedCountry.equalsIgnoreCase("United States") ||
                                selectedCountry.equalsIgnoreCase("US")) {
                            selectedState = add.get(0).getAdminArea();
                        }
                        Log.d("state", selectedState);
                        ConvertTextToSpeech();
                    }
                    Log.d("arg0", arg0.latitude + "-" + arg0.longitude);
                } catch (Exception e) {

                }
            }
        });
        mMap = googleMap;

        // Add a marker in California and move the camera
        LatLng sydney = new LatLng(37, -122);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Click anywhere to get more info "));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    /*
    Async Task of connecting to rest api.
     */
    private class LongRunningGetIO extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
            try {
                Log.d(TAG, "invoking capitals rest api");
                background();
            } catch (Exception e) {
                Log.e(TAG, "invoking capitals rest api failed", e);
            }
            return null;
        }
    }
}
