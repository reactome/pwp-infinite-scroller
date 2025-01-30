package org.reactome.web.scroller.client.manager;

import java.util.List;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public interface AsyncListManager<T> {

    void onNewDataArrived(List<T> newItems, int start, int length);

    void onPreviousDataArrived(List<T> newItems, int start, int length);

    void onNextDataArrived(List<T> newItems, int start, int length);

    void onErrorRetrievingData(int code, String errorMsg);

    void onErrorRetrievingData(String errorMsg);
}
