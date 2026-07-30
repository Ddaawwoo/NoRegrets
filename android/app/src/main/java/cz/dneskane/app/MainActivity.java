package cz.dneskane.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.app.role.RoleManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int REQ_CONTACT = 10;
    private static final int REQ_CONTACT_PERMISSION = 11;
    private final Handler handler = new Handler();
    private final List<LockStore.ProtectedContact> contacts = new ArrayList<>();
    private long selectedUntil;
    private TextView status;
    private TextView countdown;
    private TextView contactList;
    private TextView selectedTime;
    private Button roleButton;
    private Button contactButton;
    private Button timeButton;
    private Button activateButton;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LockStore.clearExpired(this);
        contacts.addAll(LockStore.getContacts(this));
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, 1);
        c.set(Calendar.HOUR_OF_DAY, 9);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        selectedUntil = c.getTimeInMillis();
        buildUi();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.post(ticker);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(ticker);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(40));
        root.setBackgroundColor(Color.rgb(248, 246, 243));
        scroll.addView(root);

        TextView title = label("DneskaNe", 34);
        title.setTextColor(Color.rgb(210, 48, 68));
        root.addView(title);
        TextView subtitle = label("Zamkni vybrané kontakty dřív, než večer začne rozhodovat za tebe.", 17);
        subtitle.setPadding(0, dp(8), 0, dp(22));
        root.addView(subtitle);

        status = label("", 23);
        root.addView(status);
        countdown = label("", 38);
        countdown.setGravity(Gravity.CENTER);
        countdown.setPadding(0, dp(14), 0, dp(14));
        root.addView(countdown);
        selectedTime = label("", 15);
        root.addView(selectedTime);

        roleButton = button("Povolit blokování hovorů");
        roleButton.setOnClickListener(v -> requestRole());
        root.addView(roleButton);

        contactList = label("", 16);
        contactList.setPadding(0, dp(18), 0, dp(10));
        root.addView(contactList);

        contactButton = button("Přidat kontakt");
        contactButton.setOnClickListener(v -> pickContact());
        root.addView(contactButton);

        timeButton = button("Nastavit konec zámku");
        timeButton.setOnClickListener(v -> pickDateTime());
        root.addView(timeButton);

        activateButton = button("AKTIVOVAT ZÁMEK");
        activateButton.setOnClickListener(v -> confirmOne());
        root.addView(activateButton);

        TextView note = label("Po aktivaci neexistuje nouzové ani předčasné odemčení. Zámek skončí až ve zvolený čas.", 14);
        note.setGravity(Gravity.CENTER);
        note.setPadding(0, dp(14), 0, 0);
        root.addView(note);
        setContentView(scroll);
    }

    private TextView label(String text, int size) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.rgb(30, 30, 34));
        return view;
    }

    private Button button(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setMinHeight(dp(54));
        return button;
    }

    private void refresh() {
        LockStore.clearExpired(this);
        boolean active = LockStore.isActive(this);
        long until = active ? LockStore.getLockedUntil(this) : selectedUntil;
        long remaining = Math.max(0, until - System.currentTimeMillis());
        status.setText(active ? "Zámek je aktivní" : "Zámek není aktivní");
        countdown.setText(active ? formatDuration(remaining) : "Připraveno");
        selectedTime.setText("Konec: " + DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(until));

        List<LockStore.ProtectedContact> shown = active ? LockStore.getContacts(this) : contacts;
        if (shown.isEmpty()) contactList.setText("Zatím není vybraný žádný kontakt.");
        else {
            StringBuilder out = new StringBuilder("Chráněné kontakty:\n");
            for (LockStore.ProtectedContact contact : shown) out.append("• ").append(contact.name).append("  ").append(contact.number).append("\n");
            contactList.setText(out.toString().trim());
        }

        boolean role = hasRole();
        roleButton.setEnabled(!active && !role);
        roleButton.setText(role ? "Blokování hovorů povoleno" : "Povolit blokování hovorů");
        contactButton.setEnabled(!active);
        timeButton.setEnabled(!active);
        activateButton.setEnabled(!active && role && !contacts.isEmpty() && selectedUntil > System.currentTimeMillis());
        activateButton.setText(active ? "ZÁMEK AKTIVNÍ" : "AKTIVOVAT ZÁMEK");
    }

    private boolean hasRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        return manager != null && manager.isRoleHeld(RoleManager.ROLE_CALL_REDIRECTION);
    }

    private void requestRole() {
        RoleManager manager = getSystemService(RoleManager.class);
        if (manager != null && manager.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION)) {
            startActivity(manager.createRequestRoleIntent(RoleManager.ROLE_CALL_REDIRECTION));
        } else Toast.makeText(this, "Telefon tuto systémovou roli nepodporuje.", Toast.LENGTH_LONG).show();
    }

    private void pickContact() {
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQ_CONTACT_PERMISSION);
            return;
        }
        startActivityForResult(new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI), REQ_CONTACT);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_CONTACT_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) pickContact();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CONTACT && resultCode == RESULT_OK && data != null) readContact(data.getData());
        refresh();
    }

    private void readContact(Uri uri) {
        if (uri == null) return;
        String[] columns = {ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (Cursor cursor = getContentResolver().query(uri, columns, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) contacts.add(new LockStore.ProtectedContact(cursor.getString(0), cursor.getString(1)));
        }
    }

    private void pickDateTime() {
        Calendar current = Calendar.getInstance();
        current.setTimeInMillis(selectedUntil);
        new DatePickerDialog(this, (view, year, month, day) -> new TimePickerDialog(this, (timeView, hour, minute) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, day, hour, minute, 0);
            chosen.set(Calendar.MILLISECOND, 0);
            selectedUntil = chosen.getTimeInMillis();
            refresh();
        }, current.get(Calendar.HOUR_OF_DAY), current.get(Calendar.MINUTE), true).show(), current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmOne() {
        new AlertDialog.Builder(this).setTitle("Opravdu aktivovat?").setMessage("Vybrané kontakty nepůjde volat až do nastaveného času.").setNegativeButton("Zrušit", null).setPositiveButton("Pokračovat", (d, w) -> confirmTwo()).show();
    }

    private void confirmTwo() {
        new AlertDialog.Builder(this).setTitle("Poslední kontrola").setMessage("Nouzové odemčení neexistuje. Zámek skončí " + DateFormat.getDateTimeInstance().format(selectedUntil) + ".").setNegativeButton("Zrušit", null).setPositiveButton("Aktivovat napevno", (d, w) -> {
            LockStore.activate(this, selectedUntil, contacts);
            refresh();
        }).show();
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
