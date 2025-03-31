package eus.ehu.brokenracket.domain;

import eus.ehu.brokenracket.configuration.UtilDate;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Entity
public class Court {
  @Id
  private Integer id;

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
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
}
