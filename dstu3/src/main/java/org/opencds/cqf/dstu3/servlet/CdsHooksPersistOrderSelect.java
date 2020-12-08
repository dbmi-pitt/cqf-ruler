package org.opencds.cqf.dstu3.servlet;

import com.google.gson.Gson;
import org.opencds.cqf.cds.response.CdsCard;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

public class CdsHooksPersistOrderSelect {

    private Connection connection;

    public CdsHooksPersistOrderSelect() {
        this.connection = null;
    }


    public Boolean persistOrderSelectRequestData(String orderingPhys, String patient, String encounter, String medicationId, String medicationObject, String knowledgeArtifactUrl, List<CdsCard> cdsCardList) {
        try {
            // create a database connection
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.out.println("ERROR: can't find the sqlite JDBC");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:order-select-store.db");

            Statement statement = connection.createStatement();
            statement.setQueryTimeout(3);  // set timeout to 3 sec.

            // create the table if it doesn't exist.
            statement.executeUpdate("create table if not exists order_select_data (order_select_id integer PRIMARY KEY, ordering_phys string, patient string, encounter string, medication_id string, medication_object string, ka_url string, UNIQUE (ordering_phys, patient, encounter, medication_id, medication_object, ka_url))");
            statement.executeUpdate("create table if not exists order_select_cds_card_data (cds_card_object string, order_select_id integer)");

            //Base 64 encode medication
            String encodedMedication = null;
            if (medicationObject != null)
                encodedMedication = Base64.getEncoder().encodeToString(medicationObject.getBytes());
            // persist the data from the order select request
            String q = "insert into order_select_data (ordering_phys, patient, encounter, medication_id, medication_object, ka_url) values('" + orderingPhys + "','" + patient + "','" + encounter + "','" + medicationId + "','" + encodedMedication + "','" + knowledgeArtifactUrl + "')";
            System.out.println("Query to SQL Lite: " + q);

            statement.executeUpdate(q);

            //Add each CDS card to DB
            int primaryKey = -1;
            ResultSet rs = statement.getGeneratedKeys();
            if (rs.next()) {
                primaryKey = rs.getInt(1);
                Gson gson = new Gson();

                for (CdsCard card : cdsCardList) {
                    //We don't need to save the entire card, just the summary, detail, and indicator
                    String relevantCard = card.getSummary() + card.getDetail() + card.getIndicator().toString();
                    String jsonCard = gson.toJson(relevantCard);
                    String encodedCard = Base64.getEncoder().encodeToString(jsonCard.getBytes());
                    q = "insert into order_select_cds_card_data values('" + encodedCard + "','" + primaryKey + "')";
                    statement.executeUpdate(q);
                }
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
            return false;
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        return true;
    }


    //return the primary key of the row that matches the given attributes, if -1 is returned then no match
    public int testForOrderSelectRequestData(String orderingPhys, String patient, String encounter, String medicationId, String knowledgeArtifactUrl) {

        int primaryKey = -1;

        try {
            // create a database connection
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.out.println("ERROR: can't find the sqlite JDBC");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:order-select-store.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(3);  // set timeout to 3 sec.

            String q = "select * from order_select_data where ordering_phys = '" + orderingPhys +
                    "' and patient = '" + patient +
                    "' and encounter = '" + encounter +
                    "' and medication_id = '" + medicationId +
                    "' and ka_url = '" + knowledgeArtifactUrl + "'";
            System.out.println("DEBUG: SQL Lite query: " + q);

            ResultSet rs = statement.executeQuery(q);
            if (rs.next()) {
                primaryKey = rs.getInt("order_select_id");
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        return primaryKey;
    }

    public List<CdsCard> updateCdsCards(int primaryKey, List<CdsCard> cdsCards) {
        try {
            // create a database connection
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                System.out.println("ERROR: can't find the sqlite JDBC");
            }
            connection = DriverManager.getConnection("jdbc:sqlite:order-select-store.db");
            Statement statement = connection.createStatement();
            statement.setQueryTimeout(3);  // set timeout to 3 sec.

            String q = "select * from order_select_cds_card_data where order_select_id = '" + primaryKey + "'";
            System.out.println("DEBUG: SQL Lite query: " + q);
            Gson gson = new Gson();

            ResultSet rs = statement.executeQuery(q);
            while (rs.next()) {
                String decodedString = new String(Base64.getDecoder().decode(rs.getString("cds_card_object")));
                Iterator<CdsCard> i = cdsCards.iterator();
                while (i.hasNext()) {
                    CdsCard card = i.next();
                    String relevantCard = card.getSummary() + card.getDetail() + card.getIndicator().toString();
                    String jsonString = gson.toJson(relevantCard);
                    if (jsonString.equals(decodedString)) {
                        i.remove();
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.getMessage());
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return cdsCards;
    }
}
