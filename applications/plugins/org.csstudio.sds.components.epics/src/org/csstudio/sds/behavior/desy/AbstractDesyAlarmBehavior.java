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

import org.csstudio.sds.model.AbstractWidgetModel;
import org.epics.css.dal.context.ConnectionState;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.Severity;

/**
 * TODO (hrickens) :
 *
 * @author hrickens
 * @author $Author$
 * @version $Revision$
 * @since 19.04.2010
 *
 * @param <W>
 *            The Widget model that have this Behavior
 */
public abstract class AbstractDesyAlarmBehavior<W extends AbstractWidgetModel> extends
        AbstractDesyConnectionBehavior<W> {

    private String _defColor;

    /**
     * Constructor.
     */
    public AbstractDesyAlarmBehavior() {
        addInvisiblePropertyId(AbstractWidgetModel.PROP_BORDER_COLOR);
        addInvisiblePropertyId(AbstractWidgetModel.PROP_BORDER_STYLE);
        addInvisiblePropertyId(AbstractWidgetModel.PROP_BORDER_WIDTH);
    }

    protected void doInitialize(final W widget) {
        super.doInitialize(widget);
//        _defColor = widget.getColor(AbstractWidgetModel.PROP_BORDER_COLOR);
    };

    @Override
    protected void doProcessConnectionStateChange(final W widget,
                                                  final ConnectionState connectionState) {
        super.doProcessConnectionStateChange(widget, connectionState);
//        widget.setPropertyValue(LabelModel.PROP_COLOR_BACKGROUND,determineBackgroundColor(connectionState));
//        if ( (connectionState != null) && (connectionState == ConnectionState.CONNECTED)) {
//            widget.setPropertyValue(LabelModel.PROP_COLOR_BACKGROUND, _normalBackgroundColor);
//        } else {
//            widget.setPropertyValue(LabelModel.PROP_COLOR_BACKGROUND,
//                                    determineBackgroundColor(connectionState));
//        }
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    protected void doProcessValueChange(final W model, final AnyData anyData) {
        Severity severity = anyData.getSeverity();
        if (severity != null) {
            if (severity.isInvalid()) {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, true);
            } else {
                model.setPropertyValue(AbstractWidgetModel.PROP_CROSSED_OUT, false);
            }
            model.setPropertyValue(AbstractWidgetModel.PROP_BORDER_COLOR, determineColorBySeverity(severity, _defColor));
            model.setPropertyValue(AbstractWidgetModel.PROP_BORDER_STYLE, SeverityUtil
                                   .determineBorderStyleBySeverity(severity).getIndex());
            model.setPropertyValue(AbstractWidgetModel.PROP_BORDER_WIDTH, SeverityUtil
                                   .determineBorderWidthBySeverity(severity));
        }
    };
}
