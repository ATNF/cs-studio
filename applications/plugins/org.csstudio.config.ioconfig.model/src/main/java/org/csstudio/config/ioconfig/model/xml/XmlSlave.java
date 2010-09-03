/*
 * Copyright (c) 2007 Stiftung Deutsches Elektronen-Synchrotron,
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
/*
 * $Id: XmlSlave.java,v 1.4 2010/09/03 07:13:20 hrickens Exp $
 */
package org.csstudio.config.ioconfig.model.xml;

import java.util.Comparator;
import java.util.TreeSet;

import org.csstudio.config.ioconfig.model.pbmodel.Module;
import org.csstudio.config.ioconfig.model.pbmodel.Slave;
import org.csstudio.config.ioconfig.model.pbmodel.SlaveCfgData;
import org.csstudio.config.ioconfig.model.pbmodel.gsdParser.GsdSlaveModel;
import org.jdom.Element;

/**
 * @author hrickens
 * @author $Author: hrickens $
 * @version $Revision: 1.4 $
 * @since 14.05.2008
 */
public class XmlSlave {

    /**
     * The Slave root {@link Element}.
     */
    private final Element _slaveElement;
    /**
     * Set of Profibus Modules.
     */
    private final TreeSet<Module> _modules;

    /**
     * Generate a GSD Configfile XML-Tag for a Profibus Slave.
     *
     * @param slave
     *            The Profibus slave.
     */
    public XmlSlave(final Slave slave) {
        _slaveElement = new Element("SLAVE");
        _slaveElement.setAttribute("fdl_add", Integer.toString(slave.getFdlAddress()));
        Comparator<Module> comparator = new Comparator<Module>() {

            public int compare(final Module o1, final Module o2) {
                return o1.getSortIndex() - o2.getSortIndex();
            }

        };
        _modules = new TreeSet<Module>(comparator);
        _modules.addAll(slave.getModules());
        Element e2 = setSlavePrmData(slave);
        Element e3 = setSlaveCfgData(slave);
        Element e4 = setSlaveAatData(slave);
        Element e5 = setSlaveUserData(slave);
        int prmLen = Integer.parseInt(e2.getAttributeValue("prm_data_len"));
        int cfgLen = Integer.parseInt(e3.getAttributeValue("cfg_data_len"));
        Element e1 = setSlaveParaSet(slave, prmLen, cfgLen, 2, 2);
        _slaveElement.addContent(e1);
        _slaveElement.addContent(e2);
        _slaveElement.addContent(e3);
        _slaveElement.addContent(e4);
        _slaveElement.addContent(e5);
    }

    /**
     * Set all XML slave_para_set parameter.
     *
     * @param slave
     *            The Profibus Slave.
     * @param prmDataLen
     *            The parameter data length.
     * @param cfgDataLen
     *            The Config data length.
     * @param slaveAatLen
     *            The Slave aat length.
     * @param slaveUserDataLen
     *            The user data length.
     * @return The XML Slave Para Set Element.
     */
    private Element setSlaveParaSet(final Slave slave, final int prmDataLen, final int cfgDataLen,
            final int slaveAatLen, final int slaveUserDataLen) {
        Element slaveParaSet = new Element("SLAVE_PARA_SET");
        int slaveParaLen = 16 + prmDataLen + cfgDataLen + slaveAatLen + slaveUserDataLen;
        slaveParaSet.setAttribute("slave_para_len", Integer.toString(slaveParaLen));
        slaveParaSet.setAttribute("sl_flag", Integer.toString(slave.getSlaveFlag()));
        slaveParaSet.setAttribute("slave_type", Integer.toString(slave.getSlaveType()));
        slaveParaSet.setAttribute("reserved", "0,0,0,0,0,0,0,0,0,0,0,0");
        return slaveParaSet;
    }

