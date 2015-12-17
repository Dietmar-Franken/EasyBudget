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

package com.benoitletondor.easybudgetapp.view;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.batch.android.Batch;
import com.batch.android.BatchCodeListener;
import com.batch.android.CodeErrorInfo;
import com.batch.android.FailReason;
import com.batch.android.Offer;
import com.benoitletondor.easybudgetapp.BuildConfig;
import com.benoitletondor.easybudgetapp.EasyBudget;
import com.benoitletondor.easybudgetapp.R;
import com.benoitletondor.easybudgetapp.helper.CurrencyHelper;
import com.benoitletondor.easybudgetapp.helper.Logger;
import com.benoitletondor.easybudgetapp.helper.ParameterKeys;
import com.benoitletondor.easybudgetapp.helper.Parameters;
import com.benoitletondor.easybudgetapp.helper.UIHelper;
import com.benoitletondor.easybudgetapp.helper.UserHelper;
import com.benoitletondor.easybudgetapp.view.selectcurrency.SelectCurrencyFragment;
import com.google.android.gms.appinvite.AppInviteInvitation;

import java.util.Map;

/**
 * Fragment to display preferences
 *
 * @author Benoit LETONDOR
 */
public class PreferencesFragment extends PreferenceFragment
{
    /**
     * The dialog to select a new currency (will be null if not shown)
     */
    private SelectCurrencyFragment selectCurrencyDialog;
    /**
     * Broadcast receiver (used for currency selection)
     */
    private BroadcastReceiver      receiver;

// ---------------------------------------->

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the preferences from the XML resource
        addPreferencesFromResource(R.xml.preferences);

