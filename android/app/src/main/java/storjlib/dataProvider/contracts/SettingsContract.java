package storjlib.dataProvider.contracts;

import android.provider.BaseColumns;

/**
 * Created by Yaroslav-Note on 3/19/2018.
 */

public final class SettingsContract implements BaseColumns {

    private SettingsContract() {}

    public final static String TABLE_NAME = "settingsTable";

    public final static String _SETTINGS_ID = "settingsId";
    public final static String _SYNC_SETTINGS = "syncSettings";
    public final static String _LAST_SYNC = "lastSync";

    //public final static String

    public static String createTable() {
        return String.format(
                "create table if not exists %s (" +
                        "%s TEXT primary key not null, " +
                        "%s NUMBER DEFAULT 0, " +
                        "%s TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
                TABLE_NAME, _ID, _SYNC_SETTINGS, _LAST_SYNC);
    }
}