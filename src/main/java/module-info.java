module bookcourt.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires jakarta.persistence;
    requires org.hibernate.orm.core;
    requires com.h2database;

    opens eus.ehu.brokenracket.ui to javafx.fxml, javafx.graphics;
    opens eus.ehu.brokenracket.businessLogic to jakarta.persistence;
    opens eus.ehu.brokenracket.configuration to jakarta.persistence;
    opens eus.ehu.brokenracket.dataAccess to jakarta.persist, org.hibernate.orm.core, javafx.base;
    opens eus.ehu.brokenracket.domain to jakarta.persistence, org.hibernate.orm.core, javafx.base;


    // If your MainUI class is in a different package, adjust the 'opens' directive
    // If you have other packages that need to be opened or exported, add them here
} 