package org.reactome.web.scroller.test;

import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import org.reactome.web.scroller.client.provider.AbstractListAsyncDataProvider;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Kostas Sidiropoulos (ksidiro@ebi.ac.uk)
 */
public class AsyncContactProvider extends AbstractListAsyncDataProvider<ContactInfo> {

    @Override
    protected List<ContactInfo> processResult(String body) {
        return getResults(toList(body));
    }

    @Override
    protected String processError(Response response) {
        return response.getStatusText() + " (" + response.getStatusCode() + ")";
    }

    private List<ContactInfo> getResults(List<String> stringList) {
        List<ContactInfo> rtn = new ArrayList<>();

        for (String result : stringList) {
            rtn.add(new ContactInfo(result, " - "));
        }
        return rtn;
    }

    private List<String> toList(String jsonStr) {
        List<String> rtn = new ArrayList<>();
        if (jsonStr == null || jsonStr.isEmpty()) return rtn;

        JSONValue parsed = JSONParser.parseStrict(jsonStr);
        JSONObject e = parsed.isObject();

        JSONArray jsonArray = e.get("entries").isArray();

        for (int i = 0; i < jsonArray.size(); i++) {
            rtn.add(jsonArray.get(i).isObject().get("stId").isString().stringValue());
        }

        return rtn;
    }
}
