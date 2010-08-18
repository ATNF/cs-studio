/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron, Member of the Helmholtz
 * Association, (DESY), HAMBURG, GERMANY.
 *
 * THIS SOFTWARE IS PROVIDED UNDER THIS LICENSE ON AN "../AS IS" BASIS. WITHOUT WARRANTY OF ANY
 * KIND, EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE. SHOULD THE SOFTWARE PROVE DEFECTIVE IN ANY RESPECT, THE USER ASSUMES
 * THE COST OF ANY NECESSARY SERVICING, REPAIR OR CORRECTION. THIS DISCLAIMER OF WARRANTY
 * CONSTITUTES AN ESSENTIAL PART OF THIS LICENSE. NO USE OF ANY SOFTWARE IS AUTHORIZED HEREUNDER
 * EXCEPT UNDER THIS DISCLAIMER. DESY HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT, UPDATES,
 * ENHANCEMENTS, OR MODIFICATIONS. THE FULL LICENSE SPECIFYING FOR THE SOFTWARE THE REDISTRIBUTION,
 * MODIFICATION, USAGE AND OTHER RIGHTS AND OBLIGATIONS IS INCLUDED WITH THE DISTRIBUTION OF THIS
 * PROJECT IN THE FILE LICENSE.HTML. IF THE LICENSE IS NOT INCLUDED YOU MAY FIND A COPY AT
 * HTTP://WWW.DESY.DE/LEGAL/LICENSE.HTM
 */
package org.csstudio.sds.behavior.desy;

import org.csstudio.sds.components.model.BooleanSwitchModel;
import org.epics.css.dal.context.ConnectionState;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.MetaData;

public class BooleanSwitchBehavior extends
		AbstractDesyBehavior<BooleanSwitchModel> {

	@Override
	protected String[] doGetInvisiblePropertyIds() {
		return new String[] { BooleanSwitchModel.PROP_VALUE};
	}

	@Override
	protected void doInitialize(final BooleanSwitchModel widget) {
	}

	@Override
	protected void doProcessConnectionStateChange(final BooleanSwitchModel widget,
			final ConnectionState connectionState) {
	}

	@Override
	protected void doProcessMetaDataChange(final BooleanSwitchModel widget,
			final MetaData metaData) {
	}

	@Override
	protected void doProcessValueChange(final BooleanSwitchModel model,
			final AnyData anyData) {
		// .. value (influenced by current value, depending on onTrue Value)
		double value = anyData.doubleValue();
		boolean b = value == model
				.getDoubleProperty(BooleanSwitchModel.PROP_ON_STATE_VALUE);
		model.setPropertyValue(BooleanSwitchModel.PROP_VALUE, b);
	}

	@Override
	protected Object doConvertOutgoingValue(final BooleanSwitchModel widgetModel,
			final String propertyId, final Object value) {
		if (propertyId.equals(BooleanSwitchModel.PROP_VALUE)) {
			boolean currentValue = widgetModel
					.getBooleanProperty(BooleanSwitchModel.PROP_VALUE);
			double outgoingValue = widgetModel
					.getDoubleProperty(BooleanSwitchModel.PROP_OFF_STATE_VALUE);
			if (currentValue) {
				outgoingValue = widgetModel
						.getDoubleProperty(BooleanSwitchModel.PROP_ON_STATE_VALUE);
			}
			return outgoingValue;
		} else {
			return super.doConvertOutgoingValue(widgetModel, propertyId, value);
		}
	}

}
