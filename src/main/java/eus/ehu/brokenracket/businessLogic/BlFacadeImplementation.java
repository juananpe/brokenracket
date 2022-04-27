package eus.ehu.brokenracket.businessLogic;

import eus.ehu.brokenracket.configuration.ConfigXML;
import eus.ehu.brokenracket.dataAccess.DataAccess;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Member;

import java.util.List;


/**
 * Implements the business logic as a web service.
 */
public class BlFacadeImplementation implements BlFacade {

	DataAccess dbManager;
	ConfigXML config = ConfigXML.getInstance();

	private static BlFacadeImplementation bl = new BlFacadeImplementation();

	public static BlFacadeImplementation getInstance(){
		return bl;
	}

	private BlFacadeImplementation()  {
		System.out.println("Creating BlFacadeImplementation instance");
		dbManager = new DataAccess();
		dbManager.close();
	}




	public void close() {
		dbManager.close();
	}

	/**
	 * This method invokes the data access to initialize the database with some events and questions.
	 * It is invoked only when the option "initialize" is declared in the tag dataBaseOpenMode of resources/config.xml file
	 */
	public void initializeBD(){
		dbManager.open(false);
		dbManager.initializeDB();
		dbManager.close();
	}

	@Override
	public void createInvoice(Member member, int month, int year) {
		float total = 0f;

		List<Booking> bookingList = member.getBookings(month);

		for(Booking b : bookingList){
			Booking.Status status = b.getStatus();
			if (status.equals(Booking.Status.OCCUPIED)){
				total += b.getTarif();
			}
		};

		dbManager.open(false);
		dbManager.createInvoice(member, total, month, year);
		dbManager.close();
	}
}
