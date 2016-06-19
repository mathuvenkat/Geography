package aditi.geography;

import android.location.Address;
import android.location.Geocoder;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.FragmentActivity;
//import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    TextToSpeech tts;
    Map<String, String> countryCapitalMap = new HashMap<>();
    Map<String, String> currencyMap = new HashMap<>();
    private String selectedStateOrCountry;
    Properties properties = new Properties();


    private static String TAG = "MapsActivity";
    private String urlString = "https://restcountries.eu/rest/v1/all";
    String propFileUSA = "states.properties";
    String currencyCodes = "currencycodes.csv";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Log.d(TAG, "Invoking onCreate");
        //Log.d(TAG, "size of countryCapitalMap" + countryCapitalMap.size());
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
                    //Log.e(TAG, "Failed to initialize text to speech");
                }
            }
        });
    }

    @Override
    protected void onPause() {
        //Log.d(TAG, "Invoking onPause");
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }


        super.onPause();
    }

    @Override
    protected void onStop() {
        //Log.d(TAG, "Invoking onStop");
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (SSLUtils.conn != null) {
            SSLUtils.conn.disconnect();
        }

        super.onStop();
    }

    private void initMap() {


        InputStream stream = getClass().getClassLoader().getResourceAsStream(propFileUSA);
        try {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception e) {
            //Log.e(TAG, "Could not load prop file");
        }

        String line;
        String arr[];
        stream = getClass().getClassLoader().getResourceAsStream(currencyCodes);
        try {
            if (stream != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                while ((line = br.readLine()) != null) {
                    arr = line.split(",");
                    currencyMap.put(arr[0], arr[1]);
                }
            }
        } catch (Exception e) {

        }


        new LongRunningGetIO().execute();

    }

    /**
     * Make rest call to api to get countries/capitals and populate map
     */
    private void background() {
        InputStream inputStream = null;
        try {
            inputStream = SSLUtils.invokeHttpsApi(urlString);
            //Log.d(TAG, "result is " + inputStream.toString());

        } catch (Exception e) {
            //Log.e(TAG, "Unable to talk to rest countries api", e);
        }

        try {
            SSLUtils.parseJsonCountriesOutput(inputStream, countryCapitalMap, currencyMap);
        } catch (Exception e) {
            //Log.e(TAG, "unable to parse json output from countries api");
        } finally {
            try {
                inputStream.close();
            } catch (Exception e) {

            }
        }
    }


    /*
    Convert text to speech
     */
    private void ConvertTextToSpeech() {
        if (properties.get(selectedStateOrCountry) != null) {
            tts.speak(String.format("This is %s %s", selectedStateOrCountry, properties.get(selectedStateOrCountry)),
                    TextToSpeech.QUEUE_FLUSH, null);

        } else if (countryCapitalMap.get(selectedStateOrCountry) != null) {
            tts.speak(countryCapitalMap.get(selectedStateOrCountry), TextToSpeech.QUEUE_FLUSH, null);
        } else {
            tts.speak(selectedStateOrCountry, TextToSpeech.QUEUE_FLUSH, null);
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
                        selectedStateOrCountry = selectedCountry;

                        //Log.d("country", selectedCountry);
                        //For usa go with states . All other countries - it gives the capital
                        if (selectedCountry.equalsIgnoreCase("United States") ||
                                selectedCountry.equalsIgnoreCase("US")) {
                            selectedStateOrCountry = add.get(0).getAdminArea();
                        }
                        //Log.d("state", selectedStateOrCountry);
                        ConvertTextToSpeech();
                    }
                } catch (Exception e) {
                    //Log.e(TAG, "Failed to initialize map", e);
                }
            }
        });
        mMap = googleMap;

        // Add a marker in California and move the camera
        LatLng californiaMarker = new LatLng(37, -122);
        mMap.addMarker(new MarkerOptions().position(californiaMarker).title("Click anywhere to get more info"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(californiaMarker));
    }

    /*
    Async Task of connecting to rest api.
     */
    private class LongRunningGetIO extends AsyncTask<Void, Void, String> {
        protected String doInBackground(Void... params) {
            try {
                //Log.d(TAG, "invoking capitals rest api");
                background();
            } catch (Exception e) {
                //Log.e(TAG, "invoking capitals rest api failed", e);
            }
            return null;
        }
    }
}
