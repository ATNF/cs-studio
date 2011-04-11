/*
 * Copyright (c) 2011 Stiftung Deutsches Elektronen-Synchrotron,
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
 *
 * $Id: DesyKrykCodeTemplates.xml,v 1.7 2010/04/20 11:43:22 bknerr Exp $
 */
package org.csstudio.config.ioconfig.model.pbmodel.gsdParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * TODO (hrickens) : 
 * 
 * @author hrickens
 * @author $Author: bknerr $
 * @version $Revision: 1.7 $
 * @since 30.03.2011
 */
public abstract class AbstractGsdPropertyModel {

    private final Map<String, String> _stringValueMap;
    private final Map<String, Integer> _intergerValueMap;
    private final Map<String, List<Integer>> _intArrayValueMap;

    /**
     * Constructor.
     */
    public AbstractGsdPropertyModel() {
        _stringValueMap = new HashMap<String, String>();
        _intergerValueMap = new HashMap<String, Integer>();
        _intArrayValueMap = new HashMap<String, List<Integer>>();
    }
    
    private void setIntArrayValue(@Nonnull KeyValuePair keyValuePair) {
        List<Integer> valueList = new ArrayList<Integer>();
        GsdFileParser.addValues2IntList(keyValuePair.getValue(), valueList);
        _intArrayValueMap.put(keyValuePair.getKey(), valueList);
    }

    private void setIntegerValue(@Nonnull KeyValuePair keyValuePair) {
        Integer inValue = GsdFileParser.gsdValue2Int(keyValuePair.getValue());
        _intergerValueMap.put(keyValuePair.getKey(), inValue);
    }

    /**
     * Sets the property according to their type. (type-safe)
     */
    public void setProperty(@Nonnull KeyValuePair keyValuePair) {
        String value = keyValuePair.getValue();
        if(value.startsWith("\"")) {
            setStringValue(keyValuePair);
        } else if(value.contains(",")) {
            setIntArrayValue(keyValuePair);
        } else {
            setIntegerValue(keyValuePair);
        }
    }

    private void setStringValue(@Nonnull KeyValuePair keyValuePair) {
        _stringValueMap.put(keyValuePair.getKey(), keyValuePair.getValue());
    }

    @CheckForNull
    protected List<Integer> getIntListValue(@Nonnull String propertty) {
        return _intArrayValueMap.get(propertty);
    }

    @CheckForNull
    protected String getStringValue(@Nonnull String propertty) {
        return _stringValueMap.get(propertty);
    }

    @CheckForNull
    protected Integer getIntValue(@Nonnull String propertty) {
        return _intergerValueMap.get(propertty);
    }
    
}
