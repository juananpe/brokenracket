package eus.ehu.brokenracket.dataAccess;

import eus.ehu.brokenracket.businessLogic.BlFacadeImplementation;
import eus.ehu.brokenracket.configuration.AppConfig;
import eus.ehu.brokenracket.configuration.UtilDate;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
// import jakarta.persistence.Persistence; // No longer using standard Persistence bootstrap
import jakarta.persistence.TypedQuery;
import jakarta.persistence.NoResultException; // Import if needed for specific queries

// Hibernate imports
import org.hibernate.Session; // Hibernate Session is often used directly or via casting EntityManager
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.MetadataSources;


import java.util.*;

/**
 * Implements the Data Access utility using Hibernate and H2 (configured via hibernate.cfg.xml).
 * Simplified initialization based on provided example.
 */
public class DataAccess {

    protected EntityManager db; // Standard JPA EntityManager
    protected EntityManagerFactory emf; // Standard JPA EMF (Hibernate SessionFactory implements this)

    AppConfig config = AppConfig.getInstance();
    
    public DataAccess() {
        // Determine initializeMode based on config's openMode (e.g., for triggering data generation)
        boolean initialize = config.getDataBaseOpenMode().equalsIgnoreCase("initialize");
        this.open(initialize); // Call open with the initialization flag

        // Add shutdown hook to close database when application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown Hook: Closing database connection...");
            this.close();
        }));
    }

    /**
     * Opens the database connection using Hibernate configuration.
     * Relies on hibernate.cfg.xml for connection details and schema management (hbm2ddl.auto).
     * @param initializeMode If true, triggers database initialization (data generation).
     */
    public void open(boolean initializeMode) {
        System.out.println("Opening Hibernate DataAccess (Simplified). Mode: " + (initializeMode ? "Initialize" : "Open"));

        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml by default
                .build();
        try {
            // Create SessionFactory from registry
            this.emf = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            // Create JPA EntityManager
            this.db = emf.createEntityManager();
            System.out.println("Hibernate EntityManager created successfully from SessionFactory.");

             // Perform data initialization *if* requested and EntityManager was created successfully
            if (initializeMode) {
               initializeDB();
            }

        } catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building it
            // so destroy the registry manually.
            System.err.println("!!! Error building SessionFactory: " + e.getMessage());
            e.printStackTrace();
            StandardServiceRegistryBuilder.destroy(registry);
            throw new RuntimeException("Failed to initialize Hibernate SessionFactory", e); // Re-throw
        }
    }

    /**
     * Initializes the database with test data. Assumes schema is managed by hbm2ddl.auto in hibernate.cfg.xml.
     */
    public void initializeDB() {
        System.out.println("Initializing DB with test data (schema managed by Hibernate cfg)...");
        if (db == null || !db.isOpen()) {
             System.err.println("Cannot initialize DB, EntityManager is not open.");
             return;
        }
        db.getTransaction().begin();
        try {
            // It's generally safer to clear existing data if initializing
            // Note: This might be redundant if hbm2ddl.auto is create or create-drop
            System.out.println("Clearing existing Booking data...");
            db.createQuery("DELETE FROM Booking").executeUpdate();
             System.out.println("Clearing existing Member data...");
            db.createQuery("DELETE FROM Member").executeUpdate();
             System.out.println("Clearing existing Court data...");
            db.createQuery("DELETE FROM Court").executeUpdate();
            db.getTransaction().commit(); // Commit deletions before inserting

            db.getTransaction().begin(); // Start new transaction for inserts
            System.out.println("Generating test data...");
            generateTestingData();
            db.getTransaction().commit();
            System.out.println("Test data generated and committed.");

        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            System.err.println("Error during DB initialization: " + e.getMessage());
            e.printStackTrace();
            // Decide if this should halt the application or just log
        }
    }

    private void generateTestingData() {
        // This method using db.persist() is standard JPA and should work fine
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
                    Booking booking;
                    // Oihane wants to book a court for April/27 and April/28
                    if (court == 0 && ((day == 27 && hour == 15) || (day == 28 && hour == 10))) {
                         booking = new Booking(UtilDate.newDate(2022, 4, day), hour, courts[court], oihane);
                    } else {
                         booking = new Booking(UtilDate.newDate(2022, 4, day), hour, courts[court], null /* free slot */);
                    }
                    db.persist(booking); // Persist each booking
                }
            }
        }

        db.persist(oihane);
        db.persist(aitor);
        System.out.println("Finished persisting test data.");
    }

    public void close() {
        System.out.println("Closing DataAccess resources...");
        if (db != null && db.isOpen()) {
            try {
              db.close();
              System.out.println("EntityManager closed.");
            } catch (Exception e) {
              System.err.println("Error closing EntityManager: " + e.getMessage());
            }
        }
        if (emf != null && emf.isOpen()) {
            try {
              emf.close(); // Close the EntityManagerFactory (SessionFactory)
              System.out.println("EntityManagerFactory closed.");
            } catch (Exception e) {
              System.err.println("Error closing EntityManagerFactory: " + e.getMessage());
            }
        }
    }
    public void createInvoice(Member member, float total, int month, int year) {
        
        db.getTransaction().begin();
        try {
            Member managedMember = db.find(Member.class, member.getId()); // Work with managed entity
            if (managedMember != null) {
                 managedMember.createInvoice(total, month, year);
                 // merge might not be needed if managedMember was fetched in the transaction
                 // db.merge(managedMember);
            } else {
                 System.err.println("Cannot create invoice, member not found: " + member.getId());
                 // Handle error appropriately
            }
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                 db.getTransaction().rollback();
            }
            System.err.println("Error creating invoice: " + e.getMessage());
            throw e;
        }
    }

    // The main method is primarily for testing DataAccess itself.
    public static void main(String[] args) {
        System.out.println("Running DataAccess main for testing...");
        // Let constructor handle opening based on config
        DataAccess da = new DataAccess();
        try {
            // Example: fetch courts
            List<Court> courts = da.getCourts();
            System.out.println("Found " + courts.size() + " courts.");
            for(Court c : courts) {
                System.out.println("  - " + c);
            }

            // Example: Try finding a member (if data was initialized)
            try {
                 TypedQuery<Member> memberQuery = da.db.createQuery(
                    "SELECT m FROM Member m WHERE m.name = ?1", Member.class);
                 memberQuery.setParameter(1, "Oihane"); // Example name
                 Member oihane = memberQuery.getSingleResult();
                 System.out.println("Found member: " + oihane.getName());
            } catch (NoResultException e) {
                 System.out.println("Member 'Oihane' not found (maybe DB not initialized?).");
            }

        } catch(Exception e) {
            System.err.println("Error in DataAccess main method: " + e.getMessage());
            e.printStackTrace();
        } finally {
             // Shutdown hook will call close, but explicit close here is fine too for main test
             da.close();
        }
        System.out.println("DataAccess main finished.");
    }

    public List<Court> getCourts() {
        TypedQuery<Court> query = db.createQuery(
            "SELECT c FROM Court c", Court.class);
        return query.getResultList();
    }

    public void setBook(String name, Booking book) {
        Member member = null;
        try {
             TypedQuery<Member> memberQuery = db.createQuery(
                "SELECT m FROM Member m WHERE m.name = ?1", Member.class);
             memberQuery.setParameter(1, name);
             member = memberQuery.getSingleResult();
        } catch (NoResultException e) {
             System.err.println("Cannot set book, member not found: " + name);
             throw new IllegalArgumentException("Member with name '" + name + "' not found.", e);
        }

        db.getTransaction().begin();
        try {
            // It's crucial to work with the managed instance of the Booking
            Booking persistentBooking = db.find(Booking.class, book.getBookingID());
            if (persistentBooking == null) {
                 throw new IllegalArgumentException("Booking not found in database: " + book.getBookingID());
            }
            if(persistentBooking.getMember() != null) {
                throw new IllegalStateException("Selected booking is already assigned to " + persistentBooking.getMember().getName());
            }
            persistentBooking.setBook(member);
            // member should already be managed if fetched previously
            // persistentBooking is managed because we used find()
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                 db.getTransaction().rollback();
            }
            System.err.println("Error setting book: " + e.getMessage());
            throw e;
        }
    }
}
