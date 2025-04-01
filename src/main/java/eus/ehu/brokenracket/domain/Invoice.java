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

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Status getStatus() {
    return status;
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public float getAmount() {
    return amount;
  }

  public void setAmount(float amount) {
    this.amount = amount;
  }

  public Member getMember() {
    return member;
  }

  public void setMember(Member member) {
    this.member = member;
  }
}
