/*
 * Copyright 2012 two forty four a.m. LLC <http://www.twofortyfouram.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package de.alxwlb.android.locale.plugin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import de.alxwlb.android.locale.plugin.Constants;
import de.alxwlb.android.locale.plugin.bundle.BundleScrubber;
import de.alxwlb.android.locale.plugin.bundle.PluginBundleManager;
import de.alxwlb.android.locale.plugin.ui.EditActivity;

/**
 * This is the "query" BroadcastReceiver for a Locale Plug-in condition.
 */
public final class QueryReceiver extends BroadcastReceiver
{

    /**
     * @param context {@inheritDoc}.
     * @param intent the incoming {@link com.twofortyfouram.locale.Intent#ACTION_QUERY_CONDITION} Intent. This should always
     *            contain the {@link com.twofortyfouram.locale.Intent#EXTRA_BUNDLE} that was saved by {@link EditActivity} and
     *            later broadcast by Locale.
     */
    @Override
    public void onReceive(final Context context, final Intent intent)
    {
        /*
         * Always be sure to be strict on input parameters! A malicious third-party app could always send an empty or otherwise
         * malformed Intent. And since Locale applies settings in the background, the plug-in definitely shouldn't crash in the
         * background
         */

        if (!com.twofortyfouram.locale.Intent.ACTION_QUERY_CONDITION.equals(intent.getAction()))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, String.format("Received unexpected Intent action %s", intent.getAction())); //$NON-NLS-1$
            }
            return;
        }

        /*
         * A hack to prevent a private serializable classloader attack
         */
        BundleScrubber.scrub(intent);
        BundleScrubber.scrub(intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE));

        final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.locale.Intent.EXTRA_BUNDLE);

        /*
         * Verify the Bundle is correct
         */
        if (!PluginBundleManager.isBundleValid(bundle))
        {
            if (Constants.IS_LOGGABLE)
            {
                Log.e(Constants.LOG_TAG, "Received an invalid bundle"); //$NON-NLS-1$
            }

            return;
        }

        final boolean isScreenOn = (((PowerManager) context.getSystemService(Context.POWER_SERVICE)).isScreenOn());
        final boolean conditionState = bundle.getBoolean(PluginBundleManager.BUNDLE_EXTRA_BOOLEAN_STATE);

        if (Constants.IS_LOGGABLE)
        {
            Log.v(Constants.LOG_TAG, String.format("Screen state is %b and condition state is %b", Boolean.valueOf(isScreenOn), Boolean.valueOf(conditionState))); //$NON-NLS-1$
        }

        if (isScreenOn)
        {
            if (conditionState)
            {
                setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
            }
            else
            {
                setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            }
        }
        else
        {
            if (conditionState)
            {
                setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_UNSATISFIED);
            }
            else
            {
                setResultCode(com.twofortyfouram.locale.Intent.RESULT_CONDITION_SATISFIED);
            }
        }

        /*
         * Because conditions are queried in the background and possibly while the phone is asleep, it is necessary to acquire a
         * WakeLock in order to guarantee that the service is started.
         */
        ServiceWakeLockManager.aquireLock(context);

        /*
         * To detect screen changes as they happen, a service must be running because the SCREEN_ON/OFF Intents are
         * REGISTERED_RECEIVER_ONLY.
         * 
         * To avoid a gap in detecting screen on/off changes, the current state of the screen needs to be sent to the service.
         */
        context.startService(new Intent(context, BackgroundService.class).putExtra(BackgroundService.EXTRA_BOOLEAN_WAS_SCREEN_ON, isScreenOn));
    }
}