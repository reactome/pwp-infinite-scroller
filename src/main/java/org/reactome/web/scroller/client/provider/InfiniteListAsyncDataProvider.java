package org.reactome.web.scroller.client.provider;

import org.reactome.web.scroller.client.manager.AsyncListManager;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public interface InfiniteListAsyncDataProvider<T> {

    void requestNewItems(int start, int length);

    void requestNextItems(int start, int length);

    void requestPreviousItems(int start, int length);

    void setHandler(AsyncListManager<T> handler);

    void setURL(String url);

}
