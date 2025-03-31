package eus.ehu.brokenracket.businessLogic;

import eus.ehu.brokenracket.dataAccess.DataAccess;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


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

	@Override
	public List<Court> getCourts() {
		dbManager.open(false);
		List<Court> result = dbManager.getCourts();
		dbManager.close();

		return result;
	}

	@Override
	public List<Booking> getFreeBooks(Court court, Date bookDate) {
		System.out.println("[Facade] Getting free books for Court #: " + court.getNumber() + " on Date: " + bookDate);

		// 1. Define possible booking hours (e.g., 9 AM to 5 PM)
		final int START_HOUR = 9;
		final int END_HOUR = 17; // Includes 17:00

		// 2. Get actual bookings for this court and date from the database
		// No need to open/close DB connection - handled inside DataAccess
		List<Booking> actualBookings = dbManager.getBookingsByCourtAndDate(court, bookDate);
		
		System.out.println("[Facade] Found " + actualBookings.size() + " actual bookings in DB for this court/date.");
		// Log the details of actual bookings found
		System.out.println("[Facade] === DETAILED BOOKING INFO ===");
		for (Booking b : actualBookings) {
			System.out.println("  Booking ID: " + b.getId() + 
					   ", Date: " + b.getDate() + 
					   ", Hour: " + b.getStartingHour() + 
					   ", Status: " + b.getStatus() + 
					   ", Member: " + (b.getMember() != null ? b.getMember().getName() : "null"));
		}
		System.out.println("[Facade] ==========================");

		// 3. Create a set of booked hours (only include if status is OCCUPIED)
		Set<Integer> bookedHours = actualBookings.stream()
			.filter(b -> {
				boolean isOccupied = b.getStatus() == Booking.Status.OCCUPIED;
				System.out.println("[Facade] Filtering booking ID " + b.getId() + ": Status=" + b.getStatus() + ", IsOccupied=" + isOccupied);
				return isOccupied;
			})
			.map(Booking::getStartingHour)
			.collect(Collectors.toSet());

		System.out.println("[Facade] Hours booked (Occupied): " + bookedHours);

		// 4. Generate the list of free slots
		List<Booking> freeSlots = new ArrayList<>();
		for (int hour = START_HOUR; hour <= END_HOUR; hour++) {
			if (!bookedHours.contains(hour)) {
				// This hour is free, create a representative Booking object using the constructor
				// Pass null for Member to indicate a free slot
				Booking freeSlot = new Booking(bookDate, hour, court, null);
				// The constructor already sets Status to FREE and calculates Rate
				freeSlots.add(freeSlot);
			}
		}

		System.out.println("[Facade] Calculated " + freeSlots.size() + " free slots.");
		return freeSlots;
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
