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
package org.csstudio.sds.internal.model;

import org.csstudio.sds.model.AbstractWidgetModel;
import org.csstudio.sds.model.PropertyTypesEnum;
import org.csstudio.sds.model.WidgetPropertyCategory;
import org.csstudio.sds.util.ChannelReferenceValidationException;
import org.csstudio.sds.util.ChannelReferenceValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A property, which is able to handle String values.
 *
 * @author Kai Meyer
 * @version $Revision: 1.2 $
 *
 */
public final class TooltipProperty extends StringProperty {
    private static final Logger LOG = LoggerFactory.getLogger(TooltipProperty.class);

    private AbstractWidgetModel widgetModel;
    public TooltipProperty(String description, WidgetPropertyCategory category, String defaultValue, AbstractWidgetModel widgetModel) {
        super(PropertyTypesEnum.PARAMSTRING, description, category, defaultValue);
        this.widgetModel = widgetModel;
    }

    @Override
    public String getTextForTooltip() {
        String v = getPropertyValue();
        String tooltip = "-";

        if (v != null && v.length() > 0) {
            try {
                tooltip = ChannelReferenceValidationUtil.createCanonicalName(v, widgetModel.getAllInheritedAliases());
            } catch (ChannelReferenceValidationException e) {
                LOG.debug("Aliases in \"" + v + "\" cannot be resolved, error message is: " + e.getMessage());
                tooltip = v + "(not resolvable)";
            }
        }

        return tooltip;
    }
}
