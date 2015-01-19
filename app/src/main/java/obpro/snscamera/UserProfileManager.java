package obpro.snscamera;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by shinny on 2015/01/09.
 */
public class UserProfileManager extends PreferenceActivity {
    private AccountFragment fragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragment = new AccountFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
}
