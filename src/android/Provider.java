package org.apache.cordova.Plugins;

import java.io.IOException;

import android.nfc.tech.IsoDep;
import android.util.Log;

import com.github.devnied.emvnfccard.exception.CommunicationException;
import com.github.devnied.emvnfccard.parser.IProvider;

public class Provider implements IProvider {
	
	
	private IsoDep mTagCom;
	
	@Override
	public byte[] transceive(byte[] arg0) throws CommunicationException {

		byte[] response = null;
		
		try{
			response = mTagCom.transceive(arg0);
			
			Log.d("Info", response.toString());
			
		}catch (IOException e) {
			throw new CommunicationException(e.getMessage());
		}
		
		return response;
	}
	
	
	public void setmTagCom(final IsoDep mTagCom) {
		this.mTagCom = mTagCom;
	}

}
