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

    protected EntityManager  db;
    protected EntityManagerFactory emf;

    ConfigXML config = ConfigXML.getInstance();

    public DataAccess(boolean initializeMode)  {
      System.out.println("Creating DataAccess instance => isDatabaseLocal: " +
          config.isDataAccessLocal() + " getDatabBaseOpenMode: " + config.getDataBaseOpenMode());
      open(initializeMode);

      if (initializeMode){
        initializeDB();
      }
    }

    public DataAccess()  {
      this(false);
    }


    /**
     * This method initializes the database with some trial events and questions.
     * It is invoked by the business logic when the option "initialize" is used
     * in the tag dataBaseOpenMode of resources/config.xml file
     */
    public void initializeDB(){



      try {

        db.getTransaction().begin();

        generateTestingData();

        db.getTransaction().commit();
        System.out.println("The database has been initialized");


      }
      catch (Exception e){
        e.printStackTrace();
      }
    }


    private void generateTestingData() {
      Member oihane = new Member("Oihane", "c/ Melancolía 13", "678012345");
      Member aitor = new Member("Aitor", "c/ Esperanza 14", "678999999");

      Court court1 = new Court(1);

      Booking booking1 = new Booking(UtilDate.newDate(2022, 4, 27), 15, court1, oihane);
      Booking booking2 = new Booking(UtilDate.newDate(2022, 4, 28), 10, court1, oihane);


      db.persist(oihane);
      db.persist(aitor);
    }


    public void open(boolean initializeMode){

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
            ":"+config.getDataAccessPort() + "/" + fileName, properties);

        db = emf.createEntityManager();
      }
    }


    public void close(){
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

      BlFacadeImplementation.getInstance().createInvoice(oihane,4, 2022);
    }
  }
