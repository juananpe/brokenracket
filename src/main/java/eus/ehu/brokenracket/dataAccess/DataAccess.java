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

// Logger imports
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

/**
 * Implements the Data Access utility using Hibernate and H2 (configured via hibernate.cfg.xml).
 * Simplified initialization based on provided example.
 */
public class DataAccess {

    private static final Logger logger = LogManager.getLogger(DataAccess.class);
    
    protected EntityManager db; // Standard JPA EntityManager
    protected EntityManagerFactory emf; // Standard JPA EMF (Hibernate SessionFactory implements this)

    AppConfig config = AppConfig.getInstance();
    
    private boolean manuallyClosedDb = false;
    
    public DataAccess() {
        // Just open the DB connection - no initialization
        this.open();
        
        // Check if we need to initialize the DB based on config
        if (config.getDataBaseOpenMode().equalsIgnoreCase("initialize")) {
            initializeDB();
        }

        // Add shutdown hook to close database when application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutdown Hook: Closing database connection...");
            this.close();
        }));
    }

    /**
     * Opens the database connection using Hibernate configuration.
     * Relies on hibernate.cfg.xml for connection details and schema management.
     */
    public void open() {
        logger.info("Opening Hibernate DataAccess connection...");

        final StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .configure() // configures settings from hibernate.cfg.xml by default
                .build();
        try {
            // Create SessionFactory from registry
            this.emf = new MetadataSources(registry).buildMetadata().buildSessionFactory();
            // Create JPA EntityManager
            this.db = emf.createEntityManager();
            logger.info("Hibernate EntityManager created successfully from SessionFactory.");
        } catch (Exception e) {
            // The registry would be destroyed by the SessionFactory, but we had trouble building it
            // so destroy the registry manually.
            logger.error("!!! Error building SessionFactory: " + e.getMessage());
            e.printStackTrace();
            StandardServiceRegistryBuilder.destroy(registry);
            throw new RuntimeException("Failed to initialize Hibernate SessionFactory", e); // Re-throw
        }
    }

    /**
     * Initializes the database with test data. Assumes schema is managed by hbm2ddl.auto in hibernate.cfg.xml.
     */
    public void initializeDB() {
        logger.info("Initializing DB with test data (schema managed by Hibernate cfg)...");
        if (db == null || !db.isOpen()) {
             logger.error("Cannot initialize DB, EntityManager is not open.");
             return;
        }
        db.getTransaction().begin();
        try {
            // 1. Clear Court-Booking relationships first to avoid constraint violations
            logger.info("Clearing Court-Booking relationships...");
            List<Court> courts = db.createQuery("SELECT c FROM Court c", Court.class).getResultList();
            for (Court court : courts) {
                court.clearBookings(); // Use the new method to clear the collection
                db.merge(court); // Update the court entity to remove associations in join table
            }
            db.flush(); // Ensure changes are pushed to the database
            logger.info("Court-Booking relationships cleared.");

            // 2. Clear Member-Booking relationships
            logger.info("Clearing Member-Booking relationships...");
            List<Member> members = db.createQuery("SELECT m FROM Member m", Member.class).getResultList();
            for (Member member : members) {
                member.clearBookings(); // Use the new method
                db.merge(member); // Update the member entity
            }
            db.flush(); // Ensure changes are pushed to the database
            logger.info("Member-Booking relationships cleared.");

            // 3. Now delete entities in the correct order
            logger.info("Clearing existing Booking data...");
            db.createQuery("DELETE FROM Booking").executeUpdate();
             logger.info("Clearing existing Member data...");
            db.createQuery("DELETE FROM Member").executeUpdate();
             logger.info("Clearing existing Court data...");
            db.createQuery("DELETE FROM Court").executeUpdate();
            db.getTransaction().commit(); // Commit deletions before inserting

            // Clear the persistence context to remove any cached entities
            db.clear();
            
            db.getTransaction().begin(); // Start new transaction for inserts
            logger.info("Generating test data...");
            generateTestingData();
            db.getTransaction().commit();
            logger.info("Test data generated and committed.");

        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            logger.error("Error during DB initialization: " + e.getMessage());
            e.printStackTrace();
            // Decide if this should halt the application or just log
        }
    }

    private void generateTestingData() {
        // This method using db.persist() is standard JPA and should work fine
        logger.debug("Creating member entities...");
        Member ane = new Member("ane", "c/ Melancolía 13", "678012345");
        Member aitor = new Member("Aitor", "c/ Esperanza 14", "678999999");

        // Persist members first to ensure they have IDs before being referenced
        logger.debug("Persisting member ane: " + ane.getName());
        db.persist(ane);
        logger.debug("Persisting member aitor: " + aitor.getName());
        db.persist(aitor);
        
        // Ensure members are flushed to the database immediately
        logger.debug("Flushing entities to database to ensure they are saved...");
        db.flush();

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
                    // ane wants to book a court for April/27 and April/28
                    if ((court == 0 && ((day == 27 && hour == 15) || (day == 28 && hour == 10))) ||
                        (court == 1 && day == 27 && hour == 16)) {
                         booking = new Booking(UtilDate.newDate(2025, 4, day), hour, courts[court], ane);
                         
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
        
        logger.info("Finished persisting test data.");
    }

    public void close() {
        if (manuallyClosedDb) {
            logger.info("DataAccess already closed, skipping...");
            return;
        }
        
        logger.info("Closing DataAccess resources...");
        manuallyClosedDb = true;
        if (db != null && db.isOpen()) {
            try {
              db.close();
              logger.info("EntityManager closed.");
            } catch (Exception e) {
              logger.error("Error closing EntityManager: " + e.getMessage());
            }
        }
        if (emf != null && emf.isOpen()) {
            try {
              emf.close(); // Close the EntityManagerFactory (SessionFactory)
              logger.info("EntityManagerFactory closed.");
            } catch (Exception e) {
              logger.error("Error closing EntityManagerFactory: " + e.getMessage());
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
                 logger.error("Cannot create invoice, member not found: " + member.getId());
                 // Handle error appropriately
            }
            db.getTransaction().commit();
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                 db.getTransaction().rollback();
            }
            logger.error("Error creating invoice: " + e.getMessage());
            throw e;
        }
    }

    // The main method is primarily for testing DataAccess itself.
    public static void main(String[] args) {
        logger.info("Running DataAccess main for testing...");
        // Create new DataAccess - constructor handles opening and initialization based on config
        DataAccess da = new DataAccess();
        try {
            // Example: fetch courts
            List<Court> courts = da.getCourts();
            logger.info("Found " + courts.size() + " courts.");
            for(Court c : courts) {
                logger.info("  - " + c);
            }

            // Example: Try finding a member (if data was initialized)
            try {
                 TypedQuery<Member> memberQuery = da.db.createQuery(
                    "SELECT m FROM Member m WHERE m.name = ?1", Member.class);
                 memberQuery.setParameter(1, "ane"); // Example name
                 Member ane = memberQuery.getSingleResult();
                 logger.info("Found member: " + ane.getName());
            } catch (NoResultException e) {
                 logger.info("Member 'ane' not found (maybe DB not initialized?).");
            }

        } catch(Exception e) {
            logger.error("Error in DataAccess main method: " + e.getMessage());
            e.printStackTrace();
        } finally {
             // Explicit close for testing purposes
             da.close();
        }
        logger.info("DataAccess main finished.");
    }

    public List<Court> getCourts() {
        TypedQuery<Court> query = db.createQuery(
            "SELECT c FROM Court c", Court.class);
        return query.getResultList();
    }

    /**
     * Retrieves a specific court by ID
     * @param courtId The court ID to find
     * @return The Court entity or null if not found
     */
    public Court getCourtById(Integer courtId) {
        return db.find(Court.class, courtId);
    }

    /**
     * Persists a court entity and its associated bookings (via cascade)
     * @param court The court to persist
     */
    public void persistCourt(Court court) {
        logger.info("Persisting court: " + court.getId());
        db.getTransaction().begin();
        try {
            db.persist(court); // Will cascade to bookings due to CascadeType.PERSIST
            db.getTransaction().commit();
            logger.info("Court persisted successfully");
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            logger.error("Error persisting court: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Removes a court entity from the database
     * @param court The court to remove
     */
    public void removeCourt(Court court) {
        logger.info("Removing court: " + court.getId());
        db.getTransaction().begin();
        try {
            // Find the managed entity
            Court managedCourt = db.find(Court.class, court.getId());
            if (managedCourt != null) {
                // For each booking associated with the court, remove it
                for (Booking booking : new ArrayList<>(managedCourt.getBookings())) {
                    db.remove(booking);
                }
                // Remove the court
                db.remove(managedCourt);
            }
            db.getTransaction().commit();
            logger.info("Court removed successfully");
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            logger.error("Error removing court: " + e.getMessage());
            throw e;
        }
    }

    public void setBook(String name, Booking book) {
        Member member = null;
        try {
             TypedQuery<Member> memberQuery = db.createQuery(
                "SELECT m FROM Member m WHERE LOWER(m.name) = LOWER(?1)", Member.class);
             memberQuery.setParameter(1, name);
             member = memberQuery.getSingleResult();
        } catch (NoResultException e) {
             logger.error("Cannot set book, member not found: " + name);
             throw new IllegalArgumentException("Member with name '" + name + "' not found.", e);
        }

        db.getTransaction().begin();
        try {
            Booking persistentBooking;
            
            // Handle both transient and managed booking cases
            if (book.getBookingID() == null) {
                // Transient booking (free slot) from UI
                logger.info("********** Processing booking for free slot: Date=" + book.getDate() + 
                                  ", Hour=" + book.getStartingHour() + ", Court=" + book.getCourt().getNumber());
                
                // Find existing booking by court/date/hour
                persistentBooking = findBookingByCourtDateHour(book.getCourt(), book.getDate(), book.getStartingHour());
                
                if (persistentBooking == null) {
                    // Create a new booking record if none exists
                    persistentBooking = new Booking(book.getDate(), book.getStartingHour(), 
                                                   book.getCourt(), null);
                    db.persist(persistentBooking);
                }
            } else {
                // Managed booking with ID
                logger.info("********** Processing booking for managed slot: ID=" + book.getBookingID());
                persistentBooking = db.find(Booking.class, book.getBookingID());
                if (persistentBooking == null) {
                     throw new IllegalArgumentException("Booking not found in database: " + book.getBookingID());
                }
            }
            
            // Common validation and assignment for both cases
            if (persistentBooking.getMember() != null) {
                throw new IllegalStateException("Selected slot is already booked by " + 
                                               persistentBooking.getMember().getName());
            }
            
            // Assign member and update status
            persistentBooking.setBook(member);
            persistentBooking.setStatus(Booking.Status.OCCUPIED);
            
            db.getTransaction().commit();
            logger.info("[DataAccess] Booking set successfully for member: " + name);
            
        } catch (Exception e) {
            if (db.getTransaction().isActive()) {
                 db.getTransaction().rollback();
            }
            logger.error("Error setting book: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Helper method to find a booking by court, date and hour.
     * @param court The court
     * @param date The date
     * @param hour The starting hour
     * @return The booking if found, null otherwise
     */
    private Booking findBookingByCourtDateHour(Court court, Date date, int hour) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1; // Calendar months are 0-based
        int day = cal.get(Calendar.DAY_OF_MONTH);
        
        String nativeQuery = "SELECT * FROM Booking b WHERE b.court_id = ? " +
                            "AND b.startingHour = ? " +
                            "AND YEAR(b.date) = ? " +
                            "AND MONTH(b.date) = ? " +
                            "AND DAY(b.date) = ?";
        
        @SuppressWarnings("unchecked")
        List<Booking> existingBookings = db.createNativeQuery(nativeQuery, Booking.class)
            .setParameter(1, court.getId())
            .setParameter(2, hour)
            .setParameter(3, year)
            .setParameter(4, month)
            .setParameter(5, day)
            .getResultList();
            
        return existingBookings.isEmpty() ? null : existingBookings.get(0);
    }

    /**
     * Finds bookings for a specific court on a specific date.
     * Note: Compares the date part only, ignoring time.
     * @param court The court entity.
     * @param date The date to search for.
     * @return A list of bookings for that court and date.
     */
    public List<Booking> getBookingsByCourtAndDate(Court court, Date date) {
        logger.info("[DataAccess] Fetching bookings for Court #: " + court.getNumber() + " on Date: " + date);

        try {
            if (db == null || !db.isOpen()) {
                logger.error("[DataAccess] EntityManager is not available. Cannot query bookings.");
                return Collections.emptyList();
            }
            
            db.getTransaction().begin();
            
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
            List<Booking> bookings = db.createNativeQuery(sql, Booking.class)
                .setParameter(1, court.getNumber())
                .setParameter(2, year)
                .setParameter(3, month)
                .setParameter(4, day)
                .getResultList();
            
            // Commit transaction
            db.getTransaction().commit();
            
            logger.info("[DataAccess] Found " + bookings.size() + " bookings for Court #" + court.getNumber() + " on " + date);
            
            // Debug output
            for (Booking b : bookings) {
                logger.info("[DataAccess]   - Booking: ID=" + b.getId() + 
                                   ", Hour=" + b.getStartingHour() + 
                                   ", Status=" + b.getStatus() + 
                                   ", Member=" + (b.getMember() != null ? b.getMember().getName() : "null"));
            }
            
            return bookings;
        } catch (Exception e) {
            logger.error("Error fetching bookings: " + e.getMessage());
            e.printStackTrace();
            if (db != null && db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            return Collections.emptyList();
        }
    }

    /**
     * Finds free bookings for a specific court on a specific date.
     * @param court The court entity.
     * @param date The date to search for.
     * @return A list of free bookings for that court and date.
     */
    public List<Booking> getFreeBookingsByCourtAndDate(Court court, Date date) {
        logger.info("Fetching FREE bookings for Court #: " + court.getNumber() + " on Date: " + date);

        try {
            if (db == null || !db.isOpen()) {
                logger.error("[DataAccess] EntityManager is not available. Cannot query bookings.");
                return Collections.emptyList();
            }
            
            db.getTransaction().begin();
            
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
            List<Booking> freeBookings = db.createNativeQuery(sql, Booking.class)
                .setParameter(1, court.getNumber())
                .setParameter(2, year)
                .setParameter(3, month)
                .setParameter(4, day)
                .getResultList();
            
            // Commit transaction
            db.getTransaction().commit();
            
            logger.info("[DataAccess] Found " + freeBookings.size() + " FREE bookings for Court #" + court.getNumber() + " on " + date);
            
            return freeBookings;
        } catch (Exception e) {
            logger.error("Error fetching free bookings: " + e.getMessage());
            e.printStackTrace();
            if (db != null && db.getTransaction().isActive()) {
                db.getTransaction().rollback();
            }
            return Collections.emptyList();
        }
    }
}
