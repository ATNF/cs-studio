package org.csstudio.config.ioconfig.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.csstudio.config.ioconfig.model.pbmodel.ChannelDBO;
import org.csstudio.config.ioconfig.model.pbmodel.GSDFileDBO;
import org.csstudio.config.ioconfig.model.pbmodel.GSDModuleDBO;

public class DummyRepository implements IRepository {

    private int id = 1;

    @Override
    public String getEpicsAddressString(final String ioName) {
        return null;
    }

    @Override
    public List<String> getIoNames() {
        return null;
    }

    @Override
    public List<String> getIoNames(final String iocName) {
        return null;
    }

    @Override
    public <T> List<T> load(final Class<T> clazz) {
        return null;
    }

    @Override
    public List<DocumentDBO> loadDocument() {
        return null;
    }

    @Override
    public void removeGSDFiles(final GSDFileDBO gsdFile) {

    }

    @Override
    public <T extends DBClass> void removeNode(final T dbClass) {

    }

    @Override
    public GSDFileDBO save(final GSDFileDBO gsdFile) {
        return null;
    }

    @Override
    public <T extends DBClass> T saveOrUpdate(final T dbClass) throws PersistenceException {
        dbClass.setId(id++);
        return null;
    }

    public GSDModuleDBO saveWithChildren(final GSDModuleDBO gsdModule) throws PersistenceException {
        return null;
    }

    @Override
    public <T extends DBClass> T update(final T dbClass) {
        return null;
    }

    @Override
    public DocumentDBO update(final DocumentDBO document) {
        return null;
    }

    @Override
    public <T> T load(final Class<T> clazz, final Serializable id) {
        return null;
    }

    @Override
    public List<SensorsDBO> loadSensors(final String ioName) {
        return null;
    }

    @Override
    public SensorsDBO loadSensor(final String ioName, final String selection) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean search(final Class class1, final int id) {
        return false;
    }

    public List<Integer> getRootPath(final int id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DocumentDBO save(final DocumentDBO document) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getShortChannelDesc(final String ioName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ChannelDBO loadChannel(final String ioName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

    @Override
    public List<PV2IONameMatcherModelDBO> loadPV2IONameMatcher(final Collection<String> pvName) {
        // TODO Auto-generated method stub
        return null;
    }

}
