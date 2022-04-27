package eus.ehu.brokenracket.domain;

import javax.persistence.Entity;
import javax.persistence.Id;


@Entity
public class Court {
  @Id
  private Integer id;

  public Court(Integer id) {
    this.id = id;
  }

  public Court() {

  }
}
