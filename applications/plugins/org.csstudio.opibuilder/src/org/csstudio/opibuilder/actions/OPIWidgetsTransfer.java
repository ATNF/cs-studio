/*
 * Copyright (c) 2008 Stiftung Deutsches Elektronen-Synchrotron,
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

package org.csstudio.opibuilder.actions;

import java.util.List;

import org.csstudio.opibuilder.model.AbstractWidgetModel;
import org.csstudio.opibuilder.model.DisplayModel;
import org.csstudio.opibuilder.persistence.XMLUtil;
import org.csstudio.platform.logging.CentralLogger;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TransferData;

/**The transfer for clip board related actions.
 * @author Xihui Chen, Sven Wende (part of the code is copied from SDS)
 *
 */
public class OPIWidgetsTransfer extends ByteArrayTransfer {
	
	private static OPIWidgetsTransfer instance;
	
	private static final String TYPE_NAME = "OPIWidgetsTransfer:"
			+ System.currentTimeMillis();
	
	private static final int TYPEID = registerType(TYPE_NAME);
	
	
	public synchronized static OPIWidgetsTransfer getInstance() {
		if(instance == null)
			instance = new OPIWidgetsTransfer();
		return instance;
	}

	@Override
	protected int[] getTypeIds() {
		return new int[] {TYPEID};
	}

	@Override
	protected String[] getTypeNames() {
		return new String[] {TYPE_NAME};
	}
	
	@Override
	protected void javaToNative(Object object, TransferData transferData) {
		if (!isSupportedType(transferData) || !(checkInput(object))) {
			DND.error(DND.ERROR_INVALID_DATA);
		}
		super.javaToNative(((String)object).getBytes(), transferData);
	}
	
	@Override
	protected Object nativeToJava(TransferData transferData) {
		if(!isSupportedType(transferData))
			return null;
		byte[] bytes = (byte[])super.nativeToJava(transferData);
		if(bytes == null)
			return null;
		try {
			DisplayModel displayModel = (DisplayModel) XMLUtil.XMLStringToWidget(new String(bytes));
			List<AbstractWidgetModel> widgets = displayModel.getChildren();
			return widgets;
		} catch (Exception e) {
			CentralLogger.getInstance().error(this, "Failed to transfer XML to widget", e);
		}
		return null;
		
	}
	
	/**
	 * Checks the provided input, which must be a non-empty list that contains
	 * only objects of type {@link AbstractWidgetModel}.
	 * 
	 * @param input
	 *            the input to check
	 * @return true, if the input object is valid, false otherwise
	 */
	private boolean checkInput(final Object input) {
		
		if(input == null)
			return false;
		return input instanceof String;
		
	}

}
