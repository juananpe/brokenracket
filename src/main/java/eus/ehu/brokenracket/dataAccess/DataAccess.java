package eus.ehu.brokenracket.dataAccess;

import eus.ehu.brokenracket.businessLogic.BlFacadeImplementation;
import eus.ehu.brokenracket.configuration.ConfigXML;
import eus.ehu.brokenracket.configuration.UtilDate;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;
import javafx.scene.chart.PieChart;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 * Implements the Data Access utility to the objectDb database
 */
public class DataAccess {

  protected EntityManager db;
  protected EntityManagerFactory emf;

  ConfigXML config = ConfigXML.getInstance();

  public DataAccess(boolean initializeMode) {
    System.out.println("Creating DataAccess instance => isDatabaseLocal: " +
        config.isDataAccessLocal() + " getDatabBaseOpenMode: " + config.getDataBaseOpenMode());
    open(initializeMode);

    if (initializeMode) {
      initializeDB();
    }
  }

  public DataAccess() {
    this(false);
  }


  /**
   * This method initializes the database with some trial events and questions.
   * It is invoked by the business logic when the option "initialize" is used
   * in the tag dataBaseOpenMode of resources/config.xml file
   */
  public void initializeDB() {


    try {

      db.getTransaction().begin();

      generateTestingData();

      db.getTransaction().commit();
      System.out.println("The database has been initialized");


    } catch (Exception e) {
      e.printStackTrace();
    }
  }


  private void generateTestingData() {
    Member oihane = new Member("Oihane", "c/ Melancolía 13", "678012345");
    Member aitor = new Member("Aitor", "c/ Esperanza 14", "678999999");

    // initialize courts
    final int COURTNUM = 3;
    Court[] courts = new Court[COURTNUM];
    for (int court = 0; court < COURTNUM; court++) {
      courts[court] = new Court(court);
      db.persist(courts[court]);
    }

    // Generate free slots for all courts, month April
    for (int court = 0; court < COURTNUM; court++) {
      for (int day = 1; day < 30; day++) {
        for (int hour = 9; hour < 18; hour++) {

          // Oihane wants to book a court for April/27 and April/28
          if (court == 0 && ((day == 27 && hour == 15) || (day == 28 && hour == 10))) {
            new Booking(UtilDate.newDate(2022, 4, day), hour, courts[court], oihane);
          } else {
            new Booking(UtilDate.newDate(2022, 4, day), hour, courts[court], null /* free slot */);
          }
        }
      }
    }


    db.persist(oihane);
    db.persist(aitor);
  }


  public void open(boolean initializeMode) {

    System.out.println("Opening DataAccess instance => isDatabaseLocal: " +
        config.isDataAccessLocal() + " getDatabBaseOpenMode: " + config.getDataBaseOpenMode());

    String fileName = config.getDataBaseFilename();
    if (initializeMode) {
      fileName = fileName + ";drop";
      System.out.println("Deleting the DataBase");
    }

    if (config.isDataAccessLocal()) {
      emf = Persistence.createEntityManagerFactory("objectdb:" + fileName);
      db = emf.createEntityManager();
    } else {
      Map<String, String> properties = new HashMap<String, String>();
      properties.put("javax.persistence.jdbc.user", config.getDataBaseUser());
      properties.put("javax.persistence.jdbc.password", config.getDataBasePassword());

      emf = Persistence.createEntityManagerFactory("objectdb://" + config.getDataAccessNode() +
          ":" + config.getDataAccessPort() + "/" + fileName, properties);

      db = emf.createEntityManager();
    }
  }


  public void close() {
    db.close();
    System.out.println("DataBase is closed");
  }

  public void createInvoice(Member member, float total, int month, int year) {
    db.getTransaction().begin();
    member.createInvoice(total, month, year);
    db.merge(member);
    db.getTransaction().commit();
  }


  public static void main(String[] args) {

    DataAccess da = new DataAccess(true);
    String name = "Oihane";
    TypedQuery<Member> query = da.db.createQuery(
        "SELECT m FROM Member m WHERE m.name = ?1", Member.class);
    Member oihane = query.setParameter(1, name).getSingleResult();
    da.close();

    BlFacadeImplementation.getInstance().createInvoice(oihane, 4, 2022);

    List<Court> courts = BlFacadeImplementation.getInstance().getCourts();
    List<Booking> books = BlFacadeImplementation.getInstance().getFreeBooks(courts.get(0), UtilDate.newDate(2022, 4, 27));

    Booking book = books.get(0); // select the first free booking slot
    BlFacadeImplementation.getInstance().setBook(name, book);  // the first free slot for court:0 date:2022/04/27 will be assigned to Oihane

    System.out.println(oihane);

  }

  public List<Court> getCourts() {
    TypedQuery<Court> query = db.createQuery(
        "SELECT c FROM Court c", Court.class);
    return query.getResultList();
  }


  public void setBook(String name, Booking book) {
    TypedQuery<Member> query = db.createQuery(
        "SELECT m FROM Member m WHERE m.name = ?1", Member.class);
    query.setParameter(1, name);
    Member member = query.getSingleResult();

    db.getTransaction().begin();
    book.setBook(member);
    db.getTransaction().commit();

  }
}
