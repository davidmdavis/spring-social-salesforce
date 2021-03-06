package org.springframework.social.salesforce.api.impl;

import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.social.salesforce.api.GetDeletedResult;
import org.springframework.social.salesforce.api.SObjectDetail;
import org.springframework.social.salesforce.api.SObjectSummary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import static org.junit.Assert.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.social.test.client.RequestMatchers.method;
import static org.springframework.social.test.client.RequestMatchers.requestTo;
import static org.springframework.social.test.client.ResponseCreators.withResponse;

/**
 * @author Umut Utkan
 */
public class SObjectsTemplateTest extends AbstractSalesforceTest {

    @Test
    public void getSObjects() {
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects"))
                .andExpect(method(GET))
                .andRespond(withResponse(loadResource("sobjects.json"), responseHeaders));
        List<Map> sobjects = salesforce.sObjectsOperations().getSObjects();
        assertEquals(160, sobjects.size());
        assertEquals("Account", sobjects.get(0).get("name"));
        assertEquals("Account", sobjects.get(0).get("label"));
        assertEquals("Accounts", sobjects.get(0).get("labelPlural"));
        assertEquals("/services/data/v23.0/sobjects/Account", ((Map) sobjects.get(0).get("urls")).get("sobject"));
    }

    @Test
    public void getSObject() {
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Account"))
                .andExpect(method(GET))
                .andRespond(withResponse(loadResource("account.json"), responseHeaders));
        SObjectSummary account = salesforce.sObjectsOperations().getSObjectSummary("Account");
        assertNotNull(account);
        assertEquals("Account", account.getName());
        assertEquals("Account", account.getLabel());
        assertEquals(true, account.isUndeletable());
        assertEquals("001", account.getKeyPrefix());
        assertEquals(false, account.isCustom());
        assertEquals("/services/data/v23.0/sobjects/Account/{ID}", account.getUrls().get("rowTemplate"));
    }

    @Test
    public void describeSObject() {
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Account/describe"))
                .andExpect(method(GET))
                .andRespond(withResponse(loadResource("account_desc.json"), responseHeaders));
        SObjectDetail account = salesforce.sObjectsOperations().describeSObject("Account");
        assertNotNull(account);
        assertEquals("Account", account.getName());
        assertEquals("Account", account.getLabel());
        assertEquals(45, account.getFields().size());
        assertEquals("Id", account.getFields().get(0).getName());
        assertEquals(1, account.getRecordTypeInfos().size());
        assertEquals("Master", account.getRecordTypeInfos().get(0).getName());
        assertEquals(36, account.getChildRelationships().size());
        assertEquals("ParentId", account.getChildRelationships().get(0).getField());
    }

    @Test
    public void getBlob() throws IOException {
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Account/xxx/avatar"))
                .andExpect(method(GET))
                .andRespond(withResponse(new ByteArrayResource("does-not-matter".getBytes("UTF-8")), responseHeaders));
        BufferedReader reader = new BufferedReader(new InputStreamReader(salesforce.sObjectsOperations().getBlob("Account", "xxx", "avatar")));
        assertEquals("does-not-matter", reader.readLine());
    }

    @Test
    public void testCreate() throws IOException {
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Lead"))
                .andExpect(method(POST))
                .andRespond(withResponse(new ByteArrayResource("{\"Id\" : \"1234\"}".getBytes("UTF-8")), responseHeaders));
        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put("LastName", "Doe");
        fields.put("FirstName", "John");
        fields.put("Company", "Acme, Inc.");
        Map<?, ?> result = salesforce.sObjectsOperations().create("Lead", fields);
        assertEquals(1, result.size());
        assertEquals("1234", result.get("Id"));
    }

    @Test
    public void testUpdate() throws IOException {
        // salesforce returns an empty body with a success code if no failures.
        // But, have to mock a json string to satisfy Mock Rest service.
        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Lead/abc123?_HttpMethod=PATCH"))
                .andExpect(method(POST))
                .andRespond(withResponse("{}", responseHeaders));
        Map<String, Object> leadData = new HashMap<String, Object>();
        leadData.put("LastName", "Doe");
        leadData.put("FirstName", "John");
        leadData.put("Company", "Acme, Inc.");
        Map<?, ?> result = salesforce.sObjectsOperations().update("Lead", "abc123", leadData);
        assertTrue(result.size() == 0);
    }

    @Test
    public  void testGetDeleted(){
        TimeZone utc = TimeZone.getTimeZone("UTC");

        Calendar start = Calendar.getInstance(utc);
        start.clear();
        start.set(2014, 0, 2);

        Calendar end = Calendar.getInstance(utc);
        end.clear();
        end.set(2014, 0, 3);

        mockServer.expect(requestTo("https://na7.salesforce.com/services/data/" + AbstractSalesForceOperations.API_VERSION + "/sobjects/Account/deleted/?start=2014-01-02T00:00:00%2B0000&end=2014-01-03T00:00:00%2B0000"))
                .andExpect(method(GET))
                .andRespond(withResponse(loadResource("deleted.json"), responseHeaders));

        GetDeletedResult deletedResult = salesforce.sObjectsOperations().getDeleted("Account", start.getTime(), end.getTime());

        Calendar deletedDate = Calendar.getInstance(utc);
        deletedDate.clear();
        deletedDate.set(2014, 0, 3);

        Calendar earliestAvailable = Calendar.getInstance(utc);
        earliestAvailable.clear();
        earliestAvailable.set(2014, 0, 1);

        Calendar latestCovered = Calendar.getInstance(utc);
        latestCovered.clear();
        latestCovered.set(2014, 0, 5);

        assertEquals(1, deletedResult.getDeletedRecords().size());
        assertEquals("001Z000000gFpeGIAS", deletedResult.getDeletedRecords().get(0).getId());
        assertEquals(deletedDate.getTime(), deletedResult.getDeletedRecords().get(0).getDeletedDate());
        assertEquals(earliestAvailable.getTime(), deletedResult.getEarliestDateAvailable());
        assertEquals(latestCovered.getTime(), deletedResult.getLatestDateCovered());
    }

}
