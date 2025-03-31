package eus.ehu.brokenracket.dataAccess;

import eus.ehu.brokenracket.configuration.AppConfig;
import eus.ehu.brokenracket.configuration.UtilDate;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import eus.ehu.brokenracket.domain.Member;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.NoResultException;

// Hibernate imports
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
    
    private boolean manuallyClosedDb = false;
    
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
            // 1. Clear Court-Booking relationships first to avoid constraint violations
            System.out.println("Clearing Court-Booking relationships...");
            List<Court> courts = db.createQuery("SELECT c FROM Court c", Court.class).getResultList();
            for (Court court : courts) {
                court.clearBookings(); // Use the new method to clear the collection
                db.merge(court); // Update the court entity to remove associations in join table
            }
            db.flush(); // Ensure changes are pushed to the database
            System.out.println("Court-Booking relationships cleared.");

            // 2. Clear Member-Booking relationships
            System.out.println("Clearing Member-Booking relationships...");
            List<Member> members = db.createQuery("SELECT m FROM Member m", Member.class).getResultList();
            for (Member member : members) {
                member.clearBookings(); // Use the new method
                db.merge(member); // Update the member entity
            }
            db.flush(); // Ensure changes are pushed to the database
            System.out.println("Member-Booking relationships cleared.");

            // 3. Now delete entities in the correct order
            System.out.println("Clearing existing Booking data...");
            db.createQuery("DELETE FROM Booking").executeUpdate();
             System.out.println("Clearing existing Member data...");
            db.createQuery("DELETE FROM Member").executeUpdate();
             System.out.println("Clearing existing Court data...");
            db.createQuery("DELETE FROM Court").executeUpdate();
            db.getTransaction().commit(); // Commit deletions before inserting

            // Clear the persistence context to remove any cached entities
            db.clear();
            
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
        final int COURTNUM = 5;
        Court[] courts = new Court[COURTNUM];
        for (int court = 0; court < COURTNUM; court++) {
            courts[court] = new Court(court);
            db.persist(courts[court]);
        }

        // For debugging - track occupied bookings for April 27, 2025
        List<String> apr27Bookings = new ArrayList<>();

        // Generate free slots for all courts, month April
        for (int court = 0; court < COURTNUM; court++) {
            for (int day = 1; day < 30; day++) {
                for (int hour = 9; hour < 18; hour++) {
                    Booking booking;
                    // Oihane wants to book a court for April/27 and April/28
                    if ((court == 0 && ((day == 27 && hour == 15) || (day == 28 && hour == 10))) ||
                        (court == 1 && day == 27 && hour == 16)) {
                         booking = new Booking(UtilDate.newDate(2025, 4, day), hour, courts[court], oihane);
                         
                         // Debug for April 27
                         if (day == 27) {
                             apr27Bookings.add("Booked: Court " + court + ", Hour " + hour + ", Status: " + booking.getStatus());
                         }
                    } else {
                         booking = new Booking(UtilDate.newDate(2025, 4, day), hour, courts[court], null /* free slot */);
                    }
                    db.persist(booking); // Persist each booking
                }
            }
        }

        db.persist(oihane);
        db.persist(aitor);
        
        // Print debug info for April 27 bookings
        System.out.println("\n=== INITIALIZATION: APRIL 27, 2025 BOOKINGS ===");
        System.out.println("Bookings created for Oihane: " + apr27Bookings);
        System.out.println("Expected occupied slots for April 27: Court 0, Hour 15 and Court 1, Hour 16");
        System.out.println("=================================================\n");
        
        System.out.println("Finished persisting test data.");
    }

    public void close() {
        if (manuallyClosedDb) {
            System.out.println("DataAccess already closed, skipping...");
            return;
        }
        
        System.out.println("Closing DataAccess resources...");
        manuallyClosedDb = true;
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
            // Check if this is a transient booking object (from free slots list)
            if (book.getBookingID() == null) {
                // This is a transient booking (free slot) from the UI
                System.out.println("[DataAccess] Creating new booking for free slot: Date=" + book.getDate() + 
                                  ", Hour=" + book.getStartingHour() + ", Court=" + book.getCourt().getNumber());
                
                // First, check if a booking already exists for this court, date, and hour
                // Use a simplified native query to avoid JPQL function problems
                String nativeQuery = "SELECT * FROM Booking b WHERE b.court_id = ? " +
                                    "AND b.startingHour = ? " +
                                    "AND YEAR(b.date) = ? " +
                                    "AND MONTH(b.date) = ? " +
                                    "AND DAY(b.date) = ?";
                
                Calendar cal = Calendar.getInstance();
                cal.setTime(book.getDate());
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
                int day = cal.get(Calendar.DAY_OF_MONTH);
                
                @SuppressWarnings("unchecked")
                List<Booking> existingBookings = db.createNativeQuery(nativeQuery, Booking.class)
                    .setParameter(1, book.getCourt().getId())
                    .setParameter(2, book.getStartingHour())
                    .setParameter(3, year)
                    .setParameter(4, month)
                    .setParameter(5, day)
                    .getResultList();
                
                Booking persistentBooking;
                if (!existingBookings.isEmpty()) {
                    // Use the existing booking
                    persistentBooking = existingBookings.get(0);
                    if (persistentBooking.getMember() != null) {
                        throw new IllegalStateException("Selected slot is already booked by " + 
                                                      persistentBooking.getMember().getName());
                    }
                } else {
                    // Create a new booking record
                    persistentBooking = new Booking(book.getDate(), book.getStartingHour(), 
                                                   book.getCourt(), null);
                    db.persist(persistentBooking);
                }
                
                // Now assign the member to the booking
                persistentBooking.setBook(member);
                // Explicitly set status to OCCUPIED
                persistentBooking.setStatus(Booking.Status.OCCUPIED);
                
            } else {
                // Original code for managed bookings with IDs
                Booking persistentBooking = db.find(Booking.class, book.getBookingID());
                if (persistentBooking == null) {
                     throw new IllegalArgumentException("Booking not found in database: " + book.getBookingID());
                }
                if(persistentBooking.getMember() != null) {
                    throw new IllegalStateException("Selected booking is already assigned to " + persistentBooking.getMember().getName());
                }
                persistentBooking.setBook(member);
                // Explicitly set status to OCCUPIED
                persistentBooking.setStatus(Booking.Status.OCCUPIED);
            }
            
            db.getTransaction().commit();
            System.out.println("[DataAccess] Booking set successfully for member: " + name);
            
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                 db.getTransaction().rollback();
            }
            System.err.println("Error setting book: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Finds bookings for a specific court on a specific date.
     * Note: Compares the date part only, ignoring time.
     * @param court The court entity.
     * @param date The date to search for.
     * @return A list of bookings for that court and date.
     */
    public List<Booking> getBookingsByCourtAndDate(Court court, Date date) {
        System.out.println("[DataAccess] Fetching bookings for Court #: " + court.getNumber() + " on Date: " + date);

        // Create a new EntityManager specifically for this query to avoid connection issues
        EntityManager localEm = null;
        try {
            if (emf == null || !emf.isOpen()) {
                System.err.println("[DataAccess] EntityManagerFactory is not available. Cannot query bookings.");
                return Collections.emptyList();
            }
            
            localEm = emf.createEntityManager();
            localEm.getTransaction().begin();
            
            // Extract year, month, day from the input date
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int day = cal.get(Calendar.DAY_OF_MONTH);
            
            // Use native SQL query to avoid complex JPQL with functions
            String sql = "SELECT * FROM Booking b WHERE b.court_id = ? " +
                         "AND YEAR(b.date) = ? AND MONTH(b.date) = ? AND DAY(b.date) = ?";
            
            @SuppressWarnings("unchecked")
            List<Booking> bookings = localEm.createNativeQuery(sql, Booking.class)
                .setParameter(1, court.getNumber())
                .setParameter(2, year)
                .setParameter(3, month)
                .setParameter(4, day)
                .getResultList();
            
            // Commit transaction
            localEm.getTransaction().commit();
            
            System.out.println("[DataAccess] Found " + bookings.size() + " bookings for Court #" + court.getNumber() + " on " + date);
            
            // Debug output
            for (Booking b : bookings) {
                System.out.println("[DataAccess]   - Booking: ID=" + b.getId() + 
                                   ", Hour=" + b.getStartingHour() + 
                                   ", Status=" + b.getStatus() + 
                                   ", Member=" + (b.getMember() != null ? b.getMember().getName() : "null"));
            }
            
            return bookings;
        } catch (Exception e) {
            System.err.println("[DataAccess] Error fetching bookings: " + e.getMessage());
            e.printStackTrace();
            if (localEm != null && localEm.getTransaction().isActive()) {
                localEm.getTransaction().rollback();
            }
            return Collections.emptyList();
        } finally {
            if (localEm != null && localEm.isOpen()) {
                localEm.close();
                System.out.println("[DataAccess] Local EntityManager closed");
            }
        }
    }

    /**
     * Finds free bookings for a specific court on a specific date.
     * @param court The court entity.
     * @param date The date to search for.
     * @return A list of free bookings for that court and date.
     */
    public List<Booking> getFreeBookingsByCourtAndDate(Court court, Date date) {
        System.out.println("[DataAccess] Fetching FREE bookings for Court #: " + court.getNumber() + " on Date: " + date);

        // Create a new EntityManager specifically for this query to avoid connection issues
        EntityManager localEm = null;
        try {
            if (emf == null || !emf.isOpen()) {
                System.err.println("[DataAccess] EntityManagerFactory is not available. Cannot query bookings.");
                return Collections.emptyList();
            }
            
            localEm = emf.createEntityManager();
            localEm.getTransaction().begin();
            
            // Extract year, month, day from the input date
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int day = cal.get(Calendar.DAY_OF_MONTH);
            
            // Use native SQL query to retrieve only FREE bookings
            String sql = "SELECT * FROM Booking b WHERE b.court_id = ? " +
                         "AND YEAR(b.date) = ? AND MONTH(b.date) = ? AND DAY(b.date) = ? " +
                         "AND b.status = 0"; // 0 is the ordinal value for Status.FREE
            
            @SuppressWarnings("unchecked")
            List<Booking> freeBookings = localEm.createNativeQuery(sql, Booking.class)
                .setParameter(1, court.getNumber())
                .setParameter(2, year)
                .setParameter(3, month)
                .setParameter(4, day)
                .getResultList();
            
            // Commit transaction
            localEm.getTransaction().commit();
            
            System.out.println("[DataAccess] Found " + freeBookings.size() + " FREE bookings for Court #" + court.getNumber() + " on " + date);
            
            return freeBookings;
        } catch (Exception e) {
            System.err.println("[DataAccess] Error fetching free bookings: " + e.getMessage());
            e.printStackTrace();
            if (localEm != null && localEm.getTransaction().isActive()) {
                localEm.getTransaction().rollback();
            }
            return Collections.emptyList();
        } finally {
            if (localEm != null && localEm.isOpen()) {
                localEm.close();
                System.out.println("[DataAccess] Local EntityManager closed");
            }
        }
    }
}
