package net.c306.done.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import net.c306.done.sync.IDTAuthenticator;

public class IDTAuthenticatorService extends Service {
    
    /**
     * The service which allows the sync adapter framework to access the authenticator.
     */
    // Instance field that stores the authenticator object
    private IDTAuthenticator mAuthenticator;
    
    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new IDTAuthenticator(this);
    }
    
    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
