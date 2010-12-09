/**
 * 
 */
package org.csstudio.utility.channel.actions;

import static gov.bnl.channelfinder.api.Tag.Builder.tag;
import static org.csstudio.utility.channel.CSSChannelUtils.*;
import gov.bnl.channelfinder.api.ChannelFinderClient;
import gov.bnl.channelfinder.api.ChannelFinderException;

import java.util.Collection;
import java.util.Iterator;

import org.csstudio.utility.channel.ICSSChannel;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
/**
 * @author shroffk
 *
 */
public class RemoveTagsJob extends Job {

	private Collection<ICSSChannel> channels;
	private Collection<String> selectedTags;
	
	public RemoveTagsJob(String name, Collection<ICSSChannel> channels,
			Collection<String> selectedTags) {
		super(name);
		this.channels = channels;
		this.selectedTags = selectedTags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask("Removing Tags from channels", IProgressMonitor.UNKNOWN);
		try {
			for (Iterator<String> iterator = selectedTags.iterator(); iterator.hasNext();) {
				String tagName = iterator.next();
				monitor.subTask("Removing tag "+tagName);
				ChannelFinderClient.getInstance().remove(tag(tagName), getCSSChannelNames(channels) );
				monitor.worked(1);
			}
		} catch (ChannelFinderException e) {
			e.printStackTrace();
		}
		monitor.done();
        return Status.OK_STATUS;
	}

}
