/*
 * Copyright (c) 2026. Arquitectura de Sistemas, DISC, UCN, Antofagasta.
 */
package cl.ucn.disc.arqsist.library.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/** A person who can borrow and reserve books. */
@DatabaseTable(tableName = "members")
public final class Member {

    /** Generated identifier of the member. */
    @DatabaseField(generatedId = true)
    private int id;

    /** Full name of the member. */
    @DatabaseField(canBeNull = false)
    private String name;

    /** Email address of the member. */
    @DatabaseField(canBeNull = false)
    private String email;

    /** Creates an empty member. ORMLite uses it to build members read from the database. */
    public Member() {
    }

    /**
     * Creates a member.
     *
     * @param name  full name of the member
     * @param email email address of the member
     */
    public Member(String name, String email) {
        this.name = name;
        this.email = email;
    }

    /**
     * Returns the identifier of the member.
     *
     * @return the identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the identifier of the member.
     *
     * @param id the identifier
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns the full name of the member.
     *
     * @return the full name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the full name of the member.
     *
     * @param name the full name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the email address of the member.
     *
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email address of the member.
     *
     * @param email the email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
