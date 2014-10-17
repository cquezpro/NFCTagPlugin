package org.apache.cordova.Plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.*; // Cordova 3.x
import org.apache.cordova.api.*;  // Cordova 2.9
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.model.EmvCard;
import com.github.devnied.emvnfccard.parser.EmvParser;

public class NFCPlugin extends CordovaPlugin {
	
	//private NfcAdapter mNfcAdapter;
    EmvCard card;
    private IsoDep mTagcomm;
    Provider mProvider =  new Provider();
    private CallbackContext CBContext;
    
    
    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    
    private PendingIntent pendingIntent = null;
    private Intent savedIntent = null;
    private final List<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
    private final ArrayList<String[]> techLists = new ArrayList<String[]>();
    
    
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

    	
    	if (!getNfcStatus().equals(STATUS_NFC_OK)) {
            callbackContext.error(getNfcStatus());
            return true; // short circuit
        }

        createPendingIntent();
        
        if (action.equalsIgnoreCase("startNFC")) {
        	startNFC(callbackContext);

        }
        
    	return true;
    }
    
    private void startNFC(final CallbackContext callbackContext)
    {
    	getActivity().runOnUiThread(new Runnable() {
            public void run() {

            	CBContext = callbackContext;
            	 NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(getActivity());
            	if (nfcAdapter == null) {
            		callbackContext.error(STATUS_NO_NFC);
                } else{
                	Log.d("NFC Trigged: ", "request to start");
                	nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), getIntentFilters(), getTechLists());
                }
            }
            });
    }
    
    
    private String getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            return STATUS_NO_NFC;
        } else if (!nfcAdapter.isEnabled()) {
            return STATUS_NFC_DISABLED;
        } else {
            return STATUS_NFC_OK;
        }
    }
    
    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }
    
    private Activity getActivity() {
        return this.cordova.getActivity();
    }
    
    private Intent getIntent() {
        return getActivity().getIntent();
    }
    
    private void setIntent(Intent intent) {
        getActivity().setIntent(intent);
    }
    
    private PendingIntent getPendingIntent() {
        return pendingIntent;
    }
    
    private IntentFilter[] getIntentFilters() {
        return intentFilters.toArray(new IntentFilter[intentFilters.size()]);
    }

    private String[][] getTechLists() {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return techLists.toArray(new String[0][0]);
    }
    
    @Override
    public void onPause(boolean multitasking) {
        Log.d("NFCPlugin: ", "onPause " + getIntent());
        super.onPause(multitasking);
        
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
            	NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(getActivity());
            	if (nfcAdapter == null) {
            	nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    try {
                    	nfcAdapter.disableForegroundDispatch(getActivity());                        
                    } catch (IllegalStateException e) {
                        // issue 125 - user exits app with back button while nfc
                        Log.w("Just", "Illegal State Exception stopping NFC. Assuming application is terminating.");                        
                    }
                }
            }
        }
        });
    	
    }
    
    @Override
    public void onNewIntent(final Intent intent) {
        Log.d("NFCPlugin: ", "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
            	
            	NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(getActivity());
        final Tag mTag =  intent.getParcelableExtra(nfcAdapter.EXTRA_TAG);
        try {
			
        	if(mTag != null)
        	{
        		startScan(mTag);
        	}else{
        		CBContext.error("Communication failed!");
        	}
		} catch (IOException e) {
			CBContext.error("Communication failed!");
		}
            }
        });
    }
    
    @Override
    public void onResume(boolean multitasking) {
        Log.d("NFCPlugin: ", "onResume " + getIntent());
        super.onResume(multitasking);
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
            	NfcAdapter nfcAdapter =  NfcAdapter.getDefaultAdapter(getActivity());
            	if(nfcAdapter != null)
            	{
                	nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), getIntentFilters(), getTechLists());
                }
            }
        });
    }
    
    
    public void startScan(Tag mTag) throws IOException
	{
		
		mTagcomm = IsoDep.get(mTag);
		if(mTagcomm==null)
		{
			CBContext.error("Communication failed!");
		}else{

			mTagcomm.connect();
			
			//mProvider = new Provider();
			
			mProvider.setmTagCom(mTagcomm);
	        // Create parser
	        EmvParser parser = new EmvParser(mProvider, true);
	        // Read card
	         try {
				card = parser.readEmvCard();
				try {
					JSONObject json = new JSONObject();
					if(card.getAid()!=null)
					{
						json.put("aid", card.getAid());
					}
					if(card.getApplicationLabel() != null)
					{
						json.put("applicationLabel", card.getApplicationLabel());	
					}
					if(card.getAtrDescription() != null)
					{
						json.put("atrDescription", card.getAtrDescription());
					}
					if(card.getCardNumber() != null)
					{
						json.put("cardNumber", card.getCardNumber());
					}
					if(card.getExpireDate() != null)
					{
						json.put("expireDate", card.getExpireDate());
					}
					if(card.getHolderName() != null)
					{
						json.put("holderName", card.getHolderName());
					}
					if(card.getType() != null)
					{
						json.put("type", card.getType());
					}
					json.put("leftPinTry", card.getLeftPinTry());
					
					Log.d("Card Information", json.toString());
					
					CBContext.success(json);
					
				} catch (JSONException e) {
					CBContext.error("JSON Parse Error");
				}
			} catch (CommunicationException e) {
				CBContext.error("Communication failed!");
			}
	         Log.d("Card Information", card.toString());
		}
	}
	
}
