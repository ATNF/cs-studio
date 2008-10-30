package org.csstudio.utility.ldapUpdater;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.sun.org.apache.xerces.internal.impl.xs.SubstitutionGroupHandler;


public class myDateTimeString {
			  
	/**
	 * Konversion von Millisekunden-Wert nach String
	 * 		params in : Date_Format, Time_Format, Millisekunden
	 * 		params out: String, der Datum und / oder Uhrzeit enth�lt
	 * (dateform und / oder timeform d�rfen leere Strings sein, aber beide Strings leer zu �bergeben macht 
	 * nicht viel Sinn, denn wer will schon einen leeren String zur�ckgegeben haben !)

	 * sample call :
	 * String ymd_hms = getDateTimeString( "yyyy-MM-dd", "HH:mm:ss", now);	
	 * 
	 * @author valett
	 *
	 */
		  public String getDateTimeString(final String dateform, final String timeform, final long millis) {
			  String result = "";
		    	    
			  if ( dateform.length() != 0 ) {
				  SimpleDateFormat sdfDate = new SimpleDateFormat(dateform);
				  String strDate = sdfDate.format(millis);
				  result = strDate;
			  } 
		    
			  if ( timeform.length() != 0 ) {
				  SimpleDateFormat sdfTime = new SimpleDateFormat(timeform);
				  String strTime = sdfTime.format(millis);
				  if ( result.length() != 0 ){
					  result = result + " " + strTime ;
				  } else {
					  result = strTime;
				  }
			  }

			  return ( result );
		  }
}
