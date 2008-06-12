package org.csstudio.nams.service.messaging.declaration;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.csstudio.nams.common.service.ExecutionService;
import org.csstudio.nams.common.service.StepByStepProcessor;
import org.csstudio.nams.common.service.ThreadType;
import org.csstudio.nams.service.logging.declaration.Logger;
import org.csstudio.nams.service.messaging.declaration.AbstractMultiConsumerMessageHandler.MultiConsumerMessageThreads;
import org.csstudio.nams.service.messaging.exceptions.MessagingException;

/**
 * Ein {@link Consumer} der auf mehreren {@link Consumer}n ließt.
 */
public class MultiConsumersConsumer implements Consumer {

	public static enum MultiConsumerConsumerThreads implements ThreadType {
		CONSUMER_THREAD
	};

	/**
	 * Queue zum zwischen speichern empfangener Nachrichten. BlockingQueue mit
	 * max groeße 1 damit keine Nachrichten auf Vorrat geholt werden.
	 */
	private final BlockingQueue<NAMSMessage> queue = new ArrayBlockingQueue<NAMSMessage>(
			1);
	private List<StepByStepProcessor> processors;
	private boolean isClosed = true;

	public MultiConsumersConsumer(final Logger logger, Consumer[] consumerArray,
			ExecutionService executionService) {
		processors = new LinkedList<StepByStepProcessor>();

		for (final Consumer consumer : consumerArray) {
			StepByStepProcessor stepByStepProcessor = new StepByStepProcessor() {
				@Override
				protected void doRunOneSingleStep() throws Throwable {
					try {
						NAMSMessage receivedMessage = consumer.receiveMessage();
						if (receivedMessage != null) {
							queue.put(receivedMessage);
						}
					} catch (MessagingException me) {
						if (me.getCause() instanceof InterruptedException) {
							// Ok, soll beendet werden....
						} else {
							// TODO Handling überlegen:
							// Sollen die Exceptions gespeichert und beim
							// "globalen" recieve zurückgeliefert werden??
							// throw me.fillInStackTrace();
							logger.logErrorMessage(this, "Exception during recieving message from: "+consumer.toString(), me);
						}
					}
				}
			};
			executionService.executeAsynchronsly(
					MultiConsumerMessageThreads.CONSUMER_THREAD,
					stepByStepProcessor);
			processors.add(stepByStepProcessor);
		}
		isClosed = false;
	}

	public void close() {
		for (StepByStepProcessor processor : processors) {
			processor.stopWorking();
		}
		isClosed = true;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public NAMSMessage receiveMessage() throws MessagingException,
			InterruptedException {
		return queue.take();
	}

}