    /**
     * Set all slave_prm_data parameter.
     *
     * @param slave
     *            The Profibus Slave.
     * @return The XML Slave Prm Data Element.
     */
    public final Element setSlavePrmData(final Slave slave) {
        Element slavePrmData = new Element("SLAVE_PRM_DATA");
        GsdSlaveModel slaveData = slave.getGSDSlaveData();
        StringBuilder prmDataSB = new StringBuilder();
        if ((slave.getGSDSlaveData() != null)
                && (slave.getGSDSlaveData().getExtUserPrmDataConst() != null)) {

//            prmDataSB.append(slave.getGSDSlaveData().getModiExtUserPrmDataConst());
//            prmDataSB.append(slave.getGSDSlaveData().getUserPrmData());
            prmDataSB.append(slave.getPrmUserData());
            /*
            TreeMap<String, ExtUserPrmDataConst> extUserPrmDataConst = slave.getGSDSlaveData().getExtUserPrmDataConst();
            Set<String> keySet = extUserPrmDataConst.keySet();
            for (String key : keySet) {
                prmDataSB.append(extUserPrmDataConst.get(key).toString().replaceAll("[\\[\\]]", ""));
                prmDataSB.append(',');
            }
            if(prmDataSB.length()>0) {
                prmDataSB.deleteCharAt(prmDataSB.length()-1);
            }
            */

//            prmData = slave.getGSDSlaveData().getExtUserPrmDataConst().values().toString()
//                    .replaceAll("[\\[\\]]", "");

            // XXX: Die ExtUserPrmData von den Modulen geh�ren hier wohl nicht hin!
            for (Module module : _modules) {
                String modiExtUserPrmDataConst = module.getConfigurationData();
                String modiExtUserPrmDataConstDef = module.getGsdModuleModel()
                        .getModiExtUserPrmDataConst();
                if ((modiExtUserPrmDataConst == null) || (modiExtUserPrmDataConst.length() < 1)) {
                    // Do Nothing
                } else if ((modiExtUserPrmDataConstDef != null)
                        && (modiExtUserPrmDataConstDef.split(",").length
                           > modiExtUserPrmDataConst.split(",").length)) {
                    modiExtUserPrmDataConst = modiExtUserPrmDataConstDef;
                    prmDataSB.append(',');
                    prmDataSB.append(modiExtUserPrmDataConst);
                } else {
                    prmDataSB.append(',');
                    prmDataSB.append(modiExtUserPrmDataConst);
                }
            }
        }
        int prmDataLen = 9 + prmDataSB.toString().split(",").length;
        slavePrmData.setAttribute("prm_data_len", Integer.toString(prmDataLen));
        slavePrmData.setAttribute("station_status", Integer.toString(slave.getStationStatus()));
        slavePrmData.setAttribute("watchdog_fact_1", Integer.toString(slave.getWdFact1()));
        slavePrmData.setAttribute("watchdog_fact_2", Integer.toString(slave.getWdFact2()));
        slavePrmData.setAttribute("min_tsdr", Integer.toString(slave.getMinTsdr()));
        if (slaveData != null) {
            /*
             * we have some problems with work of the XML configuration. 1st. The parameter
             * baud_rate in the <MASTER> section must be write in decimal notation. Otherwise the
             * bus will start with a baud_rate = 9600 kbit/s.
             *
             * 2nd. The parameter ident_number in the <SLAVE> section must be write in hex notation.
             * Otherwise the Station will not work. You will get the error code 0x42 0x05 0x00.
             */
            slavePrmData.setAttribute("ident_number", "0x"
                    + Integer.toHexString(slaveData.getIdentNumber()));
        }
        slavePrmData.setAttribute("group_ident", Integer.toString(slave.getGroupIdent()));
        slavePrmData.setText(prmDataSB.toString());
        return slavePrmData;
    }

    /**
     * Set all slave_cfg_data parameter.
     *
     * @param slave
     *            The Profibus Slave.
     * @return The XML Slave Cfg Data Element.
     */
    public final Element setSlaveCfgData(final Slave slave) {
        Element slaveCfgData = new Element("SLAVE_CFG_DATA");
        String cfgData = "";
        for (Module module : _modules) {
            cfgData = cfgData.concat(module.getGsdModuleModel().getValue() + ",").trim();
        }
        if (cfgData.endsWith(",")) {
            cfgData = cfgData.substring(0, cfgData.length() - 1);
        }
        int cfgDataLen = cfgData.split(",").length + 2;
        slaveCfgData.setAttribute("cfg_data_len", Integer.toString(cfgDataLen));
        slaveCfgData.setText(cfgData);
        return slaveCfgData;
    }

    /**
     * Set all SlaveAatData parameter.
     *
     * @param slave
     *            The Profibus Slave.
     * @return The XML Slave aat Data Element.
     */
    public final Element setSlaveAatData(final Slave slave) {
        Element slaveAatData = new Element("SLAVE_AAT_DATA");
        String aat = "0,8";
        int offset = 0;
        for (Module module : _modules) {
            SlaveCfgData slaveCfgData = new SlaveCfgData(module.getGsdModuleModel().getValue());
            int leng = 0;
            if (slaveCfgData.isInput()) {
                leng = slaveCfgData.getWordSize() * slaveCfgData.getSize();
                aat = aat.concat(Integer.toString(leng));
            }
            if (slaveCfgData.isInput()) {
                leng += slaveCfgData.getWordSize() * slaveCfgData.getSize();
                aat = aat.concat(Integer.toString(leng));
            }
            offset += leng;
        }
        int slaveAatLen = 2;
        slaveAatData.setAttribute("slave_aat_len", Integer.toString(slaveAatLen));
        // Wird bei Desy MKs2 nicht verwendet.
        slaveAatData.setText("");
        return slaveAatData;
    }

    /**
     * Set all slave_user_data parameter.
     *
     * @param slave
     *            The Profibus Slave.
     * @return The XML Slave User Data Element.
     */
    public final Element setSlaveUserData(final Slave slave) {
        Element slaveUserData = new Element("SLAVE_USER_DATA");
        int slaveUserDataLen = 2;
        slaveUserData.setAttribute("slave_user_data_len", Integer.toString(slaveUserDataLen));
        // Wird bei Desy MKs2 nicht verwendet.
        slaveUserData.setText("");
        return slaveUserData;
    }

    /**
     *
     * @return the Slave {@link Element}
     */
    public final Element getSlave() {
        return _slaveElement;
    }

}
