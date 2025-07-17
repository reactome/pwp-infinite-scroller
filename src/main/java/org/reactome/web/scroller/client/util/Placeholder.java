package org.reactome.web.scroller.client.util;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public enum Placeholder {
    START("##START##"),
    ROWS("##ROWS##");

    private final String urlValue;

    Placeholder(String urlValue) {
        this.urlValue = urlValue;
    }

    public String getUrlValue() {
        return urlValue;
    }
}
