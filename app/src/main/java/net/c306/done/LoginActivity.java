package net.c306.done;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import net.c306.done.auth.AccountAuthenticatorActivity;
import net.c306.done.sync.IDTAccountManager;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AccountAuthenticatorActivity {
    
    /****
     * Constants used by Account Manager
     ****/
    
    public static final String ARG_ACCOUNT_TYPE = "";
    public static final String ARG_AUTH_TYPE = "";
    public static final String ARG_IS_ADDING_NEW_ACCOUNT = "";
    /****
     * Constants used to build the urls
     ****/
    
    private static final String RESPONSE_TYPE_PARAM = "response_type";
    private static final String RESPONSE_TYPE_VALUE = "code";
    private static final String GRANT_TYPE_PARAM = "grant_type";
    private static final String GRANT_TYPE = "authorization_code";
    private static final String CLIENT_ID_PARAM = "client_id";
    private static final String STATE_PARAM = "state";
    private static final String REDIRECT_URI_PARAM = "redirect_uri";
    /*----------------------------------------*/
    private static final String QUESTION_MARK = "?";
    private static final String AMPERSAND = "&";
    private static final String EQUALS = "=";
    private final String LOG_TAG = Utils.LOG_TAG + getClass().getSimpleName();
    
    /*******************************************/
    /****
     * UI elements
     ****/
    
    private WebView mWebView;
    private ProgressBar mProgressBar;
    private TextView mProgressStatus;
    private Bus bus = new Bus();
    private ProgressDialog mWebViewLoadingProgress;
    
    /*********************/
    
    /**
     * Method that generates the url for get the access token from the Service
     *
     * @return Url
     */
    private static String getAccessTokenUrl(String authorizationToken) {
        return Utils.IDT_ACCESS_TOKEN_URL + QUESTION_MARK
                + GRANT_TYPE_PARAM + EQUALS + GRANT_TYPE
                + AMPERSAND
                + REDIRECT_URI_PARAM + EQUALS + Utils.REDIRECT_URI
                + AMPERSAND
                + RESPONSE_TYPE_VALUE + EQUALS + authorizationToken;
    }
    
    /**
     * Method that generates the url for get the authorization token from the Service
     *
     * @return Url
     */
    private static String getAuthorizationUrl() {
        return Utils.IDT_AUTHORIZATION_URL + QUESTION_MARK
                + RESPONSE_TYPE_PARAM + EQUALS + RESPONSE_TYPE_VALUE
                + AMPERSAND
                + CLIENT_ID_PARAM + EQUALS + Utils.CLIENT_ID
                + AMPERSAND
                + STATE_PARAM + EQUALS + Utils.STATE;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupActionBar();
        
        // Get the webView from the layout
        mWebView = (WebView) findViewById(R.id.login_webview);
        mProgressBar = (ProgressBar) findViewById(R.id.login_progress);
        mProgressStatus = (TextView) findViewById(R.id.login_status);
        
        // Attach listener to update bus
        bus.register(this);
        
        // Request focus for the webview
        if (mWebView != null)
            mWebView.requestFocus(View.FOCUS_DOWN);
        
        if (mProgressBar != null)
            mProgressBar.setIndeterminate(true);
        
        // Setup webview redirect handling to intercept and acquire authorisation token 
        mWebView.setWebViewClient(new LoginWebViewClient());
        
        String authUrl = getAuthorizationUrl();
        Log.v(LOG_TAG, "Authorize: " + "Loading Auth Url: " + authUrl);
        
        //Load the authorization URL into the webView
        mWebView.loadUrl(authUrl);
    }
    
    
    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    
    @Subscribe
    public void loginUpdateListener(LoginUpdateEvent event) {
        
        if (mProgressStatus != null) {
            if (event.message != null) {
                mProgressStatus.setText(event.message);
                mProgressStatus.setVisibility(View.VISIBLE);
            } else
                mProgressStatus.setVisibility(View.GONE);
        }
        
        if (mProgressBar != null)
            mProgressBar.setVisibility(event.progressBarStatus);
        
        if (event.finished != Utils.LOGIN_UNFINISHED) {
            Log.v(LOG_TAG, "Login activity finished. " + event.finished);
            setResult(event.finished == Utils.LOGIN_FINISHED ? RESULT_OK : RESULT_CANCELED);
    
            // If successfully logged in, start MainActivity
            if (event.finished == Utils.LOGIN_FINISHED) {
                Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(mainActivity);
            }
            
            finish();
        }
    }
    
    
    public class LoginUpdateEvent {
        public final String message;
        public final int progressBarStatus;
        public final int finished; // -1 = not finished yet, 0 = ok, 1+ = error 
        
        public LoginUpdateEvent(String message, int progressBarStatus) {
            this.message = message;
            this.progressBarStatus = progressBarStatus;
            this.finished = Utils.LOGIN_UNFINISHED;
        }
        
        public LoginUpdateEvent(String message, int progressBarStatus, int finished) {
            this.message = message;
            this.progressBarStatus = progressBarStatus;
            this.finished = finished;
        }
        
        @Override
        public String toString() {
            return message + " :: " + progressBarStatus + " :: " + finished;
        }
    }
    
    
    private class RequestAccessTokenAsyncTask extends AsyncTask<String, Void, Boolean> {
        
        private Context mContext;
        private Bus updateBus;
        
        public RequestAccessTokenAsyncTask(Context c, Bus b) {
            mContext = c;
            updateBus = b;
        }
        
        @Override
        protected void onPreExecute() {
            updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_EXCHANGE_ACCESS_TOKEN), View.VISIBLE));
        }
        
        @Override
        protected Boolean doInBackground(String... params) {
            if (params.length > 0) {
                String url = params[0];
                
                HttpsURLConnection httpcon = null;
                String result = "";
                
                if (!isCancelled()) {
                    
                    try {
                        //String authHeader =  "Basic " + Base64.encodeToString(
                        //        (CLIENT_ID + ':' + CLIENT_SECRET).getBytes(),
                        //        Base64.DEFAULT
                        //);
                        
                        //Connect
                        httpcon = (HttpsURLConnection) (new URL(url).openConnection());
                        httpcon.setDoOutput(true);
                        httpcon.setRequestProperty("Authorization", Utils.AUTH_HEADER);
                        httpcon.setRequestProperty("Cache-Control", "no-cache");
                        httpcon.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                        httpcon.setRequestProperty("Accept", "application/json");
                        httpcon.setRequestMethod("POST");
                        httpcon.setUseCaches(false);
                        
                        httpcon.connect();
                        
                        //Response Code
                        int responseCode = httpcon.getResponseCode();
                        String responseMessage = httpcon.getResponseMessage();
                        String responseContent = Utils.readResponse(httpcon);
                        Log.v(LOG_TAG, "Full response string: " + responseContent);
                        
                        result = responseContent;
                        
                        if (responseCode == HttpURLConnection.HTTP_OK && !responseContent.equals("")) {
                            Log.v(LOG_TAG, "Received access token - " + responseCode + ": " + responseMessage);
                            
                            // Convert the string result to a JSON Object
                            JSONObject responseObject = new JSONObject(responseContent);
                            
                            // Extract data from JSON Response
                            int expiresIn = responseObject.has("expires_in") ? responseObject.getInt("expires_in") : 0;
                            String accessToken = responseObject.has("access_token") ? responseObject.getString("access_token") : null;
                            String refreshToken = responseObject.has("refresh_token") ? responseObject.getString("refresh_token") : null;
                            
                            if (expiresIn > 0 && accessToken != null) {
                                Log.v(LOG_TAG, "This is the access Token: " + accessToken + ". It will expires in " + expiresIn + " secs");
                                
                                //Calculate date of expiration
                                Calendar calendar = Calendar.getInstance();
                                calendar.add(Calendar.SECOND, expiresIn);
                                long expireDate = calendar.getTimeInMillis();
                                
                                //Store both expires in and access token in shared preferences
                                SharedPreferences preferences = LoginActivity.this.getSharedPreferences(Utils.USER_DETAILS_PREFS_FILENAME, 0);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putLong("expires", expireDate);
                                editor.putString("accessToken", accessToken);
                                editor.putString("refreshToken", refreshToken);
                                editor.apply();
                                
                                httpcon.disconnect();
                                return true;
                            }
                            
                        } else {
                            Log.w(LOG_TAG, "Didn't receive access token - " + responseCode + ": " + responseMessage);
                            Log.w(LOG_TAG, "Error message from server: " + responseContent);
                        }
                        
                    } catch (Exception e) {
                        result = e.getMessage();
                        e.printStackTrace();
                    } finally {
                        if (httpcon != null)
                            httpcon.disconnect();
                    }
                }
                
                Log.v(LOG_TAG, "Response from server: " + result);
            }
            return false;
        }
        
        @Override
        protected void onPostExecute(Boolean status) {
            if (status) {
                Log.v(LOG_TAG, "Retrieved access token successfully");
                
                updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_ACCESS_TOKEN_ACQUIRED), View.VISIBLE));
                
                new GetUserNameAsyncTask(mContext, updateBus).execute();
            } else {
                // Error handling - cancel activity, notify user.
                Log.v(LOG_TAG, "Error occurred, cancelling login attempt.");
                updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_ERROR_OR_CANCELLED), View.INVISIBLE, Utils.LOGIN_CANCELLED_OR_ERROR));
            }
        }
    }
    
    
    private class GetUserNameAsyncTask extends AsyncTask<Void, Void, String> {
        
        private Context mContext;
        private Bus updateBus;
        
        public GetUserNameAsyncTask(Context c, Bus b) {
            mContext = c;
            updateBus = b;
        }
        
        @Override
        protected void onPreExecute() {
            updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_FETCH_USERNAME), View.VISIBLE));
            
            // If access token is expired, cancel.
            if (new Date().after(
                    new Date(Utils.getExpiryTime(mContext))
            )) {
                cancel(true);
                Log.w(LOG_TAG, mContext.getString(R.string.ACCESS_TOKEN_EXPIRED));
                updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.ACCESS_TOKEN_EXPIRED), View.INVISIBLE, Utils.LOGIN_CANCELLED_OR_ERROR));
                return;
            }
            
            Log.v(LOG_TAG, "Fetching username");
        }
        
        @Override
        protected String doInBackground(Void... voids) {
            
            String accessToken = Utils.getAccessToken(mContext);
            if (accessToken == null) {
                cancel(true);
                Log.w(LOG_TAG, "No access token found!");
                return null;
            } else
                Log.v(LOG_TAG, "Access Token: " + accessToken);
            
            HttpURLConnection httpcon = null;
            String responseMessage = "Empty";
            
            // Contains server response (or error message)
            String response = null;
            
            if (!isCancelled()) {
                try {
                    //Connect
                    httpcon = (HttpURLConnection) ((new URL(Utils.IDT_NOOP_URL).openConnection()));
                    httpcon.setRequestProperty("Authorization", "Bearer " + accessToken);
                    httpcon.setRequestProperty("Content-Type", "application/json");
                    httpcon.setRequestProperty("Accept", "application/json");
                    httpcon.setRequestMethod("GET");
                    httpcon.connect();
                    
                    //Response Code
                    int resultStatus = httpcon.getResponseCode();
                    responseMessage = httpcon.getResponseMessage();
                    response = Utils.readResponse(httpcon);
                    
                    // Extract username
                    if (resultStatus == HttpURLConnection.HTTP_OK && !response.equals("")) {
                        Log.v(LOG_TAG, "NOOP Succeeded: " + resultStatus + ": " + responseMessage);
                        
                        JSONObject masterObj = new JSONObject(response);
                        String username = masterObj.getString("user");
                        
                        if (null != username && !username.equals("")) {
                            
                            // Save username in shared preferences
                            Utils.setUsername(mContext, username);
                            Log.v(LOG_TAG, "Received username: " + username);
                            response = username;
                            
                        } else {
                            
                            // Remove username from shared preferences
                            Utils.setUsername(mContext, null);
                            //Utils.setTokenValidity(mContext, false);
                            
                        }
                        
                    } else {
                        
                        Log.w(LOG_TAG, "Error response: \n" + response);
                        response = null;
                        Log.w(LOG_TAG, "NOOP Failed: " + resultStatus + ": " + responseMessage);
                        
                    }
                    
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, "Exception1: " + e.getMessage());
                    Log.e(LOG_TAG, "Exception2: " + e.toString());
                    Log.e(LOG_TAG, "Exception3: " + responseMessage);
                    response = null;
                } finally {
                    if (httpcon != null)
                        httpcon.disconnect();
                }
            }
            
            return response;
        }
        
        @Override
        protected void onPostExecute(String username) {
            if (username != null) {
                Log.v(LOG_TAG, "Retrieved username successfully");
                
                // Create sync account with username. Also schedules and starts sync
                if (null != IDTAccountManager.createSyncAccount(mContext)) {
                    
                    // Send message to finish activity
                    updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_USERNAME_ACQUIRED), View.VISIBLE, Utils.LOGIN_FINISHED));
                    
                    return;
                }
            }
            
            // Error handling - cancel activity, notify user.
            Log.v(LOG_TAG, "Error occurred, cancelling login attempt.");
            updateBus.post(new LoginUpdateEvent(mContext.getString(R.string.LOGIN_ERROR_OR_CANCELLED), View.INVISIBLE, Utils.LOGIN_CANCELLED_OR_ERROR));
    
        }
    }
    
    
    private class LoginWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            
            // Retrieve auth token from url here on redirect to REDIRECT_URL 
            if (url.startsWith(Utils.REDIRECT_URI)) {
                
                // Hide webview, show progress bar
                mWebView.setVisibility(View.GONE);
                LoginActivity.this.setProgressBarVisibility(false);
                
                Uri uri = Uri.parse(url);
                
                // We take from the url the authorizationToken and the state token. 
                // We have to check that the state token returned by the Service is the same we sent.
                // If not, that means the request may be a result of CSRF and must be rejected.
                
                String stateToken = uri.getQueryParameter(STATE_PARAM);
                if (stateToken == null || !stateToken.equals(Utils.STATE)) {
                    Log.e(LOG_TAG, "Authorize: " + "State token doesn't match");
                    bus.post(new LoginUpdateEvent("User Cancelled.", View.GONE, Utils.LOGIN_CANCELLED_OR_ERROR));
                    return true;
                }
                
                // If the user doesn't allow authorization to our application, 
                // the authorizationToken Will be null.
                
                String authorizationToken = uri.getQueryParameter(RESPONSE_TYPE_VALUE);
                if (authorizationToken == null) {
                    Log.v(LOG_TAG, "Authorize: " + "The user doesn't allow authorization.");
                    bus.post(new LoginUpdateEvent("User Cancelled.", View.GONE, Utils.LOGIN_CANCELLED_OR_ERROR));
                    return true;
                }
                Log.v(LOG_TAG, "Authorize: " + "Auth token received: " + authorizationToken);
                
                // Generate URL for requesting Access Token
                String accessTokenUrl = getAccessTokenUrl(authorizationToken);
                
                // Request AccessToken in a AsyncTask
                new RequestAccessTokenAsyncTask(getApplicationContext(), bus).execute(accessTokenUrl);
                
            } else {
                // Default behaviour
                Log.v(LOG_TAG, "Authorize: " + "Redirecting to: " + url);
                mWebView.loadUrl(url);
            }
            return true;
        }
        
        @Override
        public void onLoadResource(WebView view, String url) {
            super.onLoadResource(view, url);
            
            if (mWebViewLoadingProgress == null && url.equals(getAuthorizationUrl())) {
                // in standard case YourActivity.this
                mWebViewLoadingProgress = new ProgressDialog(LoginActivity.this);
                mWebViewLoadingProgress.setMessage("Loading...");
                mWebViewLoadingProgress.show();
            }
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                if (mWebViewLoadingProgress.isShowing()) {
                    mWebViewLoadingProgress.dismiss();
                    mWebViewLoadingProgress = null;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }
}
