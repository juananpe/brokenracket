package eus.ehu.brokenracket.domain;

import eus.ehu.brokenracket.configuration.UtilDate;
import eus.ehu.brokenracket.dataAccess.DataAccess;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
public class Court {
  @Id
  private Integer id;

  @OneToMany( cascade = CascadeType.PERSIST, mappedBy = "court")
  private List<Booking> bookings = new ArrayList<>();

  public Court(Integer id) {
    this.id = id;
  }

  public Court() {

  }

  public void addBooking(Booking booking){
    bookings.add(booking);
  }

  // Method to clear the bookings collection (e.g., for DB initialization)
  public void clearBookings() {
    this.bookings.clear();
  }

  public List<Booking> getFreeBooks(Date bookDate) {
    List<Booking> res = new ArrayList<>();
    System.out.println("[Court] getFreeBooks called for Date: " + bookDate);
    System.out.println("[Court] Current bookings associated with this Court object (size = " + bookings.size() + "): " + bookings);
    int count = 0;
    for (Booking booking : bookings) {
      boolean sameDay = UtilDate.sameDay(booking.getDate(), bookDate);
      boolean isFreeOrCancelled = booking.getStatus() == Booking.Status.FREE || booking.getStatus() == Booking.Status.CANCELLED;
      System.out.println("[Court] Checking booking ID " + booking.getId() + " for date " + booking.getDate() + ": sameDay? " + sameDay + ", isFreeOrCancelled? " + isFreeOrCancelled);
      if (sameDay) {
        if (isFreeOrCancelled) {
                res.add(booking);
                count++;
                System.out.println("[Court]   -> Added booking ID " + booking.getId() + " to results.");
        }
      }
    }
    System.out.println("[Court] Returning " + count + " free/cancelled bookings found in the list.");
    return res;
  }

  public String getNumber() {
    return String.valueOf(id);
  }

  public Integer getId() {
    return id;
  }

  public List<Booking> getBookings() {
    return bookings;
  }

  
  /**
   * Tests the behavior of CascadeType.PERSIST
   * This demonstrates how changes to the Court entity cascade to its bookings
   */
  private static void testCascadePersist(DataAccess da) {
    System.out.println("\n=== Testing CascadeType.PERSIST Behavior ===");
    
    try {
      // Create a new court with bookings
      System.out.println("Creating a new court with bookings...");
      
      Court newCourt = new Court(999); // Test ID that doesn't exist yet
    
      // Create a few bookings for today
      Date today = new Date();
      Booking booking1 = new Booking(today, 10, newCourt, null);
      Booking booking2 = new Booking(today, 11, newCourt, null);
      
      // No need to add bookings to court - the Booking constructor already does this
      // newCourt.addBooking(booking1);
      // newCourt.addBooking(booking2);
      
      // Start a transaction and persist the court
      System.out.println("Persisting the court with " + newCourt.bookings.size() + " bookings...");
      da.persistCourt(newCourt);
      
      // Verify the court and bookings were saved
      System.out.println("Verifying court was saved...");
      Court persistedCourt = da.getCourtById(999);
      System.out.println("Retrieved court: " + persistedCourt.getId());
      System.out.println("Number of bookings: " + persistedCourt.bookings.size());
      
      // Clean up - remove test data
      // System.out.println("Cleaning up test data...");
      // da.removeCourt(persistedCourt); // We'll need to add this method to DataAccess
      
      
    } catch (Exception e) {
      System.err.println("Error during cascade test: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    // Create DataAccess instance
    DataAccess da = new DataAccess();
    
    try {

      // Test cascade persist behavior
      testCascadePersist(da);
      
    } catch (Exception e) {
      System.err.println("Error during testing: " + e.getMessage());
      e.printStackTrace();
    } finally {
      da.close();
    }
  }
}
