package cz.dneskane.app;

import android.net.Uri;
import android.telecom.CallRedirectionService;
import android.telecom.PhoneAccountHandle;

public class DneskaNeCallRedirectionService extends CallRedirectionService {
    @Override
    public void onPlaceCall(Uri handle, PhoneAccountHandle initialPhoneAccount, boolean allowInteractiveResponse) {
        String number = handle == null ? null : handle.getSchemeSpecificPart();
        if (LockStore.isProtectedNumber(this, number)) cancelCall();
        else placeCallUnmodified();
    }
}
