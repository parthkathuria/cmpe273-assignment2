package edu.sjsu.cmpe.procurement.jobs;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.fusesource.stomp.jms.StompJmsConnectionFactory;
import org.fusesource.stomp.jms.StompJmsDestination;
import org.fusesource.stomp.jms.message.StompJmsMessage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import de.spinscale.dropwizard.jobs.Job;
import de.spinscale.dropwizard.jobs.annotations.Every;
import edu.sjsu.cmpe.procurement.ProcurementService;
import edu.sjsu.cmpe.procurement.domain.Book;
import edu.sjsu.cmpe.procurement.domain.BookOrder;
import edu.sjsu.cmpe.procurement.domain.ShippedBooks;

/**
 * This job will run at every 5 second.
 */
@Every("1mn")
public class ProcurementSchedulerJob extends Job {
	private final Logger log = LoggerFactory.getLogger(getClass());

	private ArrayList<Integer> isbns = new ArrayList<Integer>();
	private int numberOfMsgs = 0;

	public void incNumberofMsgs() {
		setNumberOfMsgs(getNumberOfMsgs() + 1);
	}

	@Override
	public void doJob() {
		try {
			pullFromQueue();
		} catch (JMSException e) {
			e.printStackTrace();
		}
		ShippedBooks shippedBooks = getOrderFromPublisher();

		for (int i = 0; i < shippedBooks.getShipped_books().size(); i++) {
			String category = shippedBooks.getShipped_books().get(i)
					.getCategory();
			try {
				publishNewBook(shippedBooks.getShipped_books().get(i), category);
			} catch (JMSException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		String strResponse = ProcurementService.jerseyClient.resource(
				"http://ip.jsontest.com/").get(String.class);
		log.debug("Response from jsontest.com: {}", strResponse);
	}

	private void publishNewBook(Book book, String category) throws JMSException {
		String user = env("APOLLO_USER", "admin");
		String password = env("APOLLO_PASSWORD", "password");
		String host = env("APOLLO_HOST", "54.215.133.131");
		int port = Integer.parseInt(env("APOLLO_PORT", "61613"));
		String destination = arg(0, "/topic/31112.book." + category);

		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + host + ":" + port);

		Connection connection = factory.createConnection(user, password);
		connection.start();
		Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);
		Destination dest = new StompJmsDestination(destination);
		MessageProducer producer = session.createProducer(dest);
		producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		String message = book.getIsbn() + ":" + book.getTitle() + ":"
				+ book.getCategory() + ":" + book.getCoverimage();
		TextMessage msg = session.createTextMessage(message);
		msg.setLongProperty("id", System.currentTimeMillis());
		producer.send(msg);

		System.out.println(msg.toString());
		connection.close();

	}

	private ShippedBooks getOrderFromPublisher() {

		WebResource webResource = ProcurementService.jerseyClient.resource("http://54.215.133.131:9000/orders/31112");
		ClientResponse response = webResource.accept("application/json").get(
				ClientResponse.class);
		ShippedBooks shippedBooks = response.getEntity(ShippedBooks.class);
		System.out.println("Response Status : " + response.getStatus());
		return shippedBooks;

	}

	private void pullFromQueue() throws JMSException {

		String user = env("APOLLO_USER", "admin");
		String password = env("APOLLO_PASSWORD", "password");
		String host = env("APOLLO_HOST", "54.215.133.131");
		int port = Integer.parseInt(env("APOLLO_PORT", "61613"));
		String queue = "/queue/31112.book.orders";
		String destination = arg(0, queue);

		StompJmsConnectionFactory factory = new StompJmsConnectionFactory();
		factory.setBrokerURI("tcp://" + host + ":" + port);

		Connection connection = factory.createConnection(user, password);
		connection.start();
		Session session = connection.createSession(false,
				Session.AUTO_ACKNOWLEDGE);
		Destination dest = new StompJmsDestination(destination);

		MessageConsumer consumer = session.createConsumer(dest);
		System.out.println("Waiting for messages from " + queue + "...");
		long waitUntil = 5000; // wait for 5 sec
		while (true) {
			Message msg = consumer.receive(waitUntil);
			if (msg instanceof TextMessage) {
				String body = ((TextMessage) msg).getText();
				if ("SHUTDOWN".equals(body)) {
					break;
				}
				System.out.println("Received message = " + body);
				addIsbn(Integer.parseInt(body.split(":")[1]));
				incNumberofMsgs();
			} else if (msg instanceof StompJmsMessage) {
				StompJmsMessage smsg = ((StompJmsMessage) msg);
				String body = smsg.getFrame().contentAsString();
				if ("SHUTDOWN".equals(body)) {
					break;
				}
				System.out.println("Received message = " + body);
				addIsbn(Integer.parseInt(body.split(":")[1]));
				incNumberofMsgs();
			} else if (msg == null) {
				System.out
						.println("No new messages. Existing due to timeout - "
								+ waitUntil / 1000 + " sec");
				if (getNumberOfMsgs() > 0) {
					submitBookOrder(isbns);
				}
				break;
			} else {
				System.out
						.println("Unexpected message type: " + msg.getClass());
			}
		} // end while loop
		connection.close();
		System.out.println("Done");

	}

	private void submitBookOrder(ArrayList<Integer> isbns) {

		BookOrder book = new BookOrder();
		book.setId("31112");
		book.setOrder_book_isbns(isbns);

		WebResource webResource = ProcurementService.jerseyClient.resource("http://54.215.133.131:9000/orders");
		ClientResponse response = webResource.type("application/json").post(
				ClientResponse.class, book);

		if (response.getStatus() == 200) {
			setNumberOfMsgs(0);
			removeIsbns(isbns);
			System.out.println(response.getStatus());
			System.out
					.println("{'msg':'Your order was successfully submitted.'}");
		} else {
			System.out.println("Post unsuccessfull");
		}

	}

	private static String env(String key, String defaultValue) {
		String rc = System.getenv(key);
		if (rc == null) {
			return defaultValue;
		}
		return rc;
	}

	private static String arg(int index, String defaultValue) {

		return defaultValue;
	}

	/**
	 * @return the isbns
	 */
	public ArrayList<Integer> getIsbns() {
		return isbns;
	}

	public void addIsbn(int isbn) {
		isbns.add(isbn);
	}

	public void removeIsbns(ArrayList<Integer> isbns) {
		for (int i = 0; i < isbns.size(); i++) {
			isbns.remove(i);
		}
	}

	/**
	 * @return the numberOfMsgs
	 */
	public int getNumberOfMsgs() {
		return numberOfMsgs;
	}

	/**
	 * @param numberOfMsgs
	 *            the numberOfMsgs to set
	 */
	public void setNumberOfMsgs(int numberOfMsgs) {
		this.numberOfMsgs = numberOfMsgs;
	}

}
