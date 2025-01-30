package org.reactome.web.scroller.client.provider;

import com.google.gwt.http.client.*;
import org.reactome.web.scroller.client.manager.AsyncListManager;

import java.util.ArrayList;
import java.util.List;

import static org.reactome.web.scroller.client.util.Placeholder.ROWS;
import static org.reactome.web.scroller.client.util.Placeholder.START;

/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public abstract class AbstractListAsyncDataProvider<T> implements InfiniteListAsyncDataProvider<T> {

    protected String url;
    protected Request request;
    private AsyncListManager<T> handler;

    private List<T> lastItemsToShow = null;
    private int sizeOfResults = Integer.MAX_VALUE;

    @Override
    public void setHandler(AsyncListManager<T> handler) {
        this.handler = handler;
    }

    @Override
    public void setURL(String url) {
        this.url = url;
    }

    @Override
    public final void requestNewItems(int start, int length) {
        try {
            requestItems(start, length, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            List<T> toShow = processResult(response.getText());
                            addExtraItems(toShow, start, length);
                            handler.onNewDataArrived(toShow, start, length);
                            break;
                        default:
                            if (lastItemsToShow != null && !lastItemsToShow.isEmpty()) {
                                List<T> extra = new ArrayList<>();
                                addExtraItems(extra, start, length);
                                handler.onNewDataArrived(extra, start, length);
                            } else {
                                handler.onErrorRetrievingData(response.getStatusCode(), processError(response));
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onErrorRetrievingData(exception.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onErrorRetrievingData(e.getMessage());
        }
    }

    @Override
    public final void requestNextItems(int start, int length) {
        try {
            requestItems(start, length, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            List<T> toShow = processResult(response.getText());
                            addExtraItems(toShow, start, length);
                            handler.onNextDataArrived(toShow, start, length);
                            break;
                        default:
                            if (lastItemsToShow != null && !lastItemsToShow.isEmpty()) {
                                List<T> extra = new ArrayList<>();
                                addExtraItems(extra, start, length);
                                handler.onNextDataArrived(extra, start, length);
                            } else {
                                handler.onErrorRetrievingData(response.getStatusCode(), processError(response));
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onErrorRetrievingData(exception.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onErrorRetrievingData(e.getMessage());
        }
    }

    @Override
    public final void requestPreviousItems(int start, int length) {
        try {
            requestItems(start, length, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            List<T> toShow = processResult(response.getText());
                            addExtraItems(toShow, start, length);
                            handler.onPreviousDataArrived(toShow, start, length);
                            break;
                        default:
                            if (lastItemsToShow != null && !lastItemsToShow.isEmpty()) {
                                List<T> extra = new ArrayList<>();
                                addExtraItems(extra, start, length);
                                handler.onPreviousDataArrived(extra, start, length);
                            } else {
                                handler.onErrorRetrievingData(response.getStatusCode(), processError(response));
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onErrorRetrievingData(exception.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onErrorRetrievingData(e.getMessage());
        }
    }

    protected void requestItems(int start, int length, RequestCallback callback) throws RequestException {

        String url = this.url
                .replace(START.getUrlValue(), "start=" + start)
                .replace(ROWS.getUrlValue(), "rows=" + length);

        if (request != null && request.isPending()) return;

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader("Accept", "application/json");

        request = requestBuilder.sendRequest(null, callback);
    }

    /***
     * This method should include the logic of instantiating the
     * objects (to be viewed) from the response body.
     *
     * @param body the body of the response
     * @return the list of objects to be shown by the list
     */
    protected abstract List<T> processResult(String body);

    protected String processError(Response response) {
        return response.getStatusText();
    }

    public void setExtraItemsToShow(List<T> extraItems) {
        this.lastItemsToShow = extraItems;
        this.sizeOfResults = Integer.MAX_VALUE;
    }


    private List<T> addExtraItems(List<T> toShow, int start, int length) {
        if (lastItemsToShow == null || lastItemsToShow.isEmpty()) return toShow;
        if (length != toShow.size() && sizeOfResults == Integer.MAX_VALUE) {
            sizeOfResults = start + toShow.size();
        }

        if ((start + length) > sizeOfResults) {
            int copyFrom = start + toShow.size() - sizeOfResults < 0 ? 0 : start + toShow.size() - sizeOfResults;
            int newLength = Math.min(length - toShow.size(), lastItemsToShow.size());
            int eIndex = copyFrom + newLength >= lastItemsToShow.size() ? lastItemsToShow.size() : copyFrom + newLength;

            for (int i = copyFrom; i < eIndex; i++) {
                toShow.add(lastItemsToShow.get(i));
            }
        }

        return toShow;
    }

}
