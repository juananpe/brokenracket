package eus.ehu.brokenracket.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import java.util.Date;

@Entity
public class Booking {

  public Booking() {

  }

  public Date getDate() {
    return date;
  }

  public float getTarif() {
    return this.rate.getRate();
  }

  public Status getStatus() {
    return this.status;
  }

  public void setBook(Member member) {
    this.member = member;
    member.addBooking(this);
  }

  public enum Status {
    FREE, OCCUPIED, CANCELLED, UNUSED;
  }

  public enum Rate {
    R1(4.5f), R2(3.5f), R3(2.1f), R4(5.0f);

    private final float tarif;

    Rate(float tarif){
      this.tarif = tarif;
    }

    public float getRate(){
      return this.tarif;
    }
  }

  @Id  @GeneratedValue
  private Long id;
  private Date date;
  private int startingHour;

  // prime-rate R1 (for weekends and evening hours)
  // normal rate R2 (for other hours)
  // fee R3 for canceled reservations
  // penalty R4 for unused reservations.
  @Enumerated(EnumType.ORDINAL)
  private Rate rate;

  @Enumerated(EnumType.ORDINAL)
  private Status status;

  public Booking(Date date, int startingHour, Court court, Member member){
      this.date = date;
      this.startingHour = startingHour;
      this.court = court;
      this.member = member;
      this.status = member != null? Status.OCCUPIED: Status.FREE;

      if (startingHour >= 15 && startingHour <= 17) /* or weekend date */ {
        this.rate = Rate.R1;
      } else {
        this.rate = Rate.R2;
      }

      if (member != null) member.addBooking(this);
      court.addBooking(this);

  }


  @ManyToOne
  private Member member;

  @ManyToOne
  private Court court;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "Booking{" +
        "id=" + id +
        ", date=" + date +
        ", startingHour=" + startingHour +
        ", rate=" + rate +
        ", status=" + status +
        // ", member=" + member + //
        ", court=" + court +
        '}';
  }

  public String getHour() {
    return String.valueOf(startingHour);
  }

  public Member getMember() {
    return member;
  }

  public Court getCourt() {
    return court;
  }

  public Long getBookingID() {
    return id;
  }

  // Added getter for startingHour
  public int getStartingHour() {
    return startingHour;
  }

  // Add setter for status
  public void setStatus(Status newStatus) {
    this.status = newStatus;
  }
}
