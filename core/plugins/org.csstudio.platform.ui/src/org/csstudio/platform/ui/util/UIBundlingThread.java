package org.csstudio.platform.ui.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.csstudio.platform.ExecutionService;
import org.csstudio.platform.ui.CSSPlatformUiPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;



/**
 * 
 * A singleton back thread which will help to execute tasks in UI thread.
 * This way we avoid slow downs, that occur on several
 * operating systems, when Display.asyncExec() is called very often from
 * background threads.
 * 
 * This thread sleeps for a time, which is below the processing capacity of
 * human eyes and brain - so the user will not feel any delay.
 * 
 * @author Sven Wende, Xihui Chen
 * 
 */
public final class UIBundlingThread implements Runnable {
	/**
	 * The singleton instance.
	 */
	private static UIBundlingThread instance;

	/**
	 * A queue, which contains runnables that process the events that occured
	 * during the last SLEEP_TIME milliseconds.
	 */
	private Queue<Runnable> tasksQueue;

	/**
	 * Standard constructor.
	 */
	private UIBundlingThread() {
		tasksQueue = new ConcurrentLinkedQueue<Runnable>();

		ExecutionService.getInstance().getScheduledExecutorService()
				.scheduleAtFixedRate(this, 100, 20, TimeUnit.MILLISECONDS);
	}

	/**
	 * Gets the singleton instance.
	 * 
	 * @return the singleton instance
	 */
	public static synchronized UIBundlingThread getInstance() {
		if (instance == null) {
			instance = new UIBundlingThread();
		}

		return instance;
	}

	/**
	 * {@inheritDoc}.
	 */
	public void run() {
		if(!tasksQueue.isEmpty())
			processQueue();
	}

	/**
	 * Process the complete queue.
	 */
	private synchronized void processQueue() {
		Display display;
		if(CSSPlatformUiPlugin.getDefault() != null && PlatformUI.getWorkbench() != null)
			display = PlatformUI.getWorkbench().getDisplay();
		else
			display = Display.getDefault();
		Runnable r;
		while( (r=tasksQueue.poll()) != null){	
			display.asyncExec(r);
		}
	
	}

	/**
	 * Adds the specified runnable to the queue.
	 * 
	 * @param runnable
	 *            the runnable
	 */
	public synchronized void addRunnable(final Runnable runnable) {
		tasksQueue.add(runnable);
	}

}