        /*
         * Rating button
         */
        findPreference(getResources().getString(R.string.setting_category_rate_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                new RatingPopup(getActivity()).show(true);
                return false;
            }
        });

        /*
         * Bind bug report button
         */
        findPreference(getResources().getString(R.string.setting_category_bug_report_send_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                String localId = Parameters.getInstance(getActivity()).getString(ParameterKeys.LOCAL_ID);

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SENDTO);
                sendIntent.setData(Uri.parse("mailto:")); // only email apps should handle this
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getResources().getString(R.string.bug_report_email)});
                sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.setting_category_bug_report_send_text, localId));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.setting_category_bug_report_send_subject));

                if (sendIntent.resolveActivity(getActivity().getPackageManager()) != null)
                {
                    startActivity(sendIntent);
                }
                else
                {
                    Toast.makeText(getActivity(), getResources().getString(R.string.setting_category_bug_report_send_error), Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });

        /*
         * Share app
         */
        findPreference(getResources().getString(R.string.setting_category_share_app_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                try
                {
                    Intent intent = new AppInviteInvitation.IntentBuilder(getResources().getString(R.string.app_invite_title))
                            .setMessage(getResources().getString(R.string.app_invite_message))
                            .setDeepLink(Uri.parse(MainActivity.buildAppInvitesReferrerDeeplink(getActivity())))
                            .build();

                    ActivityCompat.startActivityForResult(getActivity(), intent, SettingsActivity.APP_INVITE_REQUEST, null);
                }
                catch (Exception e)
                {
                    Logger.error("An error occured during app invites activity start", e);
                }

                return false;
            }
        });

        /*
         * App version
         */
        final Preference appVersionPreference = findPreference(getResources().getString(R.string.setting_category_app_version_key));
        appVersionPreference.setTitle(getResources().getString(R.string.setting_category_app_version_title, BuildConfig.VERSION_NAME));
        appVersionPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://twitter.com/BenoitLetondor"));
                getActivity().startActivity(i);

                return false;
            }
        });

        /*
         * Currency change button
         */
        final Preference currencyPreference = findPreference(getResources().getString(R.string.setting_category_currency_change_button_key));
        currencyPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                selectCurrencyDialog = new SelectCurrencyFragment();
                selectCurrencyDialog.show(((SettingsActivity) getActivity()).getSupportFragmentManager(), "SelectCurrency");

                return false;
            }
        });
        setCurrencyPreferenceTitle(currencyPreference);

        /*
         * Warning limit button
         */
        final Preference limitWarningPreference = findPreference(getResources().getString(R.string.setting_category_limit_set_button_key));
        limitWarningPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
        {
            @Override
            public boolean onPreferenceClick(Preference preference)
            {
                View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_set_warning_limit, null);
                final EditText limitEditText = (EditText) dialogView.findViewById(R.id.warning_limit);
                limitEditText.setText(String.valueOf(Parameters.getInstance(getActivity()).getInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, EasyBudget.DEFAULT_LOW_MONEY_WARNING_AMOUNT)));
                limitEditText.setSelection(limitEditText.getText().length()); // Put focus at the end of the text

                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.adjust_limit_warning_title);
                builder.setMessage(R.string.adjust_limit_warning_message);
                builder.setView(dialogView);
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(final DialogInterface dialog, int which)
                    {
                        String limitString = limitEditText.getText().toString();
                        if( limitString.trim().isEmpty() )
                        {
                            limitString = "0"; // Set a 0 value if no value is provided (will lead to an error displayed to the user)
                        }

                        int newLimit = Integer.valueOf(limitString);

                        // Invalid value, alert the user
                        if (newLimit <= 0)
                        {
                            new AlertDialog.Builder(getActivity()).setTitle(R.string.adjust_limit_warning_error_title).setMessage(getResources().getString(R.string.adjust_limit_warning_error_message)).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    dialog.dismiss();
                                }

                            }).show();

                            return;
                        }

                        Parameters.getInstance(getActivity()).putInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, newLimit);
                        setLimitWarningPreferenceTitle(limitWarningPreference);
                    }
                });

                final Dialog dialog = builder.show();

                // Directly show keyboard when the dialog pops
                limitEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
                {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus)
                    {
                        if (hasFocus && getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) // Check if the device doesn't have a physical keyboard
                        {
                            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        }
                    }
                });

                return false;
            }
        });
        setLimitWarningPreferenceTitle(limitWarningPreference);

        /*
         * Premium status
         */
        refreshPremiumPreference();

        /*
         * Hide dev preferences if needed
         */
        PreferenceCategory devCategory = (PreferenceCategory) findPreference(getResources().getString(R.string.setting_category_dev_key));
        if( !BuildConfig.DEV_PREFERENCES )
        {
            getPreferenceScreen().removePreference(devCategory);
        }
        else
        {
            /*
             * Show welcome screen button
             */
            findPreference(getResources().getString(R.string.setting_category_show_welcome_screen_button_key)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(MainActivity.INTENT_SHOW_WELCOME_SCREEN));

                    getActivity().finish();
                    return false;
                }
            });

            /*
             * Enable animations pref
             */
            final CheckBoxPreference animationsPref = (CheckBoxPreference) findPreference(getResources().getString(R.string.setting_category_disable_animation_key));
            animationsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    UIHelper.setAnimationsEnabled(getActivity(), animationsPref.isChecked());
                    return true;
                }
            });
            animationsPref.setChecked(UIHelper.areAnimationsEnabled(getActivity()));
        }


        /*
         * Broadcast receiver
         */
        IntentFilter filter = new IntentFilter(SelectCurrencyFragment.CURRENCY_SELECTED_INTENT);
        receiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if( selectCurrencyDialog != null )
                {
                    setCurrencyPreferenceTitle(currencyPreference);

                    selectCurrencyDialog.dismiss();
                    selectCurrencyDialog = null;
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, filter);
    }

    /**
     * Set the currency preference title according to selected currency
     *
     * @param currencyPreference
     */
    private void setCurrencyPreferenceTitle(Preference currencyPreference)
    {
        currencyPreference.setTitle(getResources().getString(R.string.setting_category_currency_change_button_title, CurrencyHelper.getUserCurrency(getActivity()).getSymbol()));
    }

    /**
     * Set the limit warning preference title according to the selected limit
     *
     * @param limitWarningPreferenceTitle
     */
    private void setLimitWarningPreferenceTitle(Preference limitWarningPreferenceTitle)
    {
        limitWarningPreferenceTitle.setTitle(getResources().getString(R.string.setting_category_limit_set_button_title, CurrencyHelper.getFormattedCurrencyString(getActivity(), Parameters.getInstance(getActivity()).getInt(ParameterKeys.LOW_MONEY_WARNING_AMOUNT, EasyBudget.DEFAULT_LOW_MONEY_WARNING_AMOUNT))));
    }

    private void refreshPremiumPreference()
    {
        boolean isPremium = UserHelper.isUserPremium(getActivity());

        Preference premiumPref = findPreference(getResources().getString(R.string.setting_category_premium_status_key));
        if( isPremium )
        {
            premiumPref.setTitle(R.string.setting_category_premium_status_title);
            premiumPref.setSummary(R.string.setting_category_premium_status_message);

            premiumPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    return false;
                }
            });
        }
        else
        {
            premiumPref.setTitle(R.string.setting_category_premium_redeem_title);
            premiumPref.setSummary(null);

            premiumPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
            {
                @Override
                public boolean onPreferenceClick(Preference preference)
                {
                    View dialogView = getActivity().getLayoutInflater().inflate(R.layout.dialog_redeem_voucher, null);
                    final EditText voucherEditText = (EditText) dialogView.findViewById(R.id.voucher);

                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.voucher_redeem_dialog_title)
                        .setMessage(R.string.voucher_redeem_dialog_message)
                        .setView(dialogView)
                        .setPositiveButton(R.string.voucher_redeem_dialog_cta, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();

                                String promocode = voucherEditText.getText().toString();
                                if( promocode.trim().isEmpty() )
                                {
                                    new AlertDialog.Builder(getActivity())
                                        .setTitle(R.string.voucher_redeem_error_dialog_title)
                                        .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();

                                    return;
                                }

                                final ProgressDialog loadingDialog = ProgressDialog.show(getActivity(),
                                    getResources().getString(R.string.voucher_redeem_dialog_loading_title),
                                    getResources().getString(R.string.voucher_redeem_dialog_loading_message),
                                    true, false);

                                Batch.Unlock.redeemCode(promocode, new BatchCodeListener()
                                {
                                    @Override
                                    public void onRedeemCodeSuccess(String s, Offer offer)
                                    {
                                        loadingDialog.dismiss();

                                        if( offer.containsFeature(UserHelper.PREMIUM_FEATURE) )
                                        {
                                            Parameters.getInstance(getActivity()).putBoolean(ParameterKeys.BATCH_OFFER_REDEEMED, true);
                                            refreshPremiumPreference();
                                        }

                                        Map<String, String> additionalParameters = offer.getOfferAdditionalParameters();

                                        String rewardMessage = additionalParameters.get("reward_message");
                                        String rewardTitle = additionalParameters.get("reward_title");

                                        if (rewardTitle != null && rewardMessage != null)
                                        {
                                            new AlertDialog.Builder(getActivity())
                                                .setTitle(rewardTitle)
                                                .setMessage(rewardMessage)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .show();
                                        }
                                    }

                                    @Override
                                    public void onRedeemCodeFailed(String s, FailReason failReason, CodeErrorInfo codeErrorInfo)
                                    {
                                        loadingDialog.dismiss();

                                        if( failReason == FailReason.NETWORK_ERROR )
                                        {
                                            new AlertDialog.Builder(getActivity())
                                                .setTitle(R.string.voucher_redeem_error_dialog_title)
                                                .setMessage(R.string.voucher_redeem_error_no_internet_dialog_message)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .show();
                                        }
                                        else if( failReason == FailReason.INVALID_CODE )
                                        {
                                            new AlertDialog.Builder(getActivity())
                                                .setTitle(R.string.voucher_redeem_error_dialog_title)
                                                .setMessage(R.string.voucher_redeem_error_code_invalid_dialog_message)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .show();
                                        }
                                        else
                                        {
                                            new AlertDialog.Builder(getActivity())
                                                .setTitle(R.string.voucher_redeem_error_dialog_title)
                                                .setMessage(R.string.voucher_redeem_error_generic_dialog_message)
                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                                {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which)
                                                    {
                                                        dialog.dismiss();
                                                    }
                                                })
                                                .show();
                                        }
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                dialog.dismiss();
                            }
                        });

                    final Dialog dialog = builder.show();

                    // Directly show keyboard when the dialog pops
                    voucherEditText.setOnFocusChangeListener(new View.OnFocusChangeListener()
                    {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus)
                        {
                            if (hasFocus && getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS ) // Check if the device doesn't have a physical keyboard
                            {
                                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                            }
                        }
                    });

                    return false;
                }
            });
        }
    }

    @Override
    public void onDestroy()
    {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);

        super.onDestroy();
    }
}
