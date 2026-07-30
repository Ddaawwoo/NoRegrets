package cz.dneskane.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.PhoneNumberUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class LockStore {
    private static final String PREFS = "dneskane_local_lock";
    private static final String KEY_UNTIL = "locked_until";
    private static final String KEY_CONTACTS = "contacts";

    private LockStore() {}

    public static final class ProtectedContact {
        public final String name;
        public final String number;

        public ProtectedContact(String name, String number) {
            this.name = name == null || name.trim().isEmpty() ? number : name;
            this.number = number == null ? "" : number;
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long getLockedUntil(Context context) {
        return prefs(context).getLong(KEY_UNTIL, 0L);
    }

    public static boolean isActive(Context context) {
        return getLockedUntil(context) > System.currentTimeMillis();
    }

    public static void activate(Context context, long until, List<ProtectedContact> contacts) {
        prefs(context).edit().putLong(KEY_UNTIL, until).putString(KEY_CONTACTS, encode(contacts)).apply();
    }

    public static void clearExpired(Context context) {
        if (getLockedUntil(context) > 0 && !isActive(context)) prefs(context).edit().clear().apply();
    }

    public static List<ProtectedContact> getContacts(Context context) {
        return decode(prefs(context).getString(KEY_CONTACTS, "[]"));
    }

    public static boolean isProtectedNumber(Context context, String dialedNumber) {
        if (!isActive(context) || dialedNumber == null) return false;
        for (ProtectedContact c : getContacts(context)) {
            if (PhoneNumberUtils.compare(c.number, dialedNumber)) return true;
        }
        return false;
    }

    private static String encode(List<ProtectedContact> contacts) {
        JSONArray array = new JSONArray();
        for (ProtectedContact contact : contacts) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", contact.name);
                obj.put("number", contact.number);
                array.put(obj);
            } catch (JSONException ignored) {}
        }
        return array.toString();
    }

    private static List<ProtectedContact> decode(String json) {
        List<ProtectedContact> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json == null ? "[]" : json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new ProtectedContact(obj.optString("name"), obj.optString("number")));
            }
        } catch (JSONException ignored) {}
        return result;
    }
}
