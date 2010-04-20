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

import org.csstudio.sds.components.model.BargraphModel;
import org.csstudio.sds.components.model.SimpleSliderModel;
import org.csstudio.sds.model.AbstractWidgetModel;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.MetaData;
import org.epics.css.dal.simple.Severity;

/**
 *
 * Default DESY-Behavior for the {@link SimpleSliderModel} widget with Connection state and Alarms.
 *
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 20.04.2010
 */
public class SimpleSliderAlarmBehavior extends AbstractDesyAlarmBehavior<SimpleSliderModel> {

    private String _defColor;

    /**
     * Constructor.
     */
    public SimpleSliderAlarmBehavior() {
        addInvisiblePropertyId(SimpleSliderModel.PROP_VALUE);
        addInvisiblePropertyId(SimpleSliderModel.PROP_MAX);
        addInvisiblePropertyId(SimpleSliderModel.PROP_MIN);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(final SimpleSliderModel widget) {
        super.doInitialize(widget);
        _defColor = widget.getColor(BargraphModel.PROP_COLOR_FOREGROUND);
    }

    @Override
    protected void doProcessValueChange(final SimpleSliderModel model, final AnyData anyData) {
        // .. update slider value
        model.setPropertyValue(SimpleSliderModel.PROP_VALUE, anyData.doubleValue());
        Severity severity = anyData.getSeverity();
        if (severity != null) {
            if (severity.isInvalid()) {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, true);
            } else {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, false);
            }
            model.setPropertyValue(SimpleSliderModel.PROP_COLOR_FOREGROUND,
                                   determineColorBySeverity(severity, _defColor));
            model.setPropertyValue(SimpleSliderModel.PROP_COLOR_BACKGROUND,
                                   determineColorBySeverity(severity, _defColor));
        }
    }

    @Override
    protected void doProcessMetaDataChange(final SimpleSliderModel widget, final MetaData meta) {
        if (meta != null) {
            // .. update min / max
            widget.setPropertyValue(SimpleSliderModel.PROP_MAX, meta.getDisplayHigh());
            widget.setPropertyValue(SimpleSliderModel.PROP_MIN, meta.getDisplayLow());
        }
    }

    @Override
    protected String[] doGetSettablePropertyIds() {
        return new String[] { SimpleSliderModel.PROP_VALUE };
    }
}
