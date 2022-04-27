package eus.ehu.brokenracket.businessLogic;


import eus.ehu.brokenracket.domain.Member;

import java.util.Date;
import java.util.Vector;

/**
 * Interface that specifies the business logic.
 */
public interface BlFacade  {

    void createInvoice(Member member, int month, int year);

}
