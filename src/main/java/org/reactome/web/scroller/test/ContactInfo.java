package org.reactome.web.scroller.test;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */

import com.google.gwt.view.client.ProvidesKey;

/**
 * Information about a contact.
 */
public class ContactInfo implements Comparable<ContactInfo> {

    /**
     * The key provider that provides the unique ID of a contact.
     */
    public static final ProvidesKey<ContactInfo> KEY_PROVIDER = new ProvidesKey<ContactInfo>() {
        @Override
        public Object getKey(ContactInfo item) {
            return item == null ? null : item.getId();
        }
    };

    public static int nextId = 0;

    private final int id;
    private final String title;
    private final String message;

    public ContactInfo(String title, String message) {
        this.id = nextId;
        nextId++;
        this.title = title;
        this.message = message;
    }

    @Override
    public int compareTo(ContactInfo o) {
        return (o == null || o.title == null) ? -1 : -o.title.compareTo(title);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ContactInfo) {
            return id == ((ContactInfo) o).id;
        }
        return false;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }
}
