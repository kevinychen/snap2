package com.kyc.snap.server;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/hosting")
public interface HostingService {

    @POST
    @Path("/")
    @Consumes(MediaType.WILDCARD)
    StringJson hostResource(byte[] data);

    @GET
    @Path("/{resourceId}")
    @Produces(MediaType.WILDCARD)
    byte[] getResource(@PathParam("resourceId") String resourceId);
}
