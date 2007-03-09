/*
 * Copyright (c) 2006 Stiftung Deutsches Elektronen-Synchroton,
 * Member of the Helmholtz Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS.
 * WITHOUT WARRANTY OF ANY KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR
 * THE USE OR OTHER DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE
 * IN ANY RESPECT, THE USER ASSUMES THE COST OF ANY NECESSARY SERVICING, REPAIR OR
 * CORRECTION. THIS DISCLAIMER OF WARRANTY CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE.
 * NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER EXCEPT UNDER THIS DISCLAIMER.
 * DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS,
 * OR MODIFICATIONS.
 * THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION, MODIFICATION,
 * USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY
 * AT HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */

package org.epics.css.dal.impl;

import org.epics.css.dal.DataAccess;
import org.epics.css.dal.DataExchangeException;
import org.epics.css.dal.EnumProperty;
import org.epics.css.dal.SequenceAccess;
import org.epics.css.dal.StringAccess;


public class StringDataAccessWrapper extends AbstractDataAccessWrapper<String>
	implements StringAccess
{
	private static final int ENUM_TO_STRING = 1;
	private static final int PATTERN_TO_STRING = 2;
	private static final int DOUBLE_TO_STRING = 3;
	private static final int LONG_TO_STRING = 4;
	private static final int OBJECT_TO_STRING = 5;
	private static final int OBJECTARRAY_TO_STRING = 6;

	public StringDataAccessWrapper(DataAccess sourceDA)
	{
		super(String.class, sourceDA);
	}

	protected int getConversion()
	{
		if (valClass.equals(String.class)) {
			if (sourceDA.getClass().isAssignableFrom(SequenceAccess.class)) {
				return OBJECTARRAY_TO_STRING;
			} else if (EnumProperty.class.isAssignableFrom(sourceDA.getClass())) {
				return ENUM_TO_STRING;
			} else if (sourceDA.getDataType().equals(Double.class)) {
				return DOUBLE_TO_STRING;
			} else if (sourceDA.getDataType().equals(Long.class)) {
				return LONG_TO_STRING;
			}
			/*            else if (PatternProperty.class.isAssignableFrom(sourceDA.getClass()))
			            {
			                return PATTERN_TO_STRING;
			            } */
			else {
				return OBJECT_TO_STRING;
			}
		}

		return UNKNOWN;
	}

	@Override
	protected Object convertFromOriginal(Object value, DataAccess dataAccess)
	{
		if (value == null) {
			return null;
		}

		switch (conversion) {
		case ENUM_TO_STRING: {
			String strVal = new String();
			try 
			{

			long index = ((Long)value).longValue();
			EnumProperty enumDA = (EnumProperty)dataAccess;
			int max = enumDA.getEnumDescriptions().length;

			if (index >= 0 && index < max) {
				strVal = enumDA.valueOf(index).toString();
				strVal = strVal.concat("("
					    + enumDA.getEnumDescriptions()[(int)index] + ")");
			}
			} catch (DataExchangeException ex)
			{
				ex.printStackTrace();
			}
			return strVal;
		}

		case OBJECTARRAY_TO_STRING: {
			int nItems;

			try {
				nItems = ((SequenceAccess)dataAccess).getSequenceLength();

				Object[] values = (Object[])value;
				String strValue = new String();

				for (int i = 0; i < nItems; i++) {
					strValue.concat(values[i].toString() + " ");
				}

				return strValue;
			} catch (DataExchangeException e) {
				e.printStackTrace();
			}

			break;
		}

		// TODO add bitset (pattern property)
		case LONG_TO_STRING:
		case DOUBLE_TO_STRING:
		case OBJECT_TO_STRING:return value.toString();
		}

		return null;
	}

	@Override
	protected Object convertToOriginal(Object value, DataAccess dataAccess)
	{
		if (value == null) {
			return null;
		}

		switch (conversion) {
		case DOUBLE_TO_STRING: {
			Double doubleVal = null;

			try {
				doubleVal = Double.parseDouble((String)value);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

			return doubleVal;
		}

		case LONG_TO_STRING: {
			Long longVal = null;

			try {
				longVal = Long.parseLong((String)value);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}

			return longVal;
		}
		}

		return null;
	}
}

/* __oOo__ */
