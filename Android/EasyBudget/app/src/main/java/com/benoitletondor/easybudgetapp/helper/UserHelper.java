/*
 *   Copyright 2015 Benoit LETONDOR
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.benoitletondor.easybudgetapp.helper;

import android.content.Context;
import android.support.annotation.NonNull;

/**
 * Helper to get user status / preferences
 *
 * @author Benoit LETONDOR
 */
public class UserHelper
{
    /**
     * Feature reference for premium (used by Batch)
     */
    public static final String PREMIUM_FEATURE = "PREMIUM";

    /**
     * Is the user a premium user
     *
     * @param context non null context
     * @return true if the user if premium, false otherwise
     */
    public static boolean isUserPremium(@NonNull Context context)
    {
        return false; //Parameters.getInstance(context).getBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, false);
    }
}
