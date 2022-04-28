package eus.ehu.brokenracket.businessLogic;


import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;

import java.util.Date;
import java.util.List;
import java.util.Vector;

/**
 * Interface that specifies the business logic.
 */
public interface BlFacade  {

    void createInvoice(Member member, int month, int year);
    List<Court> getCourts();
    List<Booking> getFreeBooks(Court court, Date bookDate);
    void setBook(String name, Booking book);

}
