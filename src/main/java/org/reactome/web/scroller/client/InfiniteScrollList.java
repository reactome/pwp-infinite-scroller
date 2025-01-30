package org.reactome.web.scroller.client;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardPagingPolicy;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import org.reactome.web.scroller.client.manager.ListManager;
import org.reactome.web.scroller.client.provider.InfiniteListAsyncDataProvider;


/**
 * A list that provides infinite scroll capability.
 * As the user scrolls up or down new data are requested
 * and added in the wrapped CellList
 *
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class InfiniteScrollList<T> extends LayoutPanel implements ListManager.Handler {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_DATA_INCREMENT = DEFAULT_PAGE_SIZE / 2;
    public static final int DEFAULT_ITEM_HEIGHT = 45;

    private static final String DEFAULT_LOADING = "Loading...";
    private static final String DEFAULT_NO_RESULTS = "No results found";

    private static final boolean isFirefox = isFirefox();

    // The last scroll position
    private int lastScrollPos = 0;
    private int curStartIndex = 0;
    private int curEndIndex = 0;

    private int pageSize = DEFAULT_PAGE_SIZE;
    private final int dataIncrement = DEFAULT_DATA_INCREMENT;

    private int listWindowHeight = 0;
    private final int rowSize = DEFAULT_ITEM_HEIGHT;

    private final ListManager<T> listManager;
    private final CellList<T> display;

    private final ScrollPanel scrollable = new ScrollPanel();

    // Used at the beginning of the list to artificially increase the size of the scrollpanel
    private final SimplePanel offsetStartPanel;

    // Used at the end of the list to artificially increase the size of the scrollpanel
    private final SimplePanel offsetEndPanel;

    private final SimplePanel loadingPanel;

    private final SimplePanel noResultsPanel;
    private final HTML noResultsLabel;

    private final SimplePanel errorPanel;
    private final Label errorLabel;

    private boolean isLoading;

    private final HandlerRegistration handlerRegistration;

    public InfiniteScrollList(final Cell<T> cell, ProvidesKey<T> keyProvider, InfiniteListAsyncDataProvider<T> dataProvider) {
        this(cell, keyProvider, dataProvider, null);
    }

    public InfiniteScrollList(final Cell<T> cell, ProvidesKey<T> keyProvider, InfiniteListAsyncDataProvider<T> dataProvider, CellList.Resources resources) {
        setStyleName(RESOURCES.getCSS().scrollable());

        display = resources == null ? new CellList<>(cell, keyProvider) : new CellList<>(cell, resources, keyProvider);
        display.setPageSize(pageSize);
        display.setKeyboardPagingPolicy(HasKeyboardPagingPolicy.KeyboardPagingPolicy.INCREASE_RANGE);
        display.setKeyboardSelectionPolicy(HasKeyboardSelectionPolicy.KeyboardSelectionPolicy.DISABLED);

        listManager = new ListManager<>(this, dataProvider);
        listManager.setDataDisplay(display);
        listManager.setPageSize(pageSize);

        offsetStartPanel = new SimplePanel();
        offsetStartPanel.setStyleName(RESOURCES.getCSS().offsetDiv());

        offsetEndPanel = new SimplePanel();
        offsetEndPanel.setStyleName(RESOURCES.getCSS().offsetDiv());

        loadingPanel = new SimplePanel();
        loadingPanel.add(new Label(DEFAULT_LOADING));
        loadingPanel.setStyleName(RESOURCES.getCSS().loadingInfo());

        noResultsLabel = new HTML();

        noResultsPanel = new SimplePanel();
        noResultsPanel.add(noResultsLabel);
        noResultsPanel.setStyleName(RESOURCES.getCSS().noResults());

        errorLabel = new Label();
        errorLabel.addClickHandler(e -> {
            enableScrolling();
            clearError();
        });
        errorPanel = new SimplePanel();
        errorPanel.setTitle("Click to retry");
        errorPanel.add(errorLabel);
        errorPanel.setStyleName(RESOURCES.getCSS().errorInfo());


        FlowPanel container = new FlowPanel();
        container.add(offsetStartPanel);
        container.add(offsetEndPanel);
        container.insert(display, 1);

        scrollable.setWidget(container);

        // Do not let the scrollable take tab focus.
        scrollable.getElement().setTabIndex(-1);

        add(scrollable);
        add(loadingPanel);
        add(noResultsPanel);
        add(errorPanel);

        setWidgetLeftRight(scrollable, 0, Style.Unit.PX, 0, Style.Unit.PX);
        setWidgetTopBottom(scrollable, 0, Style.Unit.PX, 0, Style.Unit.PX);

        setWidgetLeftRight(loadingPanel, 70, Style.Unit.PCT, 0, Style.Unit.PX);
        setWidgetTopHeight(loadingPanel, -11, Style.Unit.PX, 10, Style.Unit.PX);

        setWidgetLeftRight(noResultsPanel, 0, Style.Unit.PCT, 0, Style.Unit.PX);
        setWidgetTopHeight(noResultsPanel, -51, Style.Unit.PX, 50, Style.Unit.PX);

        setWidgetLeftRight(errorPanel, 10, Style.Unit.PCT, 10, Style.Unit.PCT);
        setWidgetTopHeight(errorPanel, -11, Style.Unit.PX, 10, Style.Unit.PX);

        // Handle scroll events.
        handlerRegistration = scrollable.addScrollHandler(event -> {

            lastScrollPos = scrollable.getVerticalScrollPosition();

            if (display == null) {
                return;
            }

            curStartIndex = listManager.getCurStartIndex();
            curEndIndex = listManager.getCurEndIndex();

            if (lastScrollPos != 0 && lastScrollPos <= curStartIndex * DEFAULT_ITEM_HEIGHT) {
                listManager.loadPreviousData();
            } else if (lastScrollPos >= (((curEndIndex) * DEFAULT_ITEM_HEIGHT) - scrollable.getOffsetHeight())) {
                if (curEndIndex >= listManager.getTotalRows() - 1) {
                    // Requires expanding the rows with new data if available
                    listManager.loadNewData(listManager.getTotalRows(), dataIncrement);
                } else {
                    listManager.loadNextData();
                }
            }

        });

        Scheduler.get().scheduleDeferred(() -> listWindowHeight = scrollable.getElement().getOffsetHeight());
    }

    public void loadFirstPage() {
        if (listManager.getTotalRows() == 0) {
            listManager.loadNewData(0, pageSize);
        }
    }

    @Override
    public void onNewDataLoaded() {
        clearEmptyListMessage();
        display.setVisibleRange(0, listManager.getCurrentRows());
        updateOffsetPanelHeights();

        if (isFirefox) {
            scrollable.setVerticalScrollPosition(lastScrollPos);
        } else {
            scrollable.setVerticalScrollPosition((((curEndIndex) * DEFAULT_ITEM_HEIGHT) - scrollable.getOffsetHeight()));
        }

        adjustVerticalPosition();
    }

    @Override
    public void onPreviousDataLoaded() {
        clearEmptyListMessage();
        updateOffsetPanelHeights();

        if (isFirefox) {
            scrollable.setVerticalScrollPosition(lastScrollPos);
        } else {
            scrollable.setVerticalScrollPosition(curStartIndex * DEFAULT_ITEM_HEIGHT);
        }

        adjustVerticalPosition();
    }

    @Override
    public void onNextDataLoaded() {
        clearEmptyListMessage();
        updateOffsetPanelHeights();

        if (isFirefox) {
            scrollable.setVerticalScrollPosition(lastScrollPos);
        } else {
            scrollable.setVerticalScrollPosition((((curEndIndex) * DEFAULT_ITEM_HEIGHT) - scrollable.getOffsetHeight()));
        }

        adjustVerticalPosition();
    }

    public void setPageSize(int newPageSize) {
        this.pageSize = newPageSize;
        display.setPageSize(newPageSize);

        listManager.clear();
        listManager.setPageSize(newPageSize);

        offsetStartPanel.setHeight(listManager.getCurStartIndex() * DEFAULT_ITEM_HEIGHT + "px");
        offsetEndPanel.setHeight((listManager.getTotalRows() - (listManager.getCurStartIndex() + listManager.getCurrentRows())) * DEFAULT_ITEM_HEIGHT + "px");

        scrollable.setVerticalScrollPosition(1);

    }

    public void setSelectionModel(SelectionModel<? super T> selectionModel) {
        display.setSelectionModel(selectionModel);
    }


    public CellList<T> getDisplay() {
        return display;
    }

    private void updateOffsetPanelHeights() {
        offsetStartPanel.setHeight(listManager.getCurStartIndex() * DEFAULT_ITEM_HEIGHT + "px");
        offsetEndPanel.setHeight((listManager.getTotalRows() - (listManager.getCurStartIndex() + listManager.getCurrentRows())) * DEFAULT_ITEM_HEIGHT + "px");
    }

    private void adjustVerticalPosition() {
        if (scrollable.getVerticalScrollPosition() < offsetStartPanel.getElement().getOffsetHeight()) {
            offsetStartPanel.setHeight(scrollable.getVerticalScrollPosition() + "px");
        }
    }

    @Override
    public void onLoading(boolean isLoading) {
        if (isLoading) {
            disableScrolling();
            clearEmptyListMessage();
            clearError();
            setWidgetTopHeight(loadingPanel, 1, Style.Unit.PX, 10, Style.Unit.PX);
        } else {
            enableScrolling();
            setWidgetTopHeight(loadingPanel, -11, Style.Unit.PX, 10, Style.Unit.PX);
        }
        this.isLoading = isLoading;
    }

    @Override
    public void onError(String msg) {
        showError(msg);
    }

    @Override
    public void onNoResultsFound(String msg) {
        showEmptyListMessage(msg);
        clearAnySelection();
    }

    private void showError(String msg) {
        if (isLoading) {
            onLoading(false);
        }
        clearEmptyListMessage();
        disableScrolling();
        errorLabel.setText(msg);
        setWidgetTopHeight(errorPanel, 1, Style.Unit.PX, 10, Style.Unit.PX);
    }

    private void clearError() {
        setWidgetTopHeight(errorPanel, -11, Style.Unit.PX, 10, Style.Unit.PX);
        errorLabel.setText("");
    }

    private void showEmptyListMessage(String msg) {
        if (isLoading) {
            onLoading(false);
        }

        clearError();
        disableScrolling();
        noResultsLabel.setHTML(msg == null || msg.isEmpty() ? DEFAULT_NO_RESULTS : msg);
        setWidgetTopHeight(noResultsPanel, 1, Style.Unit.PX, 50, Style.Unit.PX);
    }

    private void clearAnySelection() {
        SelectionModel selectionModel = getDisplay().getSelectionModel();
        if (selectionModel instanceof SingleSelectionModel) {
            ((SingleSelectionModel) selectionModel).clear();
        }
    }

    private void clearEmptyListMessage() {
        setWidgetTopHeight(noResultsPanel, -51, Style.Unit.PX, 50, Style.Unit.PX);
    }

    private void enableScrolling() {
        scrollable.getElement().getStyle().setOverflow(Style.Overflow.SCROLL);
    }

    private void disableScrolling() {
        scrollable.getElement().getStyle().setOverflow(Style.Overflow.HIDDEN);
    }

    private static native boolean isFirefox()/*-{
        // Firefox 1.0+
        return typeof InstallTrigger !== 'undefined';
    }-*/;

    public static native void _log(String message)/*-{
        if ($wnd.console) {
            $wnd.console.log(message);
        }
    }-*/;


    public static Resources RESOURCES;

    static {
        RESOURCES = GWT.create(Resources.class);
        RESOURCES.getCSS().ensureInjected();
    }

    /**
     * A ClientBundle of resources used by this widget.
     */
    public interface Resources extends ClientBundle {
        /**
         * The styles used in this widget.
         */
        @Source(ResourceCSS.CSS)
        ResourceCSS getCSS();
    }

    /**
     * Styles used by this widget.
     */
    @CssResource.ImportedWithPrefix("diagram-InfiniteScrollList")
    public interface ResourceCSS extends CssResource {
        /**
         * The path to the default CSS styles used by this resource.
         */
        String CSS = "org/reactome/web/scroller/client/InfiniteScrollList.css";

        String scrollable();

        String offsetDiv();

        String loadingInfo();

        String noResults();

        String errorInfo();
    }
}
