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
package org.csstudio.archive.common.service.mysqlimpl.types;

import java.util.Collection;
import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.csstudio.domain.desy.epics.types.EpicsEnumTriple;
import org.csstudio.domain.desy.typesupport.TypeSupportException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * TODO (bknerr) :
 *
 * @author bknerr
 * @since 15.12.2010
 */
public class EnumArchiveTypeConversionSupport extends ArchiveTypeConversionSupport<EpicsEnumTriple> {

    /**
     * Constructor.
     * @param type
     */
    EnumArchiveTypeConversionSupport() {
        super(EpicsEnumTriple.class);
    }

    /**
     * guava converter function
     *
     * @author bknerr
     * @since 22.12.2010
     */
    private final class ArchiveString2EpicsEnumFunction implements
            Function<String, EpicsEnumTriple> {
        /**
         * Constructor.
         */
        public ArchiveString2EpicsEnumFunction() {
            // Empty
        }

        @Override
          @CheckForNull
          public EpicsEnumTriple apply(@Nonnull final String from) {
              try {
                  return convertFromArchiveString(from);
              } catch (final TypeSupportException e) {
                  return null;
              }
          }
    }
    private final ArchiveString2EpicsEnumFunction _archiveString2EpicsEnumFunc = new ArchiveString2EpicsEnumFunction();

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public EpicsEnumTriple convertFromDouble(@Nonnull final Double value) throws TypeSupportException {
        throw new TypeSupportException("Enum shall not be converted from Double.", null);
    }
    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public String convertToArchiveString(@Nonnull final EpicsEnumTriple value) throws TypeSupportException {
        return tupleEmbrace(Joiner.on(ARCHIVE_TUPLE_SEP).join(value.getIndex(),
                                                              value.getState(),
                                                              value.getRaw() != null ? value.getRaw() :
                                                                                       ARCHIVE_NULL_ENTRY));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public EpicsEnumTriple convertFromArchiveString(@Nonnull final String value) throws TypeSupportException {
        final String released = tupleRelease(value);
        if (released == null) {
            throw new TypeSupportException("EpicsEnumTriple '" + value + "' is not embraced by proper pre- and suffixes.", null);
        }
        final Iterable<String> splitted = Splitter.on(ARCHIVE_TUPLE_SEP).split(released);
        if (Iterables.size(splitted) != 3) {
            throw new TypeSupportException("EpicsEnumTriple '" + value + "' from archive cannot be split into three elements.", null);
        }
        final Iterator<String> it = splitted.iterator();
        final String iStr = it.next();
        final String state = it.next();
        final String rawStr = it.next();

        try {
            final Integer i = Integer.parseInt(iStr);
            final Integer raw = ARCHIVE_NULL_ENTRY.equals(rawStr) ? null : Integer.parseInt(rawStr);
            return EpicsEnumTriple.createInstance(i, state,  raw);

        } catch (final NumberFormatException e) {
            throw new TypeSupportException("1st and 3rd part of EpicsEnumTriple '" + value + "' cannot be parsed into Integer.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Nonnull
    public Collection<EpicsEnumTriple> convertFromArchiveStringToMultiScalar(@Nonnull final String values) throws TypeSupportException {
        final String collectionRelease = collectionRelease(values);
        if (collectionRelease == null) {
            throw new TypeSupportException("Values from archive do not adhere to collection pattern:\n " +
                                           values, null);
        }
        final Iterable<String> strings = Splitter.on(ARCHIVE_COLLECTION_ELEM_SEP).split(collectionRelease);
        final Iterable<EpicsEnumTriple> enums =
            Iterables.filter(Iterables.transform(strings, _archiveString2EpicsEnumFunc),
                             Predicates.<EpicsEnumTriple>notNull());
        int size;
        try {
            size = Iterables.size(enums);
        } catch (final NumberFormatException e) {
            throw new TypeSupportException("Values representation is not convertible to " + EpicsEnumTriple.class.getName(), e);
        }
        if (Iterables.size(strings) != size) {
            throw new TypeSupportException("Number of values in string representation does not match the size of the result collection..", null);
        }
        return Lists.newArrayList(enums);
    }
}
