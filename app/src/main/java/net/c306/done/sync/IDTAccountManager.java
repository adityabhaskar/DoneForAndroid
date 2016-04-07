package net.c306.done.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.Log;

import net.c306.done.R;
import net.c306.done.Utils;

public class IDTAccountManager {
    
    private static final String LOG_TAG = Utils.LOG_TAG + IDTAccountManager.class.getSimpleName();
    
    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return The sync account, or null if none found
     */
    @Nullable
    public static Account getSyncAccount(Context context) {
    
        // Get username
        String username = Utils.getUsername(context);
        if (username == null || username.equals("")) {
            Log.w(LOG_TAG, "No username found");
            return null;
        }
    
        // Get accessToken
        String accessToken = Utils.getAccessToken(context);
        if (accessToken == null || accessToken.equals("")) {
            Log.w(LOG_TAG, "No auth token found");
            return null;
        }
        
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        
        // Create the account type and default account
        Account newAccount = new Account(
                Utils.getUsername(context), context.getString(R.string.sync_account_type));
        
        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {
            Log.v(LOG_TAG, "No sync account found!");
            return null;
        }
        
        Log.v(LOG_TAG, "Found account for: " + newAccount.name);
        return newAccount;
    }
    
    
    @Nullable
    public static Account createSyncAccount(Context context) {
    
        // Get username
        String username = Utils.getUsername(context);
        if (username == null || username.equals("")) {
            Log.w(LOG_TAG, "No username found");
            return null;
        }
    
        // Get accessToken
        String accessToken = Utils.getAccessToken(context);
        if (accessToken == null || accessToken.equals("")) {
            Log.w(LOG_TAG, "No auth token found");
            return null;
        }
    
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
    
        // Create the account type and default account
        Account newAccount = new Account(
                username, context.getString(R.string.sync_account_type));
    
        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {
            Log.v(LOG_TAG, "Account doesn't exist, trying to create it");
            
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
    
            accountManager.setAuthToken(newAccount, Utils.AUTH_TOKEN, accessToken);
            
            IDTSyncAdapter.onAccountCreated(newAccount, context);
        }
    
        return newAccount;
    }
    
    
    @Nullable
    public static Account updateSyncAccountToken(Context context) {
        
        String username = Utils.getUsername(context);
        if (username == null || username.equals("")) {
            Log.e(LOG_TAG, "No username found");
            return null;
        }
        
        String authToken = Utils.getAccessToken(context);
        
        if (authToken == null || authToken.equals("")) {
            Log.e(LOG_TAG, "No auth token found");
            return null;
        }
        
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        
        // Create the account type and default account
        Account newAccount = new Account(
                username, context.getString(R.string.sync_account_type));
        
        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {
            Log.v(LOG_TAG, "Account doesn't exist, trying to create it");
            
            /*
             * Add the account and account type, no password or user data
             * If successful, return the Account object, otherwise report an error.
             */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
    
            accountManager.setAuthToken(newAccount, Utils.AUTH_TOKEN, authToken);
    
            IDTSyncAdapter.onAccountCreated(newAccount, context);
        } else {
            accountManager.setAuthToken(newAccount, Utils.AUTH_TOKEN, authToken);
        }
        
        return newAccount;
    }
    
    
    public static void removeSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        
        // Create the account type and default account
        Account account = new Account(
                Utils.getUsername(context), context.getString(R.string.sync_account_type));
        
        // Remove authToken
        accountManager.setAuthToken(account, Utils.AUTH_TOKEN, null);
        
        // If the password doesn't exist, the account doesn't exist
        if (null != accountManager.getPassword(account)) {
            
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1)
                accountManager.removeAccount(account, null, null);
            else
                accountManager.removeAccountExplicitly(account);
            
        }
    }
    
    @Nullable
    public static String getAuthToken(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        
        // Create the account type and default account
        Account account = new Account(
                Utils.getUsername(context), context.getString(R.string.sync_account_type));
        
        // If the password doesn't exist, the account doesn't exist
        if (null != accountManager.getPassword(account))
            return accountManager.peekAuthToken(account, Utils.AUTH_TOKEN);
        else
            return null;
    }
    
}
