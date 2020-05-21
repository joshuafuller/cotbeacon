package com.jon.cotbeacon.service;

import android.content.SharedPreferences;

import com.jon.cotbeacon.cot.CotRole;
import com.jon.cotbeacon.cot.CursorOnTarget;
import com.jon.cotbeacon.cot.PliCursorOnTarget;
import com.jon.cotbeacon.cot.UtcTimestamp;
import com.jon.cotbeacon.enums.TeamColour;
import com.jon.cotbeacon.utils.DeviceUid;
import com.jon.cotbeacon.utils.Key;
import com.jon.cotbeacon.utils.PrefUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class GpsCotGenerator extends CotGenerator {
    private PliCursorOnTarget cot = null;

    GpsCotGenerator(SharedPreferences prefs) {
        super(prefs);
    }

    @Override
    protected List<CursorOnTarget> generate() {
        return (cot == null) ? initialise() : update();
    }

    @Override
    protected List<CursorOnTarget> initialise() {
        cot = new PliCursorOnTarget();
        cot.uid = DeviceUid.get();
        cot.callsign = PrefUtils.getString(prefs, Key.CALLSIGN);
        cot.role = CotRole.fromPrefs(prefs);
        final UtcTimestamp now = UtcTimestamp.now();
        cot.time = now;
        cot.start = now;
        cot.setStaleDiff(PrefUtils.getInt(prefs, Key.STALE_TIMER), TimeUnit.MINUTES);
        cot.team = TeamColour.fromPrefs(prefs).get();
        LastGpsLocation.updatePli(cot);
        return toList(cot);
    }

    @Override
    protected List<CursorOnTarget> update() {
        LastGpsLocation.updatePli(cot);
        return toList(cot);
    }

    @Override
    protected void clear() {
        cot = null;
    }

    private List<CursorOnTarget> toList(CursorOnTarget c) {
        return Collections.singletonList(c);
    }
}
