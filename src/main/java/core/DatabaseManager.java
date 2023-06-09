package core;

import io.github.cdimascio.dotenv.Dotenv;
import utility.TempAttendanceData;
import utility.TempEnrollmentData;
import utility.TempExportQueryData;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The DatabaseManager class will handle the database communication and
 * execution of any SQL script.
 */
public class DatabaseManager {
    private static final String DB_USERNAME;
    private static final String DB_PASSWORD;

    static {
        Dotenv dotenv = Dotenv.load();
        DB_USERNAME = dotenv.get("DB_USERNAME");
        DB_PASSWORD = dotenv.get("DB_PASSWORD");
    }


    /**
     * Open a connection to the database.
     * <p>
     *     NOTE: The connection object should be closed using the method
     *     {@link #closeThis(Connection)}.
     * </p>
     * @return a connection object
     * @throws SQLException if a database access error or the url is null.
     */
    public Connection openConnection() throws SQLException {
        Connection connection = null;
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:postgresql://localhost/attendance_logger",
                    DB_USERNAME,
                    DB_PASSWORD
            );
        }
        catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
        return connection;
    }

    /**
     * Closes a non-null connection.
     * @param connection Closable connection
     */
    public void closeThis(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            }
            catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }


    /**
     * Overloaded method of {@link #closeThis(Connection)}.
     * @param resultSet Closable result set
     */
    public void closeThis(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            }
            catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }


    /**
     * Overloaded method of {@link #closeThis(Connection)}.
     * @param statement Closable statement
     */
    public void closeThis(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            }
            catch (SQLException sqle) {
                sqle.printStackTrace();
            }
        }
    }


    /**
     * Create the necessary tables. If the table already exists then the
     * SQL command will not execute as stated in the script "IF NOT EXISTS".
     */
    public boolean initTables() {
        Connection connection = null;
        Statement stmt = null;
        boolean isSuccessful = true;
        try {
            connection = openConnection();
            stmt = connection.createStatement();

            String create_users_t = "CREATE TABLE IF NOT EXISTS users (" +
                    "user_id serial NOT NULL PRIMARY KEY, " +
                    "full_name text NOT NULL, " +
                    "fingerprint_id integer NOT NULL, " +
                    "client_id text NOT NULL," +
                    "UNIQUE (fingerprint_id)" +
                    ")";

            String create_user_info_t = "CREATE TABLE IF NOT EXISTS user_info (" +
                    "user_id integer NOT NULL, " +
                    "first_name text, " +
                    "middle_name text, " +
                    "last_name text, " +
                    "age smallint, " +
                    "gender text, " +
                    "phone_number text, " +
                    "address text, " +
                    "CONSTRAINT FK_INFO_USER " +
                    "FOREIGN KEY (user_id) " +
                    "REFERENCES users (user_id) " +
                    "ON DELETE CASCADE " +
                    "ON UPDATE CASCADE" +
                    ") ";

            String create_attendance_t = "CREATE TABLE IF NOT EXISTS attendance (" +
                    "attendance_id serial NOT NULL PRIMARY KEY, " +
                    "user_id integer NOT NULL, " +
                    "date_attended date NOT NULL, " +
                    "time_attended time NOT NULL, " +
                    "event_name text, " +
                    "event_location text, " +
                    "CONSTRAINT FK_ATTENDANCE_USER " +
                    "FOREIGN KEY (user_id) " +
                    "REFERENCES users (user_id) " +
                    "ON DELETE CASCADE " +
                    "ON UPDATE CASCADE" +
                    ") ";

            stmt.addBatch(create_users_t);
            stmt.addBatch(create_user_info_t);
            stmt.addBatch(create_attendance_t);

            int[] create_table_feedback = stmt.executeBatch();
            for (int feedback : create_table_feedback) {
                isSuccessful = feedback != Statement.EXECUTE_FAILED;
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
            isSuccessful = false;
        }
        finally {
            closeThis(stmt);
            closeThis(connection);
        }
        return isSuccessful;
    }


    /**
     * @param enrollee_data A temporary data object: {@link TempEnrollmentData}
     * @return true if enrollment record is successfully created within the database,
     * otherwise false.
     */
    public boolean enrollUser(TempEnrollmentData enrollee_data) {
        boolean isSuccessful = true;
        Connection connection = null;
        PreparedStatement users_stmt = null;
        PreparedStatement get_user_id_stmt = null;
        ResultSet result = null;
        PreparedStatement user_info_stmt = null;
        try {
            connection = openConnection();

            // Inserting record to the users table.
            String users_record = "INSERT INTO users (" +
                    "full_name, " +
                    "fingerprint_id," +
                    "client_id) " +
                    "VALUES (?, ?, ?)";
            users_stmt = connection.prepareStatement(users_record);
            users_stmt.setString(1, enrollee_data.getFullName());
            users_stmt.setInt(2, enrollee_data.getFingerprintId());
            users_stmt.setString(3, enrollee_data.getClientID());

            users_stmt.executeUpdate();


            // get the user_id of the recently added user in the users table.
            String get_user_id = "SELECT user_id FROM users " +
                    "WHERE full_name = ? " +
                    "AND fingerprint_id = ? " +
                    "AND client_id = ?";
            get_user_id_stmt = connection.prepareStatement(get_user_id);
            get_user_id_stmt.setString(1, enrollee_data.getFullName());
            get_user_id_stmt.setInt(2, enrollee_data.getFingerprintId());
            get_user_id_stmt.setString(3, enrollee_data.getClientID());
            result = get_user_id_stmt.executeQuery();
            int user_id = 0;
            if (result.next()) {
                user_id = result.getInt("user_id");
            }


            // Inserting record to the user_info table.
            String user_info_record = "INSERT INTO user_info (" +
                    "user_id, " +
                    "first_name, " +
                    "middle_name, " +
                    "last_name, " +
                    "age, " +
                    "gender, " +
                    "phone_number, " +
                    "address) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            user_info_stmt = connection.prepareStatement(user_info_record);
            user_info_stmt.setInt(1, user_id);
            user_info_stmt.setString(2, enrollee_data.getFirstName());
            user_info_stmt.setString(3, enrollee_data.getMiddleName());
            user_info_stmt.setString(4, enrollee_data.getLastName());
            user_info_stmt.setShort(5,  enrollee_data.getAge());
            user_info_stmt.setString(6, enrollee_data.getGender());
            user_info_stmt.setString(7, enrollee_data.getPhoneNumber());
            user_info_stmt.setString(8, enrollee_data.getAddress());

            user_info_stmt.executeUpdate();
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
            isSuccessful = false;
        }
        finally {
            closeThis(users_stmt);
            closeThis(get_user_id_stmt);
            closeThis(result);
            closeThis(user_info_stmt);
            closeThis(connection);
        }
        return isSuccessful;
    }


    public boolean recordAttendance(TempAttendanceData attendance_data) {
        Connection connection = null;
        PreparedStatement find_userID_stmt = null;
        PreparedStatement find_userFN_stmt = null;
        PreparedStatement record_attendance_stmt = null;
        ResultSet users_result = null;
        ResultSet user_info_result = null;

        boolean isSuccessful = true;
        try {
            connection = openConnection();

            // Find a users record based on the fingerprint_id.
            String find_userID_script = "SELECT user_id FROM users " +
                    "WHERE fingerprint_id = ? " +
                    "AND client_id = ?";
            find_userID_stmt = connection.prepareStatement(find_userID_script);
            find_userID_stmt.setInt(1, attendance_data.getFingerprintID());
            find_userID_stmt.setString(2, attendance_data.getClientID());
            users_result = find_userID_stmt.executeQuery();

            int user_id = attendance_data.getFingerprintID();
            if (users_result.next()) {
                user_id = users_result.getInt("user_id");
            }

            if (user_id != 0 && !checkAttendanceNowExists(user_id, attendance_data.getDateNow())) {
                // Query the first_name of the attendee in the user_info table based on user_id.
                String find_userFN_script = "SELECT first_name FROM user_info " +
                        "WHERE user_id = ?";
                find_userFN_stmt = connection.prepareStatement(find_userFN_script);
                find_userFN_stmt.setInt(1, user_id);
                user_info_result = find_userFN_stmt.executeQuery();
                String attendee_first_name;

                if (user_info_result.next()) {
                    attendee_first_name = user_info_result.getString("first_name");
                    attendance_data.setFirstName(attendee_first_name);
                }

                // Record the attendance.
                String record_attendance_script = "INSERT INTO attendance (" +
                        "user_id, " +
                        "date_attended, " +
                        "time_attended, " +
                        "event_name, " +
                        "event_location) " +
                        "VALUES (?, ?, ?, ?, ?)";
                record_attendance_stmt = connection.prepareStatement(record_attendance_script);
                record_attendance_stmt.setInt(1, user_id);
                record_attendance_stmt.setDate(2, attendance_data.getDateNow());
                record_attendance_stmt.setTime(3, attendance_data.getTimeNow());
                record_attendance_stmt.setString(4, attendance_data.getEventName());
                record_attendance_stmt.setString(5, attendance_data.getEventLocation());
                record_attendance_stmt.executeUpdate();
            }
            else {
                isSuccessful = false;
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
            isSuccessful = false;
        }
        finally {
            closeThis(find_userID_stmt);
            closeThis(find_userFN_stmt);
            closeThis(record_attendance_stmt);
            closeThis(users_result);
            closeThis(user_info_result);
            closeThis(connection);
        }

        return isSuccessful;
    }


    public boolean deleteUserRecords(int user_id) {
        boolean isSuccessful = true;
        Connection connection = null;
        PreparedStatement del_stmt = null;
        try {
            connection = openConnection();

            String del_users_script = "DELETE FROM users " +
                    "WHERE user_id = ?";
            del_stmt = connection.prepareStatement(del_users_script);
            del_stmt.setInt(1, user_id);
            del_stmt.executeUpdate();

            String del_info_script = "DELETE FROM user_info " +
                    "WHERE user_id = ?";
            del_stmt = connection.prepareStatement(del_info_script);
            del_stmt.setInt(1, user_id);
            del_stmt.executeUpdate();

            String del_attendance_script = "DELETE FROM attendance " +
                    "WHERE user_id = ?";
            del_stmt = connection.prepareStatement(del_attendance_script);
            del_stmt.setInt(1, user_id);
            del_stmt.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
            isSuccessful = false;
        }
        finally {
            closeThis(del_stmt);
            closeThis(connection);
        }
        return isSuccessful;
    }


    public List<String> queryAttendanceByDate(TempExportQueryData export_data) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet date_query_result = null;
        List<String> data = null;
        try {
            connection = openConnection();

            String find_rbd_script = "SELECT * FROM attendance " +
                    "WHERE date_attended = ?";
            stmt = connection.prepareStatement(find_rbd_script);
            stmt.setDate(1, export_data.getDateQuery());
            date_query_result = stmt.executeQuery();

            data = new ArrayList<>();
            data.add("Attendee Name, Date Attended, Time Attended, Event Name, Event Location");
            if (!date_query_result.next()) {
                data.add("NO RESULTS FROM SPECIFIED DATE, 0, 0, 0, 0");
            }
            else {
                do {
                    String row_data;
                    int user_id = date_query_result.getInt("user_id");
                    Date date_attended = date_query_result.getDate("date_attended");
                    Time time_attended = date_query_result.getTime("time_attended");
                    String event_name = date_query_result.getString("event_name");
                    String event_loc = date_query_result.getString("event_location");

                    // query the full name from the users table.
                    String user_query_script = "SELECT full_name FROM users " +
                            "WHERE user_id = ?";
                    PreparedStatement user_query_stmt = connection.prepareStatement(user_query_script);
                    user_query_stmt.setInt(1, user_id);
                    ResultSet user_query_result = user_query_stmt.executeQuery();

                    String full_name;
                    if (user_query_result.next()) {
                        full_name = user_query_result.getString("full_name");
                    }
                    else {
                        full_name = "NO USER FOUND";
                    }

                    closeThis(user_query_stmt);
                    closeThis(user_query_result);

                    row_data = String.format(
                            "%s, %s, %s, %s, %s",
                            full_name,
                            date_attended,
                            time_attended,
                            event_name,
                            event_loc
                    );

                    data.add(row_data);
                }
                while (date_query_result.next());
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(stmt);
            closeThis(date_query_result);
            closeThis(connection);
        }
        return data;
    }


    public List<String> queryAttendanceByEventName(TempExportQueryData exportData) {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet event_query_result = null;
        List<String> data = null;
        try {
            connection = openConnection();

            String find_event_script = "SELECT * FROM attendance " +
                    "WHERE event_name = ?";
            stmt = connection.prepareStatement(find_event_script);
            stmt.setString(1, exportData.getEventNameQuery());
            event_query_result = stmt.executeQuery();

            data = new ArrayList<>();
            data.add("Attendee Name, Date Attended, Time Attended, Event Name, Event Location");
            if (!event_query_result.next()) {
                data.add("NO RESULTS FROM SPECIFIED EVENT, 0, 0, 0, 0");
            }
            else {
                do {
                    String row_data;
                    int user_id = event_query_result.getInt("user_id");
                    Date date_attended = event_query_result.getDate("date_attended");
                    Time time_attended = event_query_result.getTime("time_attended");
                    String event_name = event_query_result.getString("event_name");
                    String event_loc = event_query_result.getString("event_location");

                    // query the full name from the users table.
                    String user_query_script = "SELECT full_name FROM users " +
                            "WHERE user_id = ?";
                    PreparedStatement user_query_stmt = connection.prepareStatement(user_query_script);
                    user_query_stmt.setInt(1, user_id);
                    ResultSet user_query_result = user_query_stmt.executeQuery();

                    String full_name;
                    if (user_query_result.next()) {
                        full_name = user_query_result.getString("full_name");
                    } else {
                        full_name = "NO USER FOUND";
                    }

                    closeThis(user_query_stmt);
                    closeThis(user_query_result);

                    row_data = String.format(
                            "%s, %s, %s, %s, %s",
                            full_name,
                            date_attended,
                            time_attended,
                            event_name,
                            event_loc
                    );

                    data.add(row_data);
                }
                while (event_query_result.next());
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(stmt);
            closeThis(event_query_result);
            closeThis(connection);
        }
        return data;
    }


    public List<String> queryAllUsers() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet user_query_result = null;
        List<String> data = null;
        try {
            connection = openConnection();

            String find_users_script = "SELECT * FROM users";
            stmt = connection.prepareStatement(find_users_script);
            user_query_result = stmt.executeQuery();

            data = new ArrayList<>();
            data.add("USERS ENROLLED");
            if (!user_query_result.next()) {
                data.add("NO ENROLLED USERS");
            }
            else {
                do {
                    String full_name = user_query_result.getString("full_name");
                    String row_data = String.format(
                            "%s",
                            full_name
                    );
                    data.add(row_data);
                }
                while (user_query_result.next());
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(stmt);
            closeThis(user_query_result);
            closeThis(connection);
        }
        return data;
    }


    public List<String> queryAllAttendanceData() {
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet attendance_query_result = null;
        List<String> data = null;
        try {
            connection = openConnection();

            String find_attendance_script = "SELECT * FROM attendance";
            stmt = connection.prepareStatement(find_attendance_script);
            attendance_query_result = stmt.executeQuery();

            data =  new ArrayList<>();
            data.add("Attendee Name, Date Attended, Time Attended, Event Name, Event Location");

            if (!attendance_query_result.next()) {
                data.add("NO ATTENDANCE DATA, 0, 0, 0, 0");
            }
            else {
                do {
                    String row_data;
                    int user_id = attendance_query_result.getInt("user_id");
                    Date date_attended = attendance_query_result.getDate("date_attended");
                    Time time_attended = attendance_query_result.getTime("time_attended");
                    String event_name = attendance_query_result.getString("event_name");
                    String event_loc = attendance_query_result.getString("event_location");

                    // Query the full name from the users table.
                    String user_query_script = "SELECT full_name FROM users " +
                            "WHERE user_id = ?";
                    PreparedStatement user_query_stmt = connection.prepareStatement(user_query_script);
                    user_query_stmt.setInt(1, user_id);
                    ResultSet user_query_result = user_query_stmt.executeQuery();

                    String full_name;
                    if (user_query_result.next()) {
                        full_name = user_query_result.getString("full_name");
                    } else {
                        full_name = "NO USER FOUND";
                    }

                    closeThis(user_query_stmt);
                    closeThis(user_query_result);

                    row_data = String.format(
                            "%s, %s, %s, %s, %s",
                            full_name,
                            date_attended,
                            time_attended,
                            event_name,
                            event_loc
                    );

                    data.add(row_data);
                }
                while (attendance_query_result.next());
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } finally {
            closeThis(stmt);
            closeThis(attendance_query_result);
            closeThis(connection);
        }
        return data;
    }


    public boolean checkFingerIDExists(int fingerprint_id, String client_id) {
        boolean idExists = false;
        Connection connection = null;
        PreparedStatement query_stmt = null;
        ResultSet result_set = null;
        try {
            connection = openConnection();
            String query_script = "SELECT * FROM users " +
                    "WHERE fingerprint_id = ? " +
                    "AND client_id = ?";
            query_stmt = connection.prepareStatement(query_script);
            query_stmt.setInt(1, fingerprint_id);
            query_stmt.setString(2, client_id);
            result_set = query_stmt.executeQuery();

            if (result_set.next()) {
                idExists = true;
            }

        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(query_stmt);
            closeThis(result_set);
            closeThis(connection);
        }
        return idExists;
    }


    public int getUserID(int fingerprint_id, String client_id) {
        int user_id = 0;
        Connection connection = null;
        PreparedStatement query_stmt = null;
        ResultSet result_set = null;
        try {
            connection = openConnection();
            String query_script = "SELECT user_id FROM users " +
                    "WHERE fingerprint_id = ? " +
                    "AND client_id = ?";
            query_stmt = connection.prepareStatement(query_script);
            query_stmt.setInt(1, fingerprint_id);
            query_stmt.setString(2, client_id);
            result_set = query_stmt.executeQuery();

            if (result_set.next()) {
                user_id = result_set.getInt("user_id");
            }

        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(query_stmt);
            closeThis(result_set);
            closeThis(connection);
        }
        return user_id;
    }


    public boolean checkAttendanceNowExists(int user_id, Date date_now) {
        boolean alreadyExists = false;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            connection = openConnection();
            String query_script = "SELECT * FROM attendance " +
                    "WHERE user_id = ? " +
                    "AND date_attended = ?";
            stmt = connection.prepareStatement(query_script);
            stmt.setInt(1, user_id);
            stmt.setDate(2, date_now);
            result = stmt.executeQuery();

            if (result.next()) {
                alreadyExists = true;
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
            alreadyExists = true;
        }
        finally {
            closeThis(stmt);
            closeThis(result);
            closeThis(connection);
        }
        return alreadyExists;
    }


    public boolean tableExist(String tableName) {
        boolean tExists = false;
        Connection connection = null;
        ResultSet result = null;
        try {
            connection = openConnection();
            result = connection.getMetaData().getTables(null, null, tableName, null);
            if (result.next()) {
                tExists = true;
            }
        }
        catch (SQLException sqle) {
            sqle.printStackTrace();
        }
        finally {
            closeThis(result);
            closeThis(connection);
        }
        return tExists;
    }
}
