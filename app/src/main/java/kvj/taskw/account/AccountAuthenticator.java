package kvj.taskw.account;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import timber.log.Timber;

import kvj.taskw.ui.AccountAddDialog;

/**
 * Created by vorobyev on 11/17/15.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {

    private final Context context;

    public static class Service extends android.app.Service {

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return new AccountAuthenticator(this).getIBinder();
        }
    }

    public AccountAuthenticator(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Timber.d("editProperties %s %s", accountType, response);
        return null;
    }

    @Override
    public Bundle addAccount(final AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options)
        throws NetworkErrorException {
        Timber.d("addAccount %s %s %s %s", accountType, authTokenType, options, response);
        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, new Intent(context, AccountAddDialog.class));
        return bundle;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
                                     Bundle options) throws NetworkErrorException {
        Timber.d("confirmCredentials %s %s %s", account, options, response);
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
                               String authTokenType, Bundle options) throws NetworkErrorException {
        Timber.d("getAuthToken %s %s %s %s", account, authTokenType, options, response);
        return null;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return authTokenType;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
                                    String authTokenType, Bundle options)
        throws NetworkErrorException {
        Timber.d("updateCredentials %s %s %s %s", account, authTokenType, options, response);
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
                              String[] features) throws NetworkErrorException {
        Timber.d("hasFeatures %s %s", account, response);
        return null;
    }
}
