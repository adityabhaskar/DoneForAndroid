package net.c306.done.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import net.c306.done.Utils;

public class IDTSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static IDTSyncAdapter sIDTSyncAdapter = null;
    
    @Override
    public void onCreate() {
        Log.d(Utils.LOG_TAG + getClass().getSimpleName(), "onCreate - IDTSyncService");
        
        synchronized (sSyncAdapterLock) {
            if (sIDTSyncAdapter == null) {
                sIDTSyncAdapter = new IDTSyncAdapter(getApplicationContext(), true);
            }
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return sIDTSyncAdapter.getSyncAdapterBinder();
    }
}