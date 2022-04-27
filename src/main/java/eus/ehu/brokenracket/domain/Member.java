package eus.ehu.brokenracket.domain;

import eus.ehu.brokenracket.configuration.UtilDate;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Member {
  private String name;
  private String address;
  @Id  @GeneratedValue
  private Long id;
  private String password;

  public Member(String name, String address, String password) {
    this.name = name;
    this.address = address;
    this.password = password;
  }

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  private List<Invoice> invoices = new ArrayList<>();

  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  private List<Booking> bookings = new ArrayList<>();

  public Member() {

  }

  public void addBooking(Booking b){
    bookings.add(b);
  }

  public List<Booking> getBookings(int month) {
    List<Booking> result = new ArrayList<>();

    bookings.forEach(b -> {
      if (UtilDate.getMonthNumber(b.getDate()) == month){
        result.add(b);
      }
    });

    return result;
  }

  public void createInvoice(float total, int month, int year) {
    Invoice invoice = new Invoice(UtilDate.newDate(year, month, 1), total, this);
    invoices.add(invoice);
  }
}
