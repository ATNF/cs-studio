package org.csstudio.nams.application.department.decision;

/**
 * Schluesel f�r die Property-Datei dieser Anwendung. Die Namen der Elemente ({@link Enum#name()})
 * sind die Schl�ssel der Eintr�ge.
 */
enum PropertiesFileKeys {
	/**
	 * Id des Properties-Dateinamen in den System-Properties der VM.
	 * 
	 * Ehemalig: "configFile"
	 */
	PROPERTY_KEY_CONFIG_FILE,

	/**
	 * Der Schl�ssel des Properties-Datei-Eintrages f�r die client id des
	 * jms message consumer(s).
	 * 
	 * Ehemalig: "CONSUMER_CLIENT_ID"
	 */
	PROPERTY_KEY_MESSAGING_CONSUMER_CLIENT_ID,

	/**
	 * Der Schl�ssel des Properties-Datei-Eintrages f�r den source name des
	 * jms message consumer(s).
	 * 
	 * Ehemalig: "CONSUMER_SOURCE_NAME"
	 */
	PROPERTY_KEY_MESSAGING_CONSUMER_SOURCE_NAME,

	/**
	 * Der Schl�ssel des Properties-Datei-Eintrages f�r die server urls f�r
	 * den jms message consumer.
	 * 
	 * Ehemalig: "CONSUMER_SERVER_URLS"
	 */
	PROPERTY_KEY_MESSAGING_CONSUMER_SERVER_URLS
}