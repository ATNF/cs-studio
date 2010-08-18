/*
 * Copyright (c) 2010 Stiftung Deutsches Elektronen-Synchrotron,
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
package org.csstudio.sds.behavior.desy;

import org.csstudio.sds.model.LabelModel;
import org.csstudio.sds.model.TextTypeEnum;
import org.epics.css.dal.simple.AnyData;
import org.epics.css.dal.simple.MetaData;

/**
 * Default DESY-Behavior for the {@link LabelModel} widget with Connection state
 *
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.7 $
 * @since 19.04.2010
 */
public class LabeConnectionBehavior extends AbstractDesyConnectionBehavior<LabelModel> {


    private boolean _showUnits;

    /**
     * Constructor.
     */
    public LabeConnectionBehavior() {
        addInvisiblePropertyId(LabelModel.PROP_TEXTVALUE);
        addInvisiblePropertyId(LabelModel.PROP_TEXT_UNIT);
        addInvisiblePropertyId(LabelModel.PROP_PERMISSSION_ID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInitialize(final LabelModel widget) {
        super.doInitialize(widget);
        _showUnits=!widget.getValueType().equals(TextTypeEnum.TEXT);
        if(!_showUnits) {
            widget.setJavaType(String.class);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doProcessValueChange(final LabelModel model, final AnyData anyData) {
        super.doProcessValueChange(model, anyData);
        // .. fill level (influenced by current value)
        model.setPropertyValue(LabelModel.PROP_TEXTVALUE, anyData.stringValue());

    }

    @Override
    protected void doProcessMetaDataChange(final LabelModel model, final MetaData metaData) {
        if(_showUnits) {
            model.setPropertyValue(LabelModel.PROP_TEXT_UNIT, metaData.getUnits());
        }
    }

}
