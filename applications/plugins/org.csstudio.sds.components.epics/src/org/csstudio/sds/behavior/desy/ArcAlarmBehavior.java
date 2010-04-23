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

import org.csstudio.sds.components.model.ArcModel;
import org.csstudio.sds.components.model.EllipseModel;
import org.csstudio.sds.model.AbstractWidgetModel;
import org.epics.css.dal.context.ConnectionState;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.MetaData;
import org.epics.css.dal.simple.Severity;

/**
 *
 * Default DESY-Behavior for the {@link ArcModel} widget with Connection state and Alarms.
 *
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 20.04.2010
 */
public class ArcAlarmBehavior extends AbstractDesyAlarmBehavior<ArcModel> {

    private double _multi;

    /**
     * Constructor.
     */
    public ArcAlarmBehavior() {
        // add Invisible P0roperty Id here
        addInvisiblePropertyId(EllipseModel.PROP_ACTIONDATA);
        removeInvisiblePropertyId(EllipseModel.PROP_COLOR_BACKGROUND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(final ArcModel widget) {
        super.doInitialize(widget);
        widget.setPropertyValue(ArcModel.PROP_ANGLE, 360);
        widget.setPropertyValue(ArcModel.PROP_STARTANGLE, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doProcessValueChange(final ArcModel model, final AnyData anyData) {
//        super.doProcessValueChange(model, anyData);
        model.setPropertyValue(ArcModel.PROP_ANGLE, _multi * anyData.doubleValue());
        if(model.getFill()) {
            model.setPropertyValue(ArcModel.PROP_FILLCOLOR, determineColorBySeverity(anyData.getSeverity(),null));
        }else {
            model.setPropertyValue(ArcModel.PROP_COLOR_FOREGROUND, determineColorBySeverity(anyData.getSeverity(),null));
        }
        Severity severity = anyData.getSeverity();
        if (severity != null) {
            if (severity.isInvalid()) {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, true);
            } else {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, false);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doProcessConnectionStateChange(final ArcModel widget, final ConnectionState connectionState) {
        super.doProcessConnectionStateChange(widget, connectionState);
        if(connectionState != ConnectionState.CONNECTED) {
            widget.setColor(ArcModel.PROP_FILLCOLOR,determineBackgroundColor(connectionState));
        }
        if(!widget.getFill()) {
            widget.setPropertyValue(ArcModel.PROP_TRANSPARENT, connectionState == ConnectionState.CONNECTED);
        }
    }

    @Override
    protected void doProcessMetaDataChange(final ArcModel widget, final MetaData metaData) {
        if (metaData != null) {
            _multi = 360 / (metaData.getDisplayHigh() - metaData.getDisplayLow());
        }

        // do noting
    }
}
