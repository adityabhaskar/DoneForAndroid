package net.c306.done.sync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import net.c306.done.LoginActivity;

/**
 * Manages "Authentication" to idonethis's backend service.  The SyncAdapter framework
 * requires an authenticator object, so syncing to a service that doesn't need authentication
 * typically means creating a stub authenticator like this one.
 * This code is copied directly, in its entirety, from
 * http://developer.android.com/training/sync-adapters/creating-authenticator.html
 * Which is a pretty handy reference when creating your own syncadapters.  Just sayin'.
 */
public class IDTAuthenticator extends AbstractAccountAuthenticator {
    
    private Context mContext;
    
    public IDTAuthenticator(Context context) {
        super(context);
        mContext = context;
    }
    
    // No properties to edit.
    @Override
    public Bundle editProperties(
            AccountAuthenticatorResponse r, String s) {
        throw new UnsupportedOperationException();
    }
    
    // Because we're not actually adding an account to the device, just return null.
    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse response,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle options) throws NetworkErrorException {
    
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        intent.putExtra(LoginActivity.ARG_IS_ADDING_NEW_ACCOUNT, true);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
    
    // Ignore attempts to confirm credentials
    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response,
            Account account,
            Bundle bundle) throws NetworkErrorException {
        return null;
    }
    
    
    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse response,
            Account account,
            String authTokenType,
            Bundle options) throws NetworkErrorException {
        
        // Extract the username and password from the Account Manager, and ask
        // the server for an appropriate AuthToken.
        final AccountManager am = AccountManager.get(mContext);
        
        String authToken = am.peekAuthToken(account, authTokenType);
        
        // Lets give another try to authenticate the user
        if (TextUtils.isEmpty(authToken)) {
            final String password = am.getPassword(account);
            if (password != null) {
                //authToken = sServerAuthenticate.userSignIn(account.name, password, authTokenType);
            }
        }
        
        // If we get an authToken - we return it
        if (!TextUtils.isEmpty(authToken)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
            result.putString(AccountManager.KEY_AUTHTOKEN, authToken);
            return result;
        }
        
        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our LoginActivity.
        final Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        intent.putExtra(LoginActivity.ARG_ACCOUNT_TYPE, account.type);
        intent.putExtra(LoginActivity.ARG_AUTH_TYPE, authTokenType);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }
    
    // Getting a label for the auth token is not supported
    @Override
    public String getAuthTokenLabel(String accountType) {
        throw new UnsupportedOperationException();
    }
    
    // Updating user credentials is not supported
    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response,
            Account account,
            String accountType, Bundle bundle) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
    
    // Checking features for the account is not supported
    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response,
            Account account, String[] requiredFeatures) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}