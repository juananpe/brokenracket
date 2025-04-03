package eus.ehu.brokenracket.businessLogic;

import eus.ehu.brokenracket.dataAccess.DataAccess;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;

import java.util.Date;
import java.util.List;
import java.util.Calendar;

/**
 * Implements the business logic as a web service.
 */
public class BlFacadeImplementation implements BlFacade {

	DataAccess dbManager;

	private static BlFacadeImplementation bl = new BlFacadeImplementation();

	public static BlFacadeImplementation getInstance(){
		return bl;
	}

	private BlFacadeImplementation()  {
		System.out.println("Creating BlFacadeImplementation instance");
		dbManager = new DataAccess();
		// dbManager.close(); // Closing immediately after opening might not be intended here
	}




	public void close() {
		dbManager.close();
	}

	/**
	 * This method invokes the data access to initialize the database with some events and questions.
	 * It is invoked only when the option "initialize" is declared in the tag dataBaseOpenMode of resources/config.xml file
	 */
	public void initializeBD(){
		dbManager.initializeDB();
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

		dbManager.createInvoice(member, total, month, year);
	}

	@Override
	public List<Court> getCourts() {
		List<Court> result = dbManager.getCourts();

		return result;
	}

	@Override
	public List<Booking> getFreeBooks(Court court, Date bookDate) {
		System.out.println("[Facade] Getting free books for Court #: " + court.getNumber() + " on Date: " + bookDate);

		// We should Validate that booking date is within a month from current date
		// This would ensure that the validation happens even if someone tries to bypass the UI validation

		// Simplified approach: directly get free bookings from the database
		List<Booking> freeBookings = dbManager.getFreeBookingsByCourtAndDate(court, bookDate);
		
		System.out.println("[Facade] Found " + freeBookings.size() + " free bookings directly from DB.");
		
		// Log the details of free bookings found
		if (!freeBookings.isEmpty()) {
			System.out.println("[Facade] === FREE BOOKINGS DETAILS ===");
			for (Booking b : freeBookings) {
				System.out.println("  Free Booking ID: " + b.getId() + 
						   ", Date: " + b.getDate() + 
						   ", Hour: " + b.getStartingHour() + 
						   ", Status: " + b.getStatus());
			}
			System.out.println("[Facade] ============================");
		}

		return freeBookings;
	}

	@Override
	public void setBook(String name, Booking book) {
		// Don't open/close DB here to avoid premature closure
		// Connection is managed by DataAccess
		dbManager.setBook(name, book);
		
		// Log the booking for debugging
		System.out.println("[Facade] Booking set for user " + name + " at hour " + book.getStartingHour());
	}
}
