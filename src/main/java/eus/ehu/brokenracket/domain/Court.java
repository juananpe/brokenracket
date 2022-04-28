package eus.ehu.brokenracket.domain;

import eus.ehu.brokenracket.configuration.UtilDate;

import javax.persistence.*;
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

  public List<Booking> getFreeBooks(Date bookDate) {
    List<Booking> res = new ArrayList<>();
    for (Booking booking : bookings) {
      if (UtilDate.sameDay(booking.getDate(), bookDate)) {
        if (booking.getStatus() == Booking.Status.FREE ||
            booking.getStatus() == Booking.Status.CANCELLED) {
                res.add(booking);
        }
      }
    }
    return res;
  }
}
