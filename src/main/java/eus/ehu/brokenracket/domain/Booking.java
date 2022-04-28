package eus.ehu.brokenracket.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
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
  private Rate rate;
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


  @OneToOne
  private Member member;

  @OneToOne
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
}
