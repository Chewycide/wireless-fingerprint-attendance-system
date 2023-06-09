package utility;

import java.sql.Date;

public class TempExportQueryData {
    private Date date_query;
    private String event_name;


    public Date getDateQuery() {
        return date_query;
    }

    public String getEventNameQuery() { return event_name; }


    public boolean buildDate(String date_string) {
        boolean validFormat = date_string.matches("^\\d{4}-((1[0-2])|(0?[1-9]))-\\d{1,2}$");
        if (validFormat) {
            this.date_query = Date.valueOf(date_string);
        }
        return  validFormat;
    }

    public void buildEventName(String event_name) {
        this.event_name = event_name;
    }
}
