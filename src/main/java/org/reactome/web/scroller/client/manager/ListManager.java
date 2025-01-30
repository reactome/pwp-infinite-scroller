package org.reactome.web.scroller.client.manager;

import com.google.gwt.http.client.Response;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.ListDataProvider;
import org.reactome.web.scroller.client.provider.InfiniteListAsyncDataProvider;

import java.util.List;

/**
 * This class maintains a list of all the items present in the InfiniteScrollList.
 *
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class ListManager<T> implements AsyncListManager<T> {
    private int totalRows = 0;
    private int curStartIndex = 0;
    private int curEndIndex = 0;
    private int pageSize = 0;

    private boolean reachedEndOfResults;

    private InfiniteListAsyncDataProvider<T> dataProvider;

    // This is used to keep all list items
    private final ListDataProvider<T> buffer = new ListDataProvider<>();

    public interface Handler {
        void onNewDataLoaded();

        void onPreviousDataLoaded();

        void onNextDataLoaded();

        void onLoading(boolean isLoading);

        void onError(String msg);

        void onNoResultsFound(String msg);
    }

    private final Handler handler;

    public ListManager(Handler handler, InfiniteListAsyncDataProvider<T> dataProvider) {
        this.handler = handler;
        setDataProvider(dataProvider);
    }

    public void clear() {
        buffer.getList().clear();
        totalRows = 0;
        curStartIndex = 0;
        curEndIndex = 0;

        reachedEndOfResults = false;
    }

    public void setDataDisplay(final HasData<T> listDisplay) {
        buffer.addDataDisplay(listDisplay);
    }

    public void setDataProvider(InfiniteListAsyncDataProvider<T> dataProvider) {
        this.dataProvider = dataProvider;
        this.dataProvider.setHandler(this);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public void loadNewData(int start, int length) {
        if (reachedEndOfResults) return;
        handler.onLoading(true);
        dataProvider.requestNewItems(start, length);
    }

    @Override
    public void onNewDataArrived(List<T> newItems, int start, int length) {
        if (newItems.size() != length) {
            // Reached the last page!
            reachedEndOfResults = true;
        }

        totalRows += newItems.size();

        if (curEndIndex == 0) {
            curEndIndex = totalRows - 1;
            addItemsToTailOfBuffer(newItems);
        } else {
            int newStart = Math.max(totalRows - pageSize, 0);
            int newLength = Math.min(pageSize, totalRows);

            updateHeadAndTailOfBuffer(newStart, newLength, newItems);
        }

        handler.onLoading(false);
        handler.onNewDataLoaded();
    }

    public void loadPreviousData() {
        int start = Math.max(curStartIndex - (pageSize / 2), 0); //15
        int length = Math.min(pageSize, totalRows);
        if (start < curStartIndex) {
            handler.onLoading(true);
            dataProvider.requestPreviousItems(start, length);
        }
    }

    @Override
    public void onPreviousDataArrived(List<T> newItems, int start, int length) {
        updateEntireBuffer(start, length, newItems);

        handler.onLoading(false);
        handler.onPreviousDataLoaded();
    }

    public void loadNextData() {
        int start = curEndIndex - (pageSize / 3); //10
        int length = Math.min(pageSize, (pageSize / 3) + totalRows - curEndIndex);

        handler.onLoading(true);
        dataProvider.requestNextItems(start, length);
    }

    @Override
    public void onNextDataArrived(List<T> newItems, int start, int length) {
        updateEntireBuffer(start, length, newItems);

        handler.onLoading(false);
        handler.onNextDataLoaded();
    }

    @Override
    public void onErrorRetrievingData(int code, String errorMsg) {
//        InfiniteScrollList._log("onErrorRetrievingData: " + code + " - " + errorMsg);
        switch (code) {
            case Response.SC_NOT_FOUND:
                handler.onNoResultsFound(errorMsg);
                break;
            default:
                handler.onError(errorMsg);
        }
    }

    @Override
    public void onErrorRetrievingData(String errorMsg) {
//        InfiniteScrollList._log("onErrorRetrievingData: " + errorMsg);
        handler.onError(errorMsg);
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getCurrentRows() {
        return buffer.getList().size();
    }

    public int getCurStartIndex() {
        return curStartIndex;
    }

    public int getCurEndIndex() {
        return curEndIndex;
    }

    public ListDataProvider<T> getBuffer() {
        return buffer;
    }

    /**
     * Adds the specified list of items at the tail of the list
     *
     * @param newItems
     */
    private void addItemsToTailOfBuffer(List<T> newItems) {
        buffer.getList().addAll(newItems);
    }

    /**
     * Removes the specified number of items from the head of the buffer
     *
     * @param numberOfItemsToRemove
     */
    private void removeItemsFromHeadOfBuffer(int numberOfItemsToRemove) {
        for (int i = 0; i < numberOfItemsToRemove; i++) {
            buffer.getList().remove(0);
        }
    }

    private void updateHeadAndTailOfBuffer(int start, int length, List<T> newItems) {
        int shift = start - curStartIndex;

        removeItemsFromHeadOfBuffer(shift);
        addItemsToTailOfBuffer(newItems);
        buffer.flush();

        curStartIndex += shift;
        curEndIndex = curStartIndex + length - 1;
    }

    private void updateEntireBuffer(int start, int length, List<T> newItems) {
        int shift = start - curStartIndex;

        buffer.getList().clear();
        buffer.getList().addAll(newItems);
        buffer.flush();

        curStartIndex += shift;
        curEndIndex = curStartIndex + length - 1;
    }

    @Override
    public String toString() {
        return " Rows: " + totalRows
                + " pageSize: " + pageSize
                + " buffer size: " + buffer.getList().size()
                + " Start: " + curStartIndex
                + " End: " + curEndIndex;
    }
}
