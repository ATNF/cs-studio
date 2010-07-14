package org.csstudio.archive.channelarchiver;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClient;
import org.apache.xmlrpc.XmlRpcException;
import org.csstudio.archive.ArchiveAccessException;
import org.csstudio.archive.ArchiveInfo;

/** Handles the "archives" request and its results.
 *  @author Kay Kasemir
 */
public class ArchivesRequest
{
	private ArchiveInfo archive_infos[];

	/** Read info from data server
	 * @throws ArchiveAccessException */
	@SuppressWarnings({ "nls", "unchecked" })
    public void read(final XmlRpcClient xmlrpc) throws ArchiveAccessException
	{
		Vector<?> result;
		try
		{
			final Vector<Object> params = new Vector<Object>();
			result = (Vector<?>) xmlrpc.execute("archiver.archives", params);
		}
		catch (final XmlRpcException e)
		{
			throw new ArchiveAccessException("archiver.archives call failed", e);
		} catch (final IOException e) {
		    throw new ArchiveAccessException("archiver.archives call failed", e);
        }

		//	{  int32 key,
		//     string name,
		//     string path }[] = archiver.archives()
        archive_infos = new ArchiveInfo[result.size()];
		for (int i=0; i<result.size(); ++i)
		{
			final Hashtable<String,Object> info =
			    (Hashtable<String,Object>) result.get(i);
            archive_infos[i] =
                new org.csstudio.archive.channelarchiver.ArchiveInfoImpl(
				(Integer) info.get("key"),
				(String) info.get("name"),
				(String) info.get("path"));
		}
	}

	/** @return Returns all the archive infos obtained in the request. */
    public ArchiveInfo[] getArchiveInfos()
	{
		return archive_infos;
	}

	/** @return Returns a more or less useful string. */
	@SuppressWarnings("nls")
    @Override public String toString()
	{
		final StringBuffer result = new StringBuffer();
        for (final ArchiveInfo archiveInfo : archive_infos) {
            result.append(String.format("Key %4d: '%s' (%s)\n",
                archiveInfo.getKey(),
                archiveInfo.getName(),
                archiveInfo.getDescription()));
        }
		return result.toString();
	}
}
