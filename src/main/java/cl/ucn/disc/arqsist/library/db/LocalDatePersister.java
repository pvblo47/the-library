/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.db;

import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;

import java.sql.SQLException;
import java.time.LocalDate;

/** ORMLite persister that stores a {@link LocalDate} as an ISO-8601 text column (for example {@code 2026-10-04}). */
public final class LocalDatePersister extends BaseDataType {

    /** The single shared instance, which ORMLite finds through {@link #getSingleton()}. */
    private static final LocalDatePersister SINGLETON = new LocalDatePersister();

    /** Creates the persister. It is private because ORMLite uses the singleton. */
    private LocalDatePersister() {
        super(SqlType.STRING, new Class<?>[]{LocalDate.class});
    }

    /**
     * Returns the shared instance. ORMLite calls this method by reflection for {@code persisterClass}.
     *
     * @return the singleton persister
     */
    public static LocalDatePersister getSingleton() {
        return SINGLETON;
    }

    /**
     * Returns the default value text unchanged, because the SQL type is already text.
     *
     * @param fieldType  field that has the default value
     * @param defaultStr default value text
     * @return the same text
     */
    @Override
    public Object parseDefaultString(FieldType fieldType, String defaultStr) {
        return defaultStr;
    }

    /**
     * Reads the column as text.
     *
     * @param fieldType field that is read
     * @param results   database results positioned on the current row
     * @param columnPos position of the column in the results
     * @return the text stored in the column
     * @throws SQLException if the column cannot be read
     */
    @Override
    public Object resultToSqlArg(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
        return results.getString(columnPos);
    }

    /**
     * Converts the stored text into a date.
     *
     * @param fieldType field that is read
     * @param sqlArg    text read from the column
     * @param columnPos position of the column in the results
     * @return the parsed {@link LocalDate}
     * @throws java.time.format.DateTimeParseException if the text is not an ISO-8601 date
     */
    @Override
    public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) {
        return LocalDate.parse((String) sqlArg);
    }

    /**
     * Converts a date into the text that is stored.
     *
     * @param fieldType  field that is written
     * @param javaObject the {@link LocalDate} to store
     * @return the ISO-8601 text of the date
     */
    @Override
    public Object javaToSqlArg(FieldType fieldType, Object javaObject) {
        return javaObject.toString();
    }
}
