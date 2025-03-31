package eus.ehu.brokenracket.domain;

import java.util.Date;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Invoice {
  @Id  @GeneratedValue
  private Long number;
  private Date date;
  private Status status;
  private float amount;

  public Invoice() {

  }

  enum Status {
    UNPAID, PAID;
  }

  @OneToOne
  private Member member;

  public Invoice(Date date, float amount, Member member) {
    this.date = date;
    this.amount = amount;
    this.member = member;
    this.status = Status.UNPAID;
  }

  public Long getId() {
    return number;
  }

  public void setId(Long id) {
    this.number = id;
  }

}
