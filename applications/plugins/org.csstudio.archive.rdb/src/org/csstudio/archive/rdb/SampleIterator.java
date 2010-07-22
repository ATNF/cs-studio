package org.csstudio.archive.rdb;

import org.csstudio.platform.data.IValue;

/** Iterator over samples.
 *  <p>
 *  Works a bit like <code>Iterator&lt;IValue&gt;</code>,
 *  but in contrast to that interface this one passes
 *  Exceptions up to the caller.
 *  @author Kay Kasemir
 */
public interface SampleIterator
{
	/** @return <code>true</code> if there is another value */
	public boolean hasNext();
	
	/** Return the next sample.
	 *  <p>
	 *  Should only be called after <code>hasNext()</code>
	 *  indicated that there is in fact another sample.
	 */
	public IValue next() throws Exception;
}