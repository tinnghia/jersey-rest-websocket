package com.progstack.rs;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/customers")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerService {
    long currentId = 123;
    Map<Long, Customer> customers = new HashMap<Long, Customer>();

    public CustomerService() {
        init();
    }

    @GET
    @Path("{id}")
    public Customer getCustomer(@PathParam("id") String id) {
        long idNumber = Long.parseLong(id);
        Customer customer = customers.get(idNumber);
        return customer;
    }

    @PUT
    @Path("{id}")
    public Response updateCustomer(Customer customer) {
        Customer c = customers.get(customer.getId());
        Response r;
        if (c != null) {
            customers.put(customer.getId(), customer);
            r = Response.ok().build();
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    @POST
    @Path("{id}")
    public Response addCustomer(Customer customer) {
        customer.setId(++currentId);
        customers.put(customer.getId(), customer);
        return Response.ok(customer).build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteCustomer(@PathParam("id") String id) {
        long idNumber = Long.parseLong(id);
        Customer c = customers.get(idNumber);

        Response r;
        if (c != null) {
            r = Response.ok().build();
            customers.remove(idNumber);
        } else {
            r = Response.notModified().build();
        }

        return r;
    }

    final void init() {
        Customer c = new Customer();
        c.setName("John");
        c.setId(123);
        customers.put(c.getId(), c);
        c = new Customer();
        c.setName("Homer");
        c.setId(235);
        customers.put(c.getId(), c);
    }

}
