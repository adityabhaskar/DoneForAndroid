package net.c306.done.sync;

public class IDTAuthStrings {
    
    /****
     * CONSTANTS FOR THE AUTHORIZATION PROCESS
     ****/
    
    // This is the public api key of our application
    public static final String CLIENT_ID = "";
    
    // This is the authorisation key for the header
    public static final String AUTH_HEADER = "";
    
    // This is any string we want to use. This will be used for avoid CSRF attacks. You can generate one here: http://strongpasswordgenerator.com/
    public static final String STATE = "";
    
    // This is the url that Auth process will redirect to. 
    // We can put whatever we want that starts with https:// .
    // We use a made up url that we will intercept when redirecting. Avoid Uppercases. 
    public static final String REDIRECT_URI = "";
    
}
